package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.desktop.domain.model.ChronicleCommit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChronicleStore {
    private val _commits = MutableStateFlow<List<ChronicleCommit>>(emptyList())
    val commits: StateFlow<List<ChronicleCommit>> = _commits.asStateFlow()

    fun replace(commits: List<ChronicleCommit>) {
        _commits.value = commits
    }

    fun clear() {
        _commits.value = emptyList()
    }
}
