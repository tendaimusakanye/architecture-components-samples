package com.android.example.paging.pagingwithnetwork.reddit.ui.paging3

import androidx.lifecycle.ViewModel
import com.android.example.paging.pagingwithnetwork.reddit.repository.Paging3Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@FlowPreview
@ExperimentalCoroutinesApi
class Paging3SubRedditViewModel(
    private val repo: Paging3Repository
) : ViewModel() {
    private val subredditName = ConflatedBroadcastChannel<String>()

    fun setSubredditName(name : String) {
        if (subredditName.valueOrNull != name) {
            subredditName.offer(name.trim())
        }
    }
    fun refresh() {
        subredditName.valueOrNull?.let {
            subredditName.offer(it)
        }
    }
    val flow = subredditName
        .asFlow()
        .flatMapLatest {
            repo.postsOfSubreddit(it, 10)
        }
}