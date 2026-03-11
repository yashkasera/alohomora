package io.github.yashkasera.alohomora.data.paging

import io.github.yashkasera.alohomora.common.Analytics
import io.github.yashkasera.alohomora.domain.repository.EventRepository
import io.github.yashkasera.alohomora.utils.paging.FlowPagingSource
import io.github.yashkasera.alohomora.utils.paging.LoadParams
import io.github.yashkasera.alohomora.utils.paging.LoadResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class EventPagingSource(
    private val eventRepository: EventRepository,
    private val query: String,
) : FlowPagingSource<Int, Analytics> {

    override fun load(params: LoadParams<Int>): Flow<LoadResult<Int, Analytics>> {
        return eventRepository.getEvents(
            query = query,
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
