package com.nickchi.vaulttrip.file

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import com.nickchi.vaulttrip.data.DocumentItem

object VaultScanner {
    private var documentIdToIgnore: String? = null


    /**
     * Call this ONCE before starting a scan if you have an ignoreUri.
     * It resolves the document ID for the ignoreUri to allow for efficient comparison during recursion
     */
    fun setIgnoreUri(ignoreUri: Uri?) {
        if (ignoreUri == null) {
            documentIdToIgnore = null
            Log.d("VaultScanner", "No ignore URI set. documentIdToIgnore cleared.")
            return
        }
        // We assume ignoreUri is a URI for which we can get a tree document ID,
        // or a document URI from which we can get a document ID.
        // This ID should be comparable to the document IDs encountered during traversal.
        try {
            // Try to get it as a tree document ID first (most common for user-picked folders)
            val treeDocId = DocumentsContract.getTreeDocumentId(ignoreUri)
            if (treeDocId != null) {
                documentIdToIgnore = treeDocId
                Log.d(
                    "VaultScanner",
                    "Set ignore target. URI: $ignoreUri (treated as tree) has DocId: $documentIdToIgnore"
                )
            } else {
                // If not a tree, try as a regular document URI
                val docId = DocumentsContract.getDocumentId(ignoreUri)
                documentIdToIgnore = docId
                Log.d(
                    "VaultScanner",
                    "Set ignore target. URI: $ignoreUri (treated as document) has DocId: $documentIdToIgnore"
                )
            }
        } catch (e: IllegalArgumentException) {
            Log.w(
                "VaultScanner",
                "ignoreUri $ignoreUri is not a valid tree or document URI to get a documentId from. Error: ${e.message}"
            )
            documentIdToIgnore = null
        }
    }

    /**
     * Recursively fetches all Markdown file URIs (.md) starting from the given rootUri.
     * Ignores directories whose names start with a dot ('.').
     *
     * @param context Context needed for ContentResolver.
     * @param treeUri The URI of the root directory tree to start searching from.
     *                 This URI must have been granted persistent read permission.
     * @param currentParentDocumentId The document ID of the current directory being processed.
     *                                 For the initial call, this should be DocumentsContract.getTreeDocumentId(treeUri).
     * @param allMarkdownFiles A mutable list to accumulate the URIs of found Markdown files.
     *                         This list is passed recursively.
     */
    private fun fetchAllMarkdownFilesRecursive(
        context: Context,
        treeUri: Uri,
        currentParentDocumentId: String,
        allMarkdownFiles: MutableList<DocumentItem>
    ) {
        // Check if the current directory (defined by treeUri + currentParentDocumentId)
        // should be ignored.
        // This comparison is more reliable if treeUriForIgnoreCheck matches the current scan's treeUri
        if (
            documentIdToIgnore != null &&
            currentParentDocumentId == documentIdToIgnore
        ) {
            Log.d(
                "FetchMarkdown",
                "Skipping ignored directory by Document ID: $currentParentDocumentId in tree $treeUri"
            )
            return
        }
        // Fallback or alternative: if ignoreUri was a sub-folder and we have its full document URI.
        // val currentDirectoryUriForDirectComparison = DocumentsContract.buildDocumentUriUsingTree(treeUri, currentParentDocumentId)
        // if (ignoreUriFromVaultPrefs != null && currentDirectoryUriForDirectComparison == ignoreUriFromVaultPrefs) {
        //     Log.d("FetchMarkdown", "Skipping ignored directory by exact Document URI match: $currentDirectoryUriForDirectComparison")
        //     return
        // }


        val currentDirectoryUri =
            DocumentsContract.buildDocumentUriUsingTree( // For logging or other purposes
                treeUri,
                currentParentDocumentId
            )
        Log.d(
            "FetchMarkdown",
            "Processing directory: $currentDirectoryUri (DocID: $currentParentDocumentId within Tree: $treeUri)"
        )


        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            currentParentDocumentId
        )


        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val docIdIndex =
                            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val nameIndex =
                            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val mimeTypeIndex =
                            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                        if (docIdIndex == -1 || nameIndex == -1 || mimeTypeIndex == -1) {
                            Log.w(
                                "FetchMarkdown",
                                "Required column not found in cursor for $childrenUri"
                            )
                            continue
                        }

