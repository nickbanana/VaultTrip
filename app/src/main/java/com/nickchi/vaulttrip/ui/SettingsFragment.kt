package com.nickchi.vaulttrip.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.nickchi.vaulttrip.R
import com.nickchi.vaulttrip.data.VaultPrefs

class SettingsFragment : Fragment() {

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
            requireContext().contentResolver.takePersistableUriPermission(uri, flags)
            VaultPrefs.saveRootUri(requireContext(), uri)
            Toast.makeText(requireContext(), "已儲存 Vault 資料夾", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "未選擇任何資料夾", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        chooseButton = view.findViewById(R.id.chooseVaultButton)
        chooseButton.setOnClickListener {
            folderPicker.launch(null)
        }
        clearButton = view.findViewById(R.id.clearVaultButton)
        clearButton.setOnClickListener {
            VaultPrefs.clearRootUri(requireContext())
            Toast.makeText(requireContext(), "已清除 Vault 資料夾", Toast.LENGTH_SHORT).show()
        }
        return view
    }
}