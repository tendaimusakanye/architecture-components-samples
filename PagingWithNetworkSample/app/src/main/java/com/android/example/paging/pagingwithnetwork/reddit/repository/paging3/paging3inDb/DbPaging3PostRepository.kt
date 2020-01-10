package com.android.example.paging.pagingwithnetwork.reddit.repository.paging3.paging3inDb

import androidx.paging.LoadType
import androidx.paging.PagedData
import androidx.paging.PagedDataFlowBuilder
import androidx.paging.PagedSource
import androidx.paging.PagingConfig
import androidx.room.withTransaction
import com.android.example.paging.pagingwithnetwork.reddit.api.CoroutineRedditApi
import com.android.example.paging.pagingwithnetwork.reddit.api.RedditApi
import com.android.example.paging.pagingwithnetwork.reddit.db.RedditDb
import com.android.example.paging.pagingwithnetwork.reddit.repository.Paging3Repository
import com.android.example.paging.pagingwithnetwork.reddit.vo.RedditPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class DbPaging3PostRepository(
    private val fetchScope: CoroutineScope = GlobalScope,
    private val api: CoroutineRedditApi,
    private val db: RedditDb
) : Paging3Repository {
    var counter = 0
    override fun postsOfSubreddit(subreddit: String, pageSize: Int): Flow<PagedData<RedditPost>> {
        return PagedDataFlowBuilder(
            pagedSourceFactory = {
                SubredditPagedSource(
                    fetchScope = fetchScope,
                    db = db,
                    api = FakeApi(counter++),
                    subreddit = subreddit
                )
            },
            config = PagingConfig.Builder(pageSize).apply {
                setEnablePlaceholders(false)
            }.build()
        ).build()
    }

    private class SubredditPagedSource(
        private val api: CoroutineRedditApi,
        private val fetchScope: CoroutineScope,
        private val db: RedditDb,
        private val subreddit: String
    ) : PagedSource<SubredditPagedSource.PageKey, RedditPost>() {
        override fun getRefreshKeyFromPage(
            indexInPage: Int,
            page: LoadResult.Page<PageKey, RedditPost>
        ): PageKey? {
            return page.data.firstOrNull()?.toPageKey()
        }

        override suspend fun load(params: LoadParams<PageKey>): LoadResult<PageKey, RedditPost> {
            return when (params.loadType) {
                LoadType.REFRESH -> {
                    var posts = db.posts().postsAfter(
                        subreddit = subreddit,
                        indexInResponse = params.key?.indexInResponse ?: -1,
                        excludes = emptyList(),
                        limit = params.loadSize
                    )
                    if (posts.isNotEmpty()) {
                        LoadResult.Page(
                            data = posts,
                            prevKey = posts.firstOrNull()?.toPageKey(),
                            nextKey = posts.lastOrNull()?.toPageKey()
                        )
                    } else {
                        // nothing in disk, fetch sync
                        try {
                            val fetchedItems = fetchTop(params.loadSize)
                            if (fetchedItems > 0) {
                                posts = db.posts().postsAfter(
                                    subreddit = subreddit,
                                    indexInResponse = params.key?.indexInResponse ?: -1,
                                    excludes = emptyList(),
                                    limit = params.loadSize
                                )
                            }
                            LoadResult.Page(
                                data = posts,
                                prevKey = posts.firstOrNull()?.toPageKey(),
                                nextKey = posts.lastOrNull()?.toPageKey()
                            )
                        } catch (error : Throwable) {
                            LoadResult.Error<PageKey, RedditPost>(
                                throwable = error
                            )
                        }
                    }
                }
                LoadType.START -> {
                    val posts = db.posts().postsBefore(
                        subreddit = subreddit,
                        indexInResponse = params.key!!.indexInResponse,
                        excludes = listOf(params.key?.name ?: ""),
                        limit = params.loadSize
                    ).reversed()
                    LoadResult.Page(
                        data = posts,
                        prevKey = posts.firstOrNull()?.toPageKey(),
                        nextKey = posts.lastOrNull()?.toPageKey()
                    )
                }
                LoadType.END -> {
                    val posts = db.posts().postsAfter(
                        subreddit = subreddit,
                        indexInResponse = params.key!!.indexInResponse,
                        excludes = listOf(params.key?.name ?: ""),
                        limit = params.loadSize
                    )
                    return if (posts.isNotEmpty()) {
                        LoadResult.Page(
                            data = posts,
                            prevKey = posts.firstOrNull()?.toPageKey(),
                            nextKey = posts.lastOrNull()?.toPageKey()
                        )
                    } else {
                        val fetchResult = kotlin.runCatching {
                            fetchAfter(params.key!!.name, params.loadSize)
                        }
                        if (fetchResult.isFailure) {
                            LoadResult.Error(
                                throwable = requireNotNull(fetchResult.exceptionOrNull())
                            )
                        } else {
                            // reload
                            val itemsLoaded = fetchResult.getOrDefault(0)
                            if (itemsLoaded > 0) {
                                // reload
                                val reloaded = db.posts().postsAfter(
                                    subreddit = subreddit,
                                    indexInResponse = params.key!!.indexInResponse,
                                    excludes = listOf(params.key?.name ?: ""),
                                    limit = params.loadSize
                                )
                                LoadResult.Page(
                                    data = reloaded,
                                    prevKey = reloaded.firstOrNull()?.toPageKey(),
                                    nextKey = reloaded.lastOrNull()?.toPageKey()
                                )
                            } else {
                                // end of list
                                LoadResult.Page<PageKey, RedditPost>(
                                    data = emptyList(),
                                    prevKey = null,
                                    nextKey = null
                                )

                            }
                        }
                    }
                }
            }
        }

        private suspend fun fetchAfter(key: String, limit: Int): Int {
            val response = api.getTopAfter(
                subreddit = subreddit,
                after = key,
                limit = limit
            )
            val posts = db.withTransaction {
                val offset = db.posts().getNextIndexInSubreddit(subreddit)
                val posts = response.data.children.mapIndexed { index, item ->
                    item.data.also {
                        it.indexInResponse = index + offset
                    }
                }
                db.posts().insert(posts)
                posts
            }
            return posts.size
        }

        suspend fun fetchTop(limit: Int): Int {
            val response = api.getTop(
                subreddit = subreddit,
                limit = limit
            )
            db.withTransaction {
                db.posts().deleteBySubreddit(subreddit)
                db.posts().insert(response.data.children.mapIndexed { index, item ->
                    item.data.indexInResponse = index
                    item.data
                })
            }
            return response.data.children.size
        }

        fun RedditPost.toPageKey() = PageKey(
            name = name,
            indexInResponse = indexInResponse
        )

        data class PageKey(
            val name: String,
            val indexInResponse: Int
        )
    }


    class FakeApi(val id: Int) : CoroutineRedditApi {
        override suspend fun getTop(subreddit: String, limit: Int): RedditApi.ListingResponse {
            delay(5_000)
            val children = fakeRedditPosts(
                subreddit = subreddit,
                start = "aaaaaa",
                limit = limit
            )
            return RedditApi.ListingResponse(
                data = RedditApi.ListingData(
                    children = children,
                    after = children.last().data.name,
                    before = null
                )
            )
        }

        override suspend fun getTopAfter(
            subreddit: String,
            after: String,
            limit: Int
        ): RedditApi.ListingResponse {
            delay(5_000)
            val children = fakeRedditPosts(
                subreddit = subreddit,
                start = after.inc(),
                limit = limit
            )
            return RedditApi.ListingResponse(
                data = RedditApi.ListingData(
                    children = children,
                    after = children.last().data.name,
                    before = null
                )
            )
        }

        override suspend fun getTopBefore(
            subreddit: String,
            before: String,
            limit: Int
        ): RedditApi.ListingResponse {
            TODO("not implemented")
        }

        private fun fakeRedditPost(
            subreddit: String,
            name: String
        ) = RedditPost(
            name = name,
            title = name + " " + id,
            score = 1,
            author = "a",
            subreddit = subreddit,
            num_comments = 0,
            created = 10L,
            thumbnail = null,
            url = null
        )

        fun fakeRedditPosts(
            subreddit: String,
            start: String,
            limit: Int
        ): List<RedditApi.RedditChildrenResponse> {
            var word = start
            val list = mutableListOf<RedditPost>()
            repeat(limit) {
                list.add(
                    fakeRedditPost(
                        subreddit = subreddit,
                        name = word
                    )
                )
                word = word.inc()
            }
            return list.map {
                RedditApi.RedditChildrenResponse(
                    data = it
                )
            }
        }

        fun String.inc(): String {
            var carry = true
            return this.reversed().map {
                if (!carry) {
                    it
                } else {
                    if (it < 'z') {
                        carry = false
                        it + 1
                    } else {
                        'a'
                    }
                }
            }.reversed().joinToString("")
        }

        fun String.dec(): String? {
            var carry = true
            val reversed = this.reversed().map {
                if (!carry) {
                    it
                } else {
                    if (it > 'a') {
                        carry = false
                        it - 1
                    } else {
                        'z'
                    }
                }
            }.reversed().joinToString("")
            if (carry) {
                return null
            }
            return reversed
        }
    }
}