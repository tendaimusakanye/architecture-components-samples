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

package com.android.example.paging.pagingwithnetwork.reddit.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.example.paging.pagingwithnetwork.reddit.vo.RedditPost

@Dao
interface RedditPostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(posts: List<RedditPost>)

    @Query("SELECT * FROM posts WHERE subreddit = :subreddit ORDER BY indexInResponse ASC")
    fun postsBySubreddit(subreddit: String): DataSource.Factory<Int, RedditPost>

    @Query(
        """SELECT * FROM posts
        WHERE subreddit = :subreddit AND indexInResponse >= :indexInResponse AND name NOT IN (:excludes)
        ORDER BY indexInResponse ASC, name ASC
        LIMIT :limit"""
    )
    suspend fun postsAfter(
        subreddit: String,
        indexInResponse: Int,
        excludes : List<String>,
        limit: Int
    ): List<RedditPost>

    @Query(
        """SELECT * FROM posts
        WHERE subreddit = :subreddit AND indexInResponse <= :indexInResponse AND name NOT IN (:excludes)
        ORDER BY indexInResponse DESC, name DESC
        LIMIT :limit"""
    )
    suspend fun postsBefore(
        subreddit: String,
        indexInResponse: Int,
        excludes : List<String>,
        limit: Int
    ): List<RedditPost>

    @Query("DELETE FROM posts WHERE subreddit = :subreddit")
    fun deleteBySubreddit(subreddit: String)

    @Query("SELECT MAX(indexInResponse) + 1 FROM posts WHERE subreddit = :subreddit")
    fun getNextIndexInSubreddit(subreddit: String): Int
}