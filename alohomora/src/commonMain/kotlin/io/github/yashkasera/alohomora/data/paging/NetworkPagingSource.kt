package io.github.yashkasera.alohomora.data.paging

import io.github.yashkasera.alohomora.common.ApiRequest
import io.github.yashkasera.alohomora.domain.repository.NetworkRepository
import io.github.yashkasera.alohomora.utils.paging.FlowPagingSource
import io.github.yashkasera.alohomora.utils.paging.LoadParams
import io.github.yashkasera.alohomora.utils.paging.LoadResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class NetworkPagingSource(
    private val networkRepository: NetworkRepository,
    private val query: String,
    private val method: String,
) : FlowPagingSource<Int, ApiRequest> {

    override fun load(params: LoadParams<Int>): Flow<LoadResult<Int, ApiRequest>> {
        return networkRepository.getAll(
            query = query,
            method = method,
            page = params.key,
            pageSize = params.pageSize,
        ).map { items ->
            LoadResult(
                items = items,
                prevKey = if (params.key > 0) params.key - 1 else null,
                nextKey = if (items.isNotEmpty()) params.key + 1 else null,
            )
        }
    }
}
