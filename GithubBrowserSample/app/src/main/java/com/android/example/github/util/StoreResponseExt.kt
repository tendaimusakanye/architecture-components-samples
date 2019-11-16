package com.android.example.github.util

import com.android.example.github.vo.Resource
import com.nytimes.android.external.store4.ResponseOrigin
import com.nytimes.android.external.store4.StoreResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.scan

fun <T> Flow<StoreResponse<T>>.asResourceFlow() = this.scan(
    initial = Resource.loading(null as? T)
) { acc, latest ->
    when (latest) {
        is StoreResponse.Loading -> Resource.loading(acc.data)
        is StoreResponse.Error -> Resource.error(latest.error.message ?: "unknown error", acc.data)
        is StoreResponse.Data -> if (latest.origin == ResponseOrigin.Fetcher) {
            Resource.success(latest.requireData())
        } else {
            Resource(
                status = acc.status,
                data = latest.requireData(),
                message = acc.message
            )
        }
    }
}