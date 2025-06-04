package com.nickchi.vaulttrip

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

class MainActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private var initialTreeUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        scrollView.addView(container, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        setContentView(scrollView)
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = topInset)
            insets
        }
        val selectDirectoryLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->
            if (uri != null) {
                initialTreeUri = uri
                val takeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION // Assuming you only need to read
                try {
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                    var rootDocId = DocumentsContract.getTreeDocumentId(uri)
                    listFolderContents(uri, rootDocId)
                } catch (e: SecurityException) {
                    // Handle the case where permission could not be persisted
                    // Log the error, inform the user, etc.
                    Log.e(
                        "PermissionError",
                        "Failed to take persistable URI permission for $uri",
                        e
                    )
                    // You might want to fall back to a non-persisted access or re-request permission
                }
            } else {
                Toast.makeText(this, "未選擇資料夾", Toast.LENGTH_SHORT).show()
            }
        }

        selectDirectoryLauncher.launch(null)
    }

    private fun listFolderContents(currentTreeUri: Uri, parentDocumentId: String) {
        Log.d("ListContents", "Listing for tree: $currentTreeUri, parentDocId: $parentDocumentId")
        container.removeAllViews()

        // Always use the initialTreeUri (or currentTreeUri if it IS the root tree URI)
        // and the specific parentDocumentId for building the children URI
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            currentTreeUri,
            parentDocumentId
        )
        Log.d("ListContents", "Querying children URI: $childrenUri")

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

                // The URI for click handling (markdown file) can still be built this way
                // using the currentTreeUri and the child's docId
                val itemDocumentUri =
                    DocumentsContract.buildDocumentUriUsingTree(currentTreeUri, docId)

                val textView = TextView(this).apply {
                    text = if (mime == DocumentsContract.Document.MIME_TYPE_DIR) "📁 $name" else name
                    textSize = 18f
                    setPadding(0, 16, 0, 16)
                    setOnClickListener {
                        Log.d("FolderClick", "Item clicked: $name, MIME: $mime")
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            Log.d(
                                "FolderClick",
                                "Navigating into folder: $name, childDocId: $docId"
                            )
                            // When navigating into a subfolder, its docId becomes the new parentDocumentId
                            // The treeUri remains the same (the initially selected one)
                            initialTreeUri?.let { treeRootUri -> // Ensure initialTreeUri is not null
                                listFolderContents(treeRootUri, docId)
                            }
                        } else if (name.endsWith(".md")) {
                            val intent =
                                Intent(this@MainActivity, MarkdownViewerActivity::class.java)
                            // For opening a file, itemDocumentUri is correct
                            intent.putExtra("uri", itemDocumentUri.toString())
                            startActivity(intent)
                        }
                    }
                }
                container.addView(textView)
            }
        }
    }
}