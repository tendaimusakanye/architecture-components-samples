package com.android.example.paging.pagingwithnetwork.reddit.repository

import androidx.paging.PagedData
import com.android.example.paging.pagingwithnetwork.reddit.vo.RedditPost
import kotlinx.coroutines.flow.Flow


interface Paging3Repository {
    fun postsOfSubreddit(subreddit: String, pageSize: Int): Flow<PagedData<RedditPost>>

    enum class Type {
        IN_MEMORY_BY_PAGE,
        DB
    }
}