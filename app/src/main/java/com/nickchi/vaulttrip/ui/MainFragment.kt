package com.nickchi.vaulttrip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.nickchi.vaulttrip.R

class MainFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val layout = view.findViewById<LinearLayout>(R.id.container)
        val testFile = TextView(requireContext()).apply {
            text = "範例"
            textSize = 18f
            setPadding(0, 16, 0, 16)
        }
        layout.addView(testFile)
        return view
        // TODO: 用 ScrollView + LinearLayout 顯示目前資料夾內的檔案
    }
}
