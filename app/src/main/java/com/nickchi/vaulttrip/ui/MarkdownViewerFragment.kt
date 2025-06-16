package com.nickchi.vaulttrip.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.nickchi.vaulttrip.R
import io.noties.markwon.Markwon
import java.io.FileNotFoundException
import java.io.IOException

class MarkdownViewerFragment : Fragment() {

    private val args: MarkdownViewerFragmentArgs by navArgs()
    private lateinit var textView: TextView
    private lateinit var loadingIndicator: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_markdown_viewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textView = view.findViewById(R.id.markdownTextView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        var markdownUriString = args.markdownFileUri
        Log.d("MarkdownViewerFragment", "Markdown URI: $markdownUriString")
        if (markdownUriString.isEmpty()) {
            Log.e("MarkdownViewerFragment", "Markdown URI is null or empty")
            showError("Error: No file URI provided.")
            return
        }
        try {
            val markdownUri =
                markdownUriString.toUri() // Potential for IllegalArgumentException if string is not a valid URI
            // textView.text = "Loading Markdown from URI: $markdownUri" // Temporary, good for debugging
            loadMarkdownFromUri(markdownUri)
        } catch (e: IllegalArgumentException) {
            Log.e("MarkdownViewerFragment", "Invalid URI string: $markdownUriString", e)
            showError("Error: Invalid file URI format.")
        }
    }

    private fun loadMarkdownFromUri(uri: Uri) {
        showLoading(true)
        textView.text = "" // Clear any previous content
        try {
            // requireContext() is appropriate here as Fragment should be attached
            val content = requireContext().contentResolver.openInputStream(uri)
                ?.bufferedReader().use { it?.readText() }
            if (content != null) {
                val markwon = Markwon.create(requireContext())
                markwon.setMarkdown(textView, content) // Uses member textView
            } else {
                Log.e(
                    "MarkdownViewer",
                    "Failed to load markdown from URI: $uri (stream was null or empty)"
                )
                showError("Error: Could not read file content.")
            }

        } catch (e: FileNotFoundException) {
            Log.e("MarkdownViewer", "File not found for URI: $uri", e)
            showError("Error: File not found.")
        } catch (e: SecurityException) {
            Log.e("MarkdownViewer", "Permission denied for URI: $uri", e)
            showError("Error: Permission denied to access the file.")
        } catch (e: IOException) {
            Log.e("MarkdownViewer", "IO error reading URI: $uri", e)
            showError("Error: Could not read the file due to an IO problem.")
        } catch (e: Exception) { // Catch-all for other unexpected errors
            Log.e("MarkdownViewer", "Unexpected error loading markdown from URI: $uri", e)
            showError("Error: An unexpected error occurred while loading the file.")
        } finally {
            // Ensure loading is hidden if an exception occurred before explicitly hiding it
            if (loadingIndicator.isVisible) {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingIndicator.isVisible = isLoading
        textView.isVisible = !isLoading
    }

    private fun showError(message: String) {
        showLoading(false) // Hide loading indicator
        textView.text = message
        textView.isVisible = true // Make sure textView is visible to show the error
    }
}