package io.github.yashkasera.alohomora.utils.paging

sealed class LoadState {
    data object Idle : LoadState()
    data object Loading : LoadState()
    data class Error(val error: Throwable) : LoadState()
}
