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

package com.android.example.github.ui.user

import androidx.lifecycle.ViewModel
import com.android.example.github.di.GithubStore
import com.android.example.github.repository.RepoRepository
import com.android.example.github.repository.UserRepository
import com.android.example.github.testing.OpenForTesting
import com.android.example.github.util.asResourceFlow
import com.nytimes.android.external.store4.StoreRequest
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@OpenForTesting
class UserViewModel2
@Inject constructor(
    githubStore: GithubStore
) : ViewModel() {
    private val _login = ConflatedBroadcastChannel<String?>()
    val repositories = _login.asFlow().flatMapLatest {
        if (it == null) {
            flowOf()
        } else {
            githubStore.userRepositoryStore.stream(StoreRequest.cached(it, refresh = true))
        }
    }.asResourceFlow()
    val user = _login.asFlow().flatMapLatest {
        if (it == null) {
            flowOf()
        } else {
            githubStore.userStore.stream(StoreRequest.cached(it, refresh = true))
        }
    }.asResourceFlow()

    fun setLogin(login: String?) {
        if (_login.valueOrNull != login) {
            _login.offer(login)
        }
    }

    fun retry() {
        _login.valueOrNull?.let {
            _login.offer(it)
        }
    }
}
