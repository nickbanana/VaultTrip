package com.nickchi.vaulttrip.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nickchi.vaulttrip.R
import com.nickchi.vaulttrip.data.DocumentItem
import com.nickchi.vaulttrip.data.VaultPrefs
import com.nickchi.vaulttrip.file.VaultScanner.getAllMarkdownFilesFromUri

class MainFragment : Fragment() {
    private lateinit var container: LinearLayout
    private var currentTreeUri: Uri? = null
    private var rootDocumentId: String? = null
    private val parentDocIdStack = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.container)
        loadOrRequestInitialDirectory()
        initializeBackPressedBehavior()
        exampleUsageOfRecursiveFetch()
    }

    private fun initializeBackPressedBehavior() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (canNavigateUpInFilesystem()) {
                    navigateUpInFilesystem()
                } else {
                    // Disable this callback to prevent infinite loop, and manually call onBackPressed()
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadOrRequestInitialDirectory() {
        if (!VaultPrefs.hasRootUri(requireContext())) {
            findNavController().navigate(R.id.settingsFragment)
            return
        }
        VaultPrefs.getRootUri(requireContext())?.let { rootUri ->
            currentTreeUri = rootUri
            loadInitialFolderContents(rootUri)
        }
    }

    private fun loadInitialFolderContents(uri: Uri) {
        if (tryPersistReadPermissionForUri(uri)) {
            val rootDocId = DocumentsContract.getTreeDocumentId(uri)
            if (rootDocId != null) {
                this.rootDocumentId = rootDocId
                parentDocIdStack.clear()
                parentDocIdStack.add(rootDocId)
                listFolderContents(uri, rootDocId)
            } else {
                Log.e("MainFragment", "Could not get root document ID from $uri")
                Toast.makeText(requireContext(), "Could not access folder root", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun tryPersistReadPermissionForUri(uri: Uri): Boolean {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        return try {
            requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
            Log.d("Permission", "Successfully took persistable read permission for $uri")
            true
        } catch (e: SecurityException) {
            Log.e("PermissionError", "Failed to take persistable URI permission for $uri", e)
            // Inform the user
            Toast.makeText(requireContext(), "Failed to persist permission for $uri", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun listFolderContents(treeUriForNavigation: Uri, parentDocumentId: String) {
        Log.d("ListContents", "Listing for tree: $treeUriForNavigation, parentDocId: $parentDocumentId")
        val documentItems = fetchDocumentItems(treeUriForNavigation, parentDocumentId)
        val sortedDocumentItems = sortDocumentItems(documentItems)
        displayFolderContents(sortedDocumentItems, treeUriForNavigation)
    }

    private fun displayFolderContents(items: List<DocumentItem>, treeUriForNavigation: Uri) {
        container.removeAllViews()
        if (items.isEmpty()) {
            container.addView(TextView(requireContext()).apply {
                text = "資料夾為空"
                textSize = 18f
                setPadding(0, 16, 0, 16)
            })
            return
        }
        items.forEach { item ->
            val textView = createDocumentItemView(item, treeUriForNavigation)
            container.addView(textView)
        }
    }

    private fun sortDocumentItems(items: List<DocumentItem>): List<DocumentItem> {
        return items.sortedWith(compareBy({ it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }, { it.name }))
    }

    private fun createDocumentItemView(item: DocumentItem, treeUriForNavigation: Uri): TextView {
        return TextView(requireContext()).apply {
            text = if (item.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) "📁 ${item.name}" else item.name
            textSize = 18f
            setPadding(0, 16, 0, 16)
            setOnClickListener {
                Log.d("FolderClick", "Item clicked: ${item.name}, MIME: ${item.mimeType}")
                handleDocumentItemClick(item, treeUriForNavigation)
            }
        }
    }

    private fun handleDocumentItemClick(item: DocumentItem, treeUriForNavigation: Uri) {
        if (item.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
            Log.d(
                "FolderClick",
                "Navigating into folder: ${item.name}, childDocId: ${item.docId}"
            )
            if (parentDocIdStack.lastOrNull() != item.docId || parentDocIdStack.isEmpty()) {
                parentDocIdStack.add(item.docId)
            }
            // When navigating into a subfolder, its docId becomes the new parentDocumentId
            // The treeUri remains the same (the initially selected one)
            listFolderContents(treeUriForNavigation, item.docId)
        } else if (item.name.endsWith(".md")) {
            Log.d("FileClick", "Navigating to file: ${item.name}, uri: ${item.itemUri}")
            val action = MainFragmentDirections.actionMainToViewer(item.itemUri.toString())
            try {
                findNavController().navigate(action)
            } catch (e: IllegalArgumentException) {
                Log.e("NavigationError", "Failed to navigate to file: ${item.name}", e)
                Toast.makeText(requireContext(), "Failed to navigate to file: ${item.name}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchDocumentItems(treeUri: Uri, parentDocId: String): List<DocumentItem> {
        val items = mutableListOf<DocumentItem>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val cursor = requireContext().contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0)
                val docId = it.getString(1) // This is the docId of the child item
                val mime = it.getString(2)
                if (name.startsWith(".")) continue
                val itemUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                items.add(DocumentItem(name, docId, mime, itemUri))
            }
        }
        return items
    }

    private fun canNavigateUpInFilesystem(): Boolean {
        return parentDocIdStack.size > 1
    }

    private fun navigateUpInFilesystem() {
        if (canNavigateUpInFilesystem()) {
            parentDocIdStack.removeAt(parentDocIdStack.lastIndex)
            val parentToLoad = parentDocIdStack.lastOrNull()
            if (parentToLoad != null && currentTreeUri != null) {
                listFolderContents(currentTreeUri!!, parentToLoad)
            } else {
                Log.e("NavigationError", "Failed to navigate up in filesystem")
                loadOrRequestInitialDirectory()
                Toast.makeText(requireContext(), "Failed to navigate up in filesystem", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 遞迴取出檔案
    private fun exampleUsageOfRecursiveFetch() {
        VaultPrefs.getRootUri(requireContext())?.let { userSelectedRootUri ->
            // This can be a long-running operation!
            // Consider running it in a background thread or coroutine.
            Thread { // Simple Thread example, use coroutines for better management
                val ignoreUri = VaultPrefs.getTemplateUri(requireContext())
                Log.d("RecursiveFetchExample", "Starting recursive fetch from $userSelectedRootUri, ignoreUri: $ignoreUri")
                val allMdFiles = getAllMarkdownFilesFromUri(requireContext(), userSelectedRootUri, ignoreUri)
                activity?.runOnUiThread { // Update UI or log on the main thread
                    if (allMdFiles.isNotEmpty()) {
                        Log.i("RecursiveFetchExample", "Found ${allMdFiles.size} markdown files globally:")
                        // allMdFiles.forEach { Log.i("RecursiveFetchExample", "- $it") }
                        // Do something with the list, e.g., update a database, build an index
                        Toast.makeText(requireContext(), "Found ${allMdFiles.size} MD files in total.", Toast.LENGTH_LONG).show()
                    } else {
                        Log.i("RecursiveFetchExample", "No markdown files found recursively or error occurred.")
                        Toast.makeText(requireContext(), "No MD files found in total scan.", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

}
