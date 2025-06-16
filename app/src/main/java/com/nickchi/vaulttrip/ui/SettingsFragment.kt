package com.nickchi.vaulttrip.ui

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.nickchi.vaulttrip.R
import com.nickchi.vaulttrip.data.VaultPrefs
import com.nickchi.vaulttrip.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val mimeTypes = arrayOf("text/plain", "text/markdown")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root
        setupVaultSettingButtons()
        setupTemplateButtons()
        setupLocationTemplate()
        setupLocationItemTemplate()
        setupItineraryTemplate()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupVaultSettingButtons() {
        binding.buttonChooseVaultFolder.setOnClickListener {
            folderPicker.launch(null)
        }
        binding.buttonClearVaultFolder.setOnClickListener {
            VaultPrefs.clearRootUri(requireContext())
            showToast(getString(R.string.vault_folder_cleared))
        }
    }

    private fun setupTemplateButtons() {
        binding.buttonChooseTemplateFolder.setOnClickListener {
            templatePicker.launch(VaultPrefs.getRootUri(requireContext()))
        }
        binding.buttonClearTemplateFolder.setOnClickListener {
            VaultPrefs.clearTemplateUri(requireContext())
            showToast(getString(R.string.template_folder_cleared))
        }
    }

    private fun setupLocationTemplate() {
        binding.buttonChooseLocationTemplate.setOnClickListener {
            locationTemplatePicker.launch(mimeTypes)
        }
        binding.buttonClearLocationTemplate.setOnClickListener {
            VaultPrefs.clearLocationTemplateUri(requireContext())
            binding.textLocationTemplateUri.text = getString(R.string.template_uri_empty)
            showToast(getString(R.string.location_template_cleared))
        }
        updateLocationTemplateDisplay()
    }

    private fun setupLocationItemTemplate() {
        binding.buttonChooseLocationItemTemplate.setOnClickListener {
            locationItemTemplatePicker.launch(mimeTypes)
        }
        binding.buttonClearLocationItemTemplate.setOnClickListener {
            VaultPrefs.clearLocationItemTemplateUri(requireContext())
            binding.textLocationItemTemplateUri.text = getString(R.string.template_uri_empty)
            showToast(getString(R.string.location_item_template_cleared))
        }
        updateLocationItemTemplateDisplay()
    }

    private fun setupItineraryTemplate() {
        binding.buttonChooseItineraryTemplate.setOnClickListener {
            itineraryTemplatePicker.launch(mimeTypes)
        }
        binding.buttonClearItineraryTemplate.setOnClickListener {
            VaultPrefs.clearItineraryTemplateUri(requireContext())
            binding.textItineraryTemplateUri.text = getString(R.string.template_uri_empty)
            showToast(getString(R.string.itinerary_template_cleared))
        }
        updateItineraryTemplateDisplay()
    }

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        handleSelectedDirectory(
            uri,
            VaultPrefs::saveRootUri,
            R.string.vault_folder_saved,
            R.string.no_folder_selected
        )
    }

    private val templatePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        handleSelectedDirectory(
            uri,
            VaultPrefs::saveTemplateUri,
            R.string.template_folder_saved,
            R.string.no_folder_selected
        )
    }

    private val locationTemplatePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        handleSelectedFile(
            uri,
            VaultPrefs::saveLocationTemplateUri,
            ::updateLocationTemplateDisplay,
            R.string.location_template_saved,
            R.string.no_file_selected
        )
    }

    private val locationItemTemplatePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        handleSelectedFile(
            uri,
            VaultPrefs::saveLocationItemTemplateUri,
            ::updateLocationItemTemplateDisplay,
            R.string.location_item_template_saved,
            R.string.no_file_selected
        )
    }

    private val itineraryTemplatePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        handleSelectedFile(
            uri,
            VaultPrefs::saveItineraryTemplateUri,
            ::updateItineraryTemplateDisplay,
            R.string.itinerary_template_saved,
            R.string.no_file_selected
        )
    }


    private fun handleSelectedDirectory(
        uri: Uri?,
        saveAction: (context: android.content.Context, Uri) -> Unit,
        successMessageResId: Int,
        failureMessageResId: Int
    ) {
        if (uri != null) {
            takePersistableUriPermission(uri)
            saveAction(requireContext(), uri)
            showToast(getString(successMessageResId))
            // No direct UI update needed here as these don't display the URI path
        } else {
            showToast(getString(failureMessageResId))
        }
    }

    private fun handleSelectedFile(
        uri: Uri?,
        saveAction: (context: android.content.Context, Uri) -> Unit,
        updateDisplayAction: () -> Unit,
        successMessageResId: Int,
        failureMessageResId: Int
    ) {
        if (uri != null) {
            takePersistableUriPermission(uri)
            saveAction(requireContext(), uri)
            updateDisplayAction()
            showToast(getString(successMessageResId))
        } else {
            updateDisplayAction()
            showToast(getString(failureMessageResId))
        }
    }

    private fun takePersistableUriPermission(uri: Uri) {
        try {
            val flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION // Consider if you need write
            requireContext().contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: SecurityException) {
            // Handle error, e.g., show a toast or log
            Log.e("SettingsFragment", "Error taking URI permission", e)
            showToast("Error taking URI permission")
        }
    }

    // --- UI Update Methods ---
    private fun updateLocationTemplateDisplay() {
        val uri = VaultPrefs.getLocationTemplateUri(requireContext())
        val displayName = getUriDisplayName(uri)
        binding.textLocationTemplateUri.text = displayName
    }

    private fun updateLocationItemTemplateDisplay() {
        val uri = VaultPrefs.getLocationItemTemplateUri(requireContext())
        val displayName = getUriDisplayName(uri)
        binding.textLocationItemTemplateUri.text = displayName
    }

    private fun updateItineraryTemplateDisplay() {
        val uri = VaultPrefs.getItineraryTemplateUri(requireContext())
        val displayName = getUriDisplayName(uri)
        binding.textItineraryTemplateUri.text = displayName
    }

    // --- Utility ---
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun getUriDisplayName(uri: Uri?): String {
        if (uri == null) {
            return getString(R.string.template_uri_empty)
        }
        // The query can fail if the URI is invalid, so use try-catch.
        try {
            val cursor: Cursor? = requireContext().contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME), // We only want the display name
                null,
                null,
                null
            )

            cursor?.use {
                // moveToFirst() returns false if the cursor has 0 rows.
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        return it.getString(displayNameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Log the error or handle it as appropriate for your app
            // For example, you might fall back to uri.getLastPathSegment() or uri.toString()
            Log.e("SettingsFragment", "Error getting display name for URI: $uri", e)
            // Fallback: try to get the last path segment as a rough display name
            return uri.lastPathSegment ?: uri.toString()
        }
        return uri.toString()
    }
}