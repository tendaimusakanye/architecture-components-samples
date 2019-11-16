package com.android.example.github.di

import com.android.example.github.api.GithubService
import com.android.example.github.db.GithubDb
import com.android.example.github.vo.Repo
import com.android.example.github.vo.User
import com.nytimes.android.external.store4.FlowStoreBuilder
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class GithubStore @Inject constructor(
    private val db: GithubDb,
    private val githubService: GithubService
) {
    val userStore = FlowStoreBuilder.fromNonFlow<String, User, User>(
        fetcher = githubService::getUserSuspend
    ).persister(
        reader = db.userDao()::findByLoginFlow,
        writer = { id, user ->
            db.userDao().insertSuspend(user)
        }
    ).build()
    val userRepositoryStore = FlowStoreBuilder.fromNonFlow<String, List<Repo>, List<Repo>>(
        fetcher = githubService::getReposSuspend
    ).persister(
        reader = db.repoDao()::loadRepositoriesFlow,
        writer = { id, repos ->
            db.repoDao().insertReposSuspend(repos)
        }
    ).build()
}