package com.nickchi.vaulttrip.ui

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import io.noties.markwon.Markwon

class MarkdownViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val textView = TextView(this)
        scrollView.addView(textView)
        scrollView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setContentView(scrollView)

        // 自動處理狀態列內縮
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = topInset)
            insets
        }

        val uri = intent.getStringExtra("uri")?.let { Uri.parse(it) }
        if (uri != null) {
            val inputStream = contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader().use { it?.readText() ?: "" }
            val markwon = Markwon.create(this)
            markwon.setMarkdown(textView, content)
        } else {
            textView.text = "讀取失敗"
        }
    }
}