                        val docId = cursor.getString(docIdIndex)
                        val name = cursor.getString(nameIndex)
                        val mimeType = cursor.getString(mimeTypeIndex)

                        if (name.startsWith(".")) {
                            // Skip dot files and dot directories entirely
                            Log.d("FetchMarkdown", "Skipping dot item: $name")
                            continue
                        }

                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            // This is a directory, recurse into it
                            Log.d("FetchMarkdown", "Entering directory: $name (docId: $docId)")
                            fetchAllMarkdownFilesRecursive(
                                context,
                                treeUri,
                                docId,
                                allMarkdownFiles
                            )
                        } else if (name.endsWith(".md", ignoreCase = true)) {
                            // This is a Markdown file
                            val fileUri =
                                DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val documentItem = DocumentItem(name, docId, mimeType, fileUri)
                            allMarkdownFiles.add(documentItem)
                            Log.d(
                                "FetchMarkdown",
                                "Found Markdown file: ${documentItem.name}, Uri: ${documentItem.itemUri}"
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            // Catch potential SecurityExceptions or other issues during query
            Log.e(
                "FetchMarkdown",
                "Error querying children for $currentParentDocumentId under $treeUri",
                e
            )
            // You might want to inform the user or handle this more gracefully
        }
    }

    /**
     * Public wrapper function to initiate the recursive search for Markdown files.
     *
     * @param context Context needed for ContentResolver.
     * @param rootUri The URI of the root directory tree.
     * @return A List of Uris for all found Markdown files, or an empty list if errors occur or none are found.
     */
    fun getAllMarkdownFilesFromUri(
        context: Context,
        rootUri: Uri,
        ignoreUri: Uri?
    ): List<DocumentItem> {
        // Ensure we have permission for the root URI (important!)
        // This function assumes permission has already been granted and persisted.
        // If not, the queries inside fetchAllMarkdownFilesRecursive will fail.
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(rootUri, takeFlags)
        } catch (e: SecurityException) {
            Log.e(
                "FetchMarkdown",
                "Failed to ensure persistable read permission for $rootUri. Cannot fetch files.",
                e
            )
            Toast.makeText(
                context,
                "Permission error for $rootUri. Cannot scan files.",
                Toast.LENGTH_LONG
            ).show()
            return emptyList() // Return empty if permission fails
        }

        val rootDocId = DocumentsContract.getTreeDocumentId(rootUri)
        if (rootDocId == null) {
            Log.e("FetchMarkdown", "Could not get document ID for root URI: $rootUri")
            Toast.makeText(context, "Could not access root URI: $rootUri", Toast.LENGTH_SHORT)
                .show()
            return emptyList()
        }
        setIgnoreUri(ignoreUri)
        // Initial check: if the rootUri itself is the one to be ignored
        // This relies on setIgnoreDirectory correctly populating ignoreDocumentIdForTree
        // AND treeUriForIgnoreCheck to be rootUri itself.
        if (documentIdToIgnore != null && rootDocId == documentIdToIgnore) {
            Log.d(
                "FetchMarkdown",
                "Root URI is the ignored directory. Skipping entire scan based on Document ID."
            )
            return emptyList()
        }

        val markdownFiles = mutableListOf<DocumentItem>()
        Log.d(
            "FetchMarkdown",
            "Starting recursive search from root: $rootUri, rootDocId: $rootDocId"
        )
        fetchAllMarkdownFilesRecursive(context, rootUri, rootDocId, markdownFiles)
        Log.d("FetchMarkdown", "Finished search. Found ${markdownFiles.size} Markdown files.")
        return markdownFiles
    }
}