package com.android.example.paging.pagingwithnetwork.reddit.ui.paging3

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.android.example.paging.pagingwithnetwork.reddit.repository.Paging3Repository
import com.android.example.paging.pagingwithnetwork.reddit.repository.SubredditQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest

@FlowPreview
@UseExperimental(ExperimentalCoroutinesApi::class)
class Paging3SubRedditViewModel(
    private val repo: Paging3Repository
) : ViewModel() {
    private val subredditName = ConflatedBroadcastChannel<SubredditQuery>()

    fun setSubredditName(name: String) {
        val current = subredditName.valueOrNull
        if (current?.query != name) {
            subredditName.offer(
                SubredditQuery(
                    query = name,
                    forceRefresh = false
                )
            )
        }
    }

    fun refresh() {
        subredditName.valueOrNull?.let {
            subredditName.offer(
                it.copy(
                    forceRefresh = true
                )
            )
        }
    }

    val flow = subredditName
        .asFlow()
        .flatMapLatest {
            repo.postsOfSubreddit(it, 10)
        }.cachedIn(viewModelScope)
}