package com.jianpian.tv.ui.browse

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jianpian.tv.R
import com.jianpian.tv.data.remote.model.VideoItem
import com.jianpian.tv.ui.common.PosterAdapter
import com.jianpian.tv.ui.detail.DetailFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BrowseFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var textError: TextView
    private lateinit var categoryBar: LinearLayout
    private lateinit var adapter: PosterAdapter

    private val categories = listOf("首页" to "", "电影" to "电影", "电视剧" to "电视剧", "动漫" to "动漫", "综艺" to "综艺")
    private var selectedCategory = 0
    private val categoryViews = mutableListOf<TextView>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_browse, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)
        textError = view.findViewById(R.id.text_error)
        categoryBar = view.findViewById(R.id.category_bar)

        adapter = PosterAdapter { video -> openDetail(video) }
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 6)
        recyclerView.setHasFixedSize(true)
        recyclerView.isFocusable = false
        recyclerView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        recyclerView.adapter = adapter

        buildCategoryBar()
        view.findViewById<View>(R.id.btn_search).setOnClickListener { showSearchDialog() }
        textError.setOnClickListener { viewModel.loadAllCategories() }

        lifecycleScope.launch { viewModel.rows.collect { updateCategoryUI() } }
        lifecycleScope.launch { viewModel.isLoading.collect { updateLoadingUI() } }
        lifecycleScope.launch { viewModel.error.collect { updateErrorUI() } }
    }

    private fun buildCategoryBar() {
        categoryBar.removeAllViews()
        categoryViews.clear()

        categories.forEachIndexed { index, (label, _) ->
            val tv = TextView(requireContext()).apply {
                text = label
                textSize = 15f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(24, 0, 24, 0)
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                setOnClickListener { selectCategory(index) }
                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) selectCategory(index)
                    updateTabStyle(this, index, hasFocus)
                }
            }
            updateTabStyle(tv, index, false)
            categoryViews.add(tv)
            categoryBar.addView(tv)
        }
    }

    private fun updateTabStyle(tv: TextView, index: Int, hasFocus: Boolean) {
        if (hasFocus || index == selectedCategory) {
            tv.setTextColor(Color.parseColor("#FF6B35"))
            tv.setTypeface(null, Typeface.BOLD)
        } else {
            tv.setTextColor(Color.parseColor("#CCCCCC"))
            tv.setTypeface(null, Typeface.NORMAL)
        }
    }

    private fun selectCategory(index: Int) {
        // 搜索模式时点击分类，重新加载全部
        if (viewModel.isInSearchMode) {
            selectedCategory = index
            viewModel.loadAllCategories()
            updateAllTabStyles()
            return
        }

        selectedCategory = index
        updateAllTabStyles()
        updateCategoryUI()
    }

    private fun updateAllTabStyles() {
        categoryViews.forEachIndexed { i, tv ->
            if (i == selectedCategory) {
                tv.setTextColor(Color.parseColor("#FF6B35"))
                tv.setTypeface(null, Typeface.BOLD)
            } else {
                tv.setTextColor(Color.parseColor("#CCCCCC"))
                tv.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    private fun updateCategoryUI() {
        val rows = viewModel.rows.value
        val idx = if (viewModel.isInSearchMode) 0 else selectedCategory.coerceAtMost(rows.size - 1)
        if (rows.isNotEmpty() && idx >= 0) {
            val videos = rows[idx].videos
            if (videos.isNotEmpty()) adapter.submitList(videos)
        }
    }

    private fun updateLoadingUI() {
        val loading = viewModel.isLoading.value
        val hasData = viewModel.rows.value.any { it.videos.isNotEmpty() }
        progressBar.visibility = if (loading && !hasData) View.VISIBLE else View.GONE
    }

    private fun updateErrorUI() {
        val error = viewModel.error.value
        val hasData = viewModel.rows.value.any { it.videos.isNotEmpty() }
        textError.visibility = if (error != null && !hasData) View.VISIBLE else View.GONE
        textError.text = error ?: ""
    }

    private fun openDetail(video: VideoItem) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, DetailFragment.newInstance(video))
            .addToBackStack(null)
            .commit()
    }

    private fun showSearchDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "输入关键字搜索"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(40, 255, 255, 255))
            setPadding(32, 24, 32, 24)
            textSize = 16f
        }
        AlertDialog.Builder(requireContext())
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
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }
}
