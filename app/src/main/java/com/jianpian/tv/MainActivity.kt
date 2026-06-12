package com.jianpian.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.jianpian.tv.ui.browse.BrowseFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主 Activity — Leanback 单 Activity 架构
 *
 * 承载浏览、搜索、详情等 Fragment 的容器。
 * 播放器使用独立的 PlaybackActivity 实现全屏播放。
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, BrowseFragment())
                .commit()
        }
    }
}
