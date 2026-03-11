package io.github.yashkasera.alohomora.utils.paging

import kotlinx.coroutines.flow.Flow

data class LoadParams<Key>(
    val key: Key,
    val pageSize: Int,
)

data class LoadResult<Key, Item>(
    val items: List<Item>,
    val prevKey: Key?,
    val nextKey: Key?,
)

interface FlowPagingSource<Key, Item> {
    fun load(params: LoadParams<Key>): Flow<LoadResult<Key, Item>>
}
