package com.android.example.paging.pagingwithnetwork.reddit.repository.paging3.paging3inDb

import androidx.paging.LoadType
import androidx.paging.PagedData
import androidx.paging.PagedDataFlowBuilder
import androidx.paging.PagedList
import androidx.paging.PagedSource
import com.android.example.paging.pagingwithnetwork.reddit.api.CoroutineRedditApi
import com.android.example.paging.pagingwithnetwork.reddit.api.RedditApi
import com.android.example.paging.pagingwithnetwork.reddit.repository.Paging3Repository
import com.android.example.paging.pagingwithnetwork.reddit.vo.RedditPost
import kotlinx.coroutines.flow.Flow

class InMemoryPaging3PostRepository : Paging3Repository {
    private val api = CoroutineRedditApi.create()
    override fun postsOfSubreddit(
        subreddit : String,
        pageSize:Int
    ) : Flow<PagedData<RedditPost>> {
        return PagedDataFlowBuilder(
            pagedSourceFactory = {
                SubredditPagedSource(
                    api = api,
                    subreddit = subreddit
                )
            },
            config = PagedList.Config.Builder().apply {
                setEnablePlaceholders(false)
                setPageSize(pageSize)
            }.build()
        ).build()
    }

    private class SubredditPagedSource(
        private val api : CoroutineRedditApi,
        private val subreddit: String
    ) : PagedSource<String, RedditPost>() {
        override suspend fun load(params: LoadParams<String>): LoadResult<String, RedditPost> {
            return when(params.loadType) {
                LoadType.REFRESH -> {
                    api.getTop(
                        subreddit = subreddit,
                        limit = params.pageSize).toPage()
                }
                LoadType.START -> {
                    api.getTopBefore(
                        subreddit = subreddit,
                        before = params.key!!,
                        limit = params.loadSize).toPage()
                }
                LoadType.END -> {
                    api.getTopAfter(
                        subreddit = subreddit,
                        after = params.key!!,
                        limit = params.loadSize
                    ).toPage()
                }
            }
        }
        private fun RedditApi.ListingResponse.toPage() = PagedSource.LoadResult.Page(
            data = data.children.map { it.data },
            prevKey = data.before,
            nextKey = data.after
        )
    }

}