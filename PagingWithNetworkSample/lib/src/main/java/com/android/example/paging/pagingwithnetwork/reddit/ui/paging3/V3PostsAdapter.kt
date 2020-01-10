package com.android.example.paging.pagingwithnetwork.reddit.ui.paging3

import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadType
import androidx.paging.PagedDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.example.lib.R
import com.android.example.paging.pagingwithnetwork.GlideRequests
import com.android.example.paging.pagingwithnetwork.reddit.ui.NetworkStateItemViewHolder
import com.android.example.paging.pagingwithnetwork.reddit.ui.PostsAdapter.Companion.POST_COMPARATOR
import com.android.example.paging.pagingwithnetwork.reddit.ui.RedditPostViewHolder
import com.android.example.paging.pagingwithnetwork.reddit.vo.RedditPost

class V3PostsAdapter(
    private val glide: GlideRequests
) : PagedDataAdapter<RedditPost, RecyclerView.ViewHolder>(
    diffCallback = POST_COMPARATOR
) {
    private var loadMoreState : LoadState = LoadState.Idle
        set(value) {
            val didHaveExtraRow = hasExtraRow()
            field = value
            val hasExtraRow = hasExtraRow()
            if (didHaveExtraRow != hasExtraRow) {
                if (didHaveExtraRow) {
                    notifyItemRemoved(itemCount - 1)
                } else {
                    notifyItemInserted(itemCount)
                }
            }
        }

    init {
        addLoadStateListener { loadType, loadState ->
            if (loadType == LoadType.END) {
                loadMoreState = loadState
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.network_state_item
        } else {
            R.layout.reddit_post_item
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is RedditPostViewHolder -> holder.bind(getItem(position))
            is NetworkStateItemViewHolder -> holder.bindTo(loadMoreState)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            R.layout.network_state_item -> NetworkStateItemViewHolder.create(parent, ::retry)
            R.layout.reddit_post_item -> RedditPostViewHolder.create(parent, glide)
            else -> throw IllegalArgumentException("invalid type $viewType")
        }
    }

    private fun hasExtraRow() = loadMoreState == LoadState.Loading || loadMoreState is LoadState.Error
}