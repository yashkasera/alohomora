package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.desktop.domain.model.GitHistoryCommit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GitHistoryStore {
    private val _commits = MutableStateFlow<List<GitHistoryCommit>>(emptyList())
    val commits: StateFlow<List<GitHistoryCommit>> = _commits.asStateFlow()

    fun replace(commits: List<GitHistoryCommit>) {
        _commits.value = commits
    }

    fun clear() {
        _commits.value = emptyList()
    }
}
