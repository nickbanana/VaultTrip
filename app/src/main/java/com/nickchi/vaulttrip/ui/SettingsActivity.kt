package com.nickchi.vaulttrip.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.nickchi.vaulttrip.data.VaultPrefs

class SettingsActivity : AppCompatActivity() {
    private lateinit var chooseButton: Button
    private lateinit var clearButton: Button
    
    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        handleSelectedDirectory(uri)
    }

    private fun handleSelectedDirectory(uri: Uri?) {
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
            VaultPrefs.saveRootUri(this, uri)
            Toast.makeText(this, "已儲存 Vault 資料夾", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "未選擇任何資料夾", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        chooseButton = Button(this).apply {
            text = "選擇 Vault 資料夾"
            setOnClickListener { folderPicker.launch(null) }
        }

        clearButton = Button(this).apply {
            text = "清除設定"
            setOnClickListener {
                VaultPrefs.clearRootUri(this@SettingsActivity)
                Toast.makeText(this@SettingsActivity, "已清除儲存路徑", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(chooseButton)
        layout.addView(clearButton)
        setContentView(layout)
    }
}