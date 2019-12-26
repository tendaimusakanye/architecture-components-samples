package com.android.example.paging.pagingwithnetwork.reddit.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.example.paging.pagingwithnetwork.GlideApp
import com.android.example.paging.pagingwithnetwork.R
import com.android.example.paging.pagingwithnetwork.reddit.repository.paging3.paging3inDb.InMemoryPaging3PostRepository
import com.android.example.paging.pagingwithnetwork.reddit.ui.paging3.Paging3SubRedditViewModel
import com.android.example.paging.pagingwithnetwork.reddit.ui.paging3.V3PostsAdapter
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_reddit.*
import kotlinx.coroutines.flow.flowOf

class Paging3RedditActivity : AppCompatActivity() {
    private val viewModel by viewModels<Paging3SubRedditViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return Paging3SubRedditViewModel(
                    InMemoryPaging3PostRepository()
                ) as T
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reddit)
        viewModel.setSubredditName("androiddev")
        initSearch()
        initAdapter()


    }

    private fun initSearch() {
        input.setAsSearch {
            if (it.isNotBlank()) {
                viewModel.setSubredditName(it)
                list.scrollToPosition(0)
            }
        }
    }

    fun initAdapter() {
        val glide = GlideApp.with(this)
        val adapter = V3PostsAdapter(glide = glide)
        list.adapter = adapter
        adapter.connect(viewModel.flow, lifecycleScope)
    }
}
