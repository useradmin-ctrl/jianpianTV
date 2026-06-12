package com.jianpian.tv.ui.browse

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.lifecycleScope
import com.jianpian.tv.R
import com.jianpian.tv.data.remote.model.VideoItem
import com.jianpian.tv.ui.common.CardPresenter
import com.jianpian.tv.ui.detail.DetailFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : BrowseSupportFragment() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var rowsAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        title = "简片TV"
        headersState = BrowseSupportFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = Color.parseColor("#FF6B35")
        searchAffordanceColor = Color.parseColor("#FF6B35")

        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        adapter = rowsAdapter

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is VideoItem) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, DetailFragment.newInstance(item))
                    .addToBackStack(null)
                    .commit()
            }
        }

        setOnSearchClickedListener {
            showSearchDialog()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.rows.collect { rows ->
                rowsAdapter.clear()
                rows.forEach { row ->
                    if (row.videos.isNotEmpty()) {
                        val header = HeaderItem(row.title)
                        val rowAdapter = ArrayObjectAdapter(CardPresenter())
                        row.videos.forEach { rowAdapter.add(it) }
                        rowsAdapter.add(ListRow(header, rowAdapter))
                    }
                }
            }
        }
    }

    private fun showSearchDialog() {
        val ctx = requireContext()
        val editText = EditText(ctx).apply {
            hint = "输入关键字搜索"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(40, 255, 255, 255))
            setPadding(32, 24, 32, 24)
            textSize = 16f
        }
        AlertDialog.Builder(ctx)
            .setTitle("搜索视频")
            .setView(editText)
            .setPositiveButton("搜索") { _, _ ->
                val keyword = editText.text.toString().trim()
                if (keyword.isNotEmpty()) viewModel.search(keyword)
            }
            .setNegativeButton("取消", null)
            .show()
        editText.postDelayed({
            editText.requestFocus()
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }
}
