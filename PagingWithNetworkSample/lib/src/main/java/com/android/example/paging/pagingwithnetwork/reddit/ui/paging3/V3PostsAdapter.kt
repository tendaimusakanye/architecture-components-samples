package com.android.example.paging.pagingwithnetwork.reddit.ui.paging3

import android.view.ViewGroup
import androidx.paging.PagedData
import androidx.paging.PagedDataAdapter
import com.android.example.paging.pagingwithnetwork.GlideRequests
import com.android.example.paging.pagingwithnetwork.reddit.ui.PostsAdapter.Companion.POST_COMPARATOR
import com.android.example.paging.pagingwithnetwork.reddit.ui.RedditPostViewHolder
import com.android.example.paging.pagingwithnetwork.reddit.vo.RedditPost

class V3PostsAdapter(
    private val glide: GlideRequests
) : PagedDataAdapter<RedditPost, RedditPostViewHolder>(
    diffCallback = POST_COMPARATOR
) {
    override fun onBindViewHolder(holder: RedditPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RedditPostViewHolder {
        return RedditPostViewHolder.create(parent, glide)
    }
}