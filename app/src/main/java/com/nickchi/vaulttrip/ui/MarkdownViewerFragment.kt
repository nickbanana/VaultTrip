package com.nickchi.vaulttrip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.nickchi.vaulttrip.R
import io.noties.markwon.Markwon

class MarkdownViewerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_markdown_viewer, container, false)
        val textView = view.findViewById<TextView>(R.id.markdownTextView)

        val uri = arguments?.getString("uri")?.toUri()
        if (uri != null && context != null) {
            val content = requireContext().contentResolver.openInputStream(uri)
                ?.bufferedReader().use { it?.readText() ?: "" }
            val markwon = Markwon.create(requireContext())
            markwon.setMarkdown(textView, content)
        }

        return view
    }
}