package io.github.yashkasera.alohomora.utils.paging

internal class PagingConfig(
    val pageSize: Int,
    val prefetchDistance: Int = pageSize / 2,
    val maxSize: Int = Int.MAX_VALUE,
)
