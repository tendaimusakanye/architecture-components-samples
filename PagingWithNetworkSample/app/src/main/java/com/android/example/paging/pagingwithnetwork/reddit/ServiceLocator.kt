/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.example.paging.pagingwithnetwork.reddit

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.android.example.paging.pagingwithnetwork.reddit.api.CoroutineRedditApi
import com.android.example.paging.pagingwithnetwork.reddit.api.RedditApi
import com.android.example.paging.pagingwithnetwork.reddit.db.RedditDb
import com.android.example.paging.pagingwithnetwork.reddit.repository.Paging3Repository
import com.android.example.paging.pagingwithnetwork.reddit.repository.RedditPostRepository
import com.android.example.paging.pagingwithnetwork.reddit.repository.inDb.DbRedditPostRepository
import com.android.example.paging.pagingwithnetwork.reddit.repository.inMemory.byItem.InMemoryByItemRepository
import com.android.example.paging.pagingwithnetwork.reddit.repository.inMemory.byPage.InMemoryByPageKeyRepository
import com.android.example.paging.pagingwithnetwork.reddit.repository.paging3.paging3inDb.DbPaging3PostRepository
import com.android.example.paging.pagingwithnetwork.reddit.repository.paging3.paging3inDb.InMemoryPaging3PostRepository
import kotlinx.coroutines.GlobalScope
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Super simplified service locator implementation to allow us to replace default implementations
 * for testing.
 */
interface ServiceLocator {
    companion object {
        private val LOCK = Any()
        private var instance: ServiceLocator? = null
        fun instance(context: Context): ServiceLocator {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = DefaultServiceLocator(
                        app = context.applicationContext as Application,
                        useInMemoryDb = true
                    )
                }
                return instance!!
            }
        }

        /**
         * Allows tests to replace the default implementations.
         */
        @VisibleForTesting
        fun swap(locator: ServiceLocator) {
            instance = locator
        }
    }

    fun getRepository(type: RedditPostRepository.Type): RedditPostRepository

    fun getV3Repository(type: Paging3Repository.Type): Paging3Repository

    fun getNetworkExecutor(): Executor

    fun getDiskIOExecutor(): Executor

    fun getRedditApi(): RedditApi
}

/**
 * default implementation of ServiceLocator that uses production endpoints.
 */
open class DefaultServiceLocator(val app: Application, val useInMemoryDb: Boolean) :
    ServiceLocator {
    // thread pool used for disk access
    @Suppress("PrivatePropertyName")
    private val DISK_IO = Executors.newSingleThreadExecutor()

    // thread pool used for network requests
    @Suppress("PrivatePropertyName")
    private val NETWORK_IO = Executors.newFixedThreadPool(5)

    private val db by lazy {
        RedditDb.create(app, useInMemoryDb)
    }

    private val api by lazy {
        RedditApi.create()
    }

    private val coroutineApi by lazy {
        CoroutineRedditApi.create()
    }

    override fun getRepository(type: RedditPostRepository.Type): RedditPostRepository {
        return when (type) {
            RedditPostRepository.Type.IN_MEMORY_BY_ITEM -> InMemoryByItemRepository(
                redditApi = getRedditApi(),
                networkExecutor = getNetworkExecutor()
            )
            RedditPostRepository.Type.IN_MEMORY_BY_PAGE -> InMemoryByPageKeyRepository(
                redditApi = getRedditApi(),
                networkExecutor = getNetworkExecutor()
            )
            RedditPostRepository.Type.DB -> DbRedditPostRepository(
                db = db,
                redditApi = getRedditApi(),
                ioExecutor = getDiskIOExecutor()
            )
        }
    }

    override fun getV3Repository(type: Paging3Repository.Type): Paging3Repository {
       return when (type) {
            Paging3Repository.Type.IN_MEMORY_BY_PAGE -> InMemoryPaging3PostRepository(
                api = coroutineApi
            )
            Paging3Repository.Type.DB -> DbPaging3PostRepository(
                fetchScope = GlobalScope,
                db = db,
                api = coroutineApi
            )
        }
    }

    override fun getNetworkExecutor(): Executor = NETWORK_IO

    override fun getDiskIOExecutor(): Executor = DISK_IO

    override fun getRedditApi(): RedditApi = api
}