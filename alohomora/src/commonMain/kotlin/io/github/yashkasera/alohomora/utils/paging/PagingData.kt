package io.github.yashkasera.alohomora.utils.paging

internal data class PagingData<Item>(
    val items: List<Item>,
    val loadState: LoadState,
    val loadedPages: Int,
)
