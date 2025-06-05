package com.nickchi.vaulttrip.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.nickchi.vaulttrip.data.DocumentItem
import com.nickchi.vaulttrip.data.VaultPrefs

class MainActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private var selectDirectoryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        handleSelectedDirectory(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        loadOrRequestInitialDirectory()
    }

    private fun setupUI() {
        val scrollView = ScrollView(this)
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        scrollView.addView(
            container, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(scrollView)
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = topInset)
            insets
        }
    }

    private fun loadOrRequestInitialDirectory() {
        if (!VaultPrefs.hasRootUri(this)) {
            selectDirectoryLauncher.launch(null)
        } else {
            VaultPrefs.getRootUri(this)?.let { rootUri -> loadInitialFolderContents(rootUri) }
        }
    }

    private fun handleSelectedDirectory(uri: Uri?) {
        if (uri != null) {
            VaultPrefs.saveRootUri(this, uri)
            loadInitialFolderContents(uri)
        } else {
            Toast.makeText(this, "未選擇資料夾", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryPersistReadPermissionForUri(uri: Uri): Boolean {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        return try {
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            Log.d("Permission", "Successfully took persistable read permission for $uri")
            true
        } catch (e: SecurityException) {
            Log.e("PermissionError", "Failed to take persistable URI permission for $uri", e)
            // Inform the user
            Toast.makeText(this, "Failed to persist permission for $uri", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun loadInitialFolderContents(uri: Uri) {
        if (tryPersistReadPermissionForUri(uri)) {
            val rootDocId = DocumentsContract.getTreeDocumentId(uri)
            if (rootDocId != null) {
                listFolderContents(uri, rootDocId)
            } else {
                Log.e("MainActivity", "Could not get root document ID from $uri")
                Toast.makeText(this, "Could not access folder root", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun listFolderContents(treeUriForNavigation: Uri, parentDocumentId: String) {
        Log.d("ListContents", "Listing for tree: $treeUriForNavigation, parentDocId: $parentDocumentId")
        val documentItems = fetchDocumentItems(treeUriForNavigation, parentDocumentId)
        displayFolderContents(documentItems, treeUriForNavigation)
    }

    private fun displayFolderContents(items: List<DocumentItem>, treeUriForNavigation: Uri) {
        container.removeAllViews()
        if (items.isEmpty()) {
            container.addView(TextView(this).apply {
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

    private fun createDocumentItemView(item: DocumentItem, treeUriForNavigation: Uri): TextView {
        return TextView(this).apply {
            text = if (item.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) "📁 ${item.name}" else item.name
            textSize = 18f
            setPadding(0, 16, 0, 16)
            setOnClickListener {
                Log.d("FolderClick", "Item clicked: ${item.name}, MIME: ${item.mimeType}")
                handleDocumentItemClick(item, treeUriForNavigation)
            }
        }
    }

    private fun TextView.handleDocumentItemClick(item: DocumentItem, treeUriForNavigation: Uri) {
        if (item.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
            Log.d(
                "FolderClick",
                "Navigating into folder: ${item.name}, childDocId: ${item.docId}"
            )
            // When navigating into a subfolder, its docId becomes the new parentDocumentId
            // The treeUri remains the same (the initially selected one)
            listFolderContents(treeUriForNavigation, item.docId)
        } else if (item.name.endsWith(".md")) {
            val intent =
                Intent(this@MainActivity, MarkdownViewerActivity::class.java)
            // For opening a file, itemDocumentUri is correct
            intent.putExtra("uri", item.itemUri.toString())
            startActivity(intent)
        }
    }

    private fun fetchDocumentItems(treeUri: Uri, parentDocId: String): List<DocumentItem> {
        val items = mutableListOf<DocumentItem>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val cursor = contentResolver.query(
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
}