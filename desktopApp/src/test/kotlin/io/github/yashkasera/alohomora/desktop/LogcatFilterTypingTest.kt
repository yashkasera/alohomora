package io.github.yashkasera.alohomora.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.github.yashkasera.alohomora.desktop.domain.model.LogEntry
import io.github.yashkasera.alohomora.desktop.domain.repository.LogcatRepository
import io.github.yashkasera.alohomora.desktop.domain.usecase.ClearLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.ObserveLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.StartLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.StopLogcatUseCase
import io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.LogcatFilters
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import io.github.yashkasera.alohomora.ui.theme.AlohomoraTheme
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Typing into the Logcat filter row — three fields at once, each reaching its own filter.
 *
 * **This does not pin the caret bug**, and that is worth stating rather than implying: it passes with and
 * without the fix in `AlohomoraTextField`. `LogcatViewModel.uiState` is one `combine(...).stateIn` hop, while
 * `EventsViewModel` adds a second by deriving from an already-shared `indexed` flow — and only the two-hop lag
 * reproduces a lost caret in a test harness. [SearchFieldTypingTest] is the one that fails without the fix.
 *
 * What this does cover, which nothing else does: three text fields side by side, so a caret or a value leaking
 * between them would show up here.
 */
@OptIn(ExperimentalTestApi::class)
class LogcatFilterTypingTest {

    private class FakeLogcatRepository : LogcatRepository {
        override val entries: StateFlow<List<LogEntry>> = MutableStateFlow(emptyList())
        override fun streamEntries(deviceId: String): Flow<LogEntry> = emptyFlow()
        override fun append(entry: LogEntry) = Unit
        override fun clear() = Unit
    }

    private var viewModel: LogcatViewModel? = null

    @AfterTest
    fun tearDown() {
        viewModel?.close()
    }

    private fun viewModel(): LogcatViewModel {
        val repository = FakeLogcatRepository()
        return LogcatViewModel(
            repository = repository,
            observeLogcatUseCase = ObserveLogcatUseCase(repository),
            startLogcatUseCase = StartLogcatUseCase(repository),
            stopLogcatUseCase = StopLogcatUseCase(),
            clearLogcatUseCase = ClearLogcatUseCase(repository),
        ).also { viewModel = it }
    }

    /** Field order in the row: 0 = Tag, 1 = Package name, 2 = Search. */
    private fun ComposeUiTest.typeInto(index: Int, text: String) {
        onAllNodes(hasSetTextAction())[index].performClick()
        waitForIdle()
        text.forEach { char ->
            onAllNodes(hasSetTextAction())[index].performTextInput(char.toString())
            waitForIdle()
        }
    }

    private fun runFilters(block: ComposeUiTest.(LogcatViewModel) -> Unit) = runComposeUiTest {
        val vm = viewModel()
        setContent {
            AlohomoraTheme {
                val uiState by vm.uiState.collectAsState()
                LogcatFilters(
                    filterState = uiState.filterState,
                    onToggleLevel = vm::toggleLevel,
                    onSelectTag = vm::updateSelectedTag,
                    onPackageChange = vm::updatePackageName,
                    onSearch = vm::updateSearchQuery,
                )
            }
        }
        block(vm)
    }

    @Test
    fun `the search field keeps typed order`() = runFilters { vm ->
        typeInto(index = SEARCH_FIELD, text = "crash")

        assertEquals("crash", vm.uiState.value.filterState.searchQuery)
    }

    @Test
    fun `the tag field keeps typed order`() = runFilters { vm ->
        typeInto(index = TAG_FIELD, text = "OkHttp")

        assertEquals("OkHttp", vm.uiState.value.filterState.selectedTag)
    }

    @Test
    fun `the package field keeps typed order`() = runFilters { vm ->
        typeInto(index = PACKAGE_FIELD, text = "com.example")

        assertEquals("com.example", vm.uiState.value.filterState.packageName)
    }

    @Test
    fun `typing in one field leaves the others alone`() = runFilters { vm ->
        typeInto(index = TAG_FIELD, text = "abc")
        typeInto(index = SEARCH_FIELD, text = "xyz")

        // Each field remembers its own caret; a shared one would splice these together.
        assertEquals("abc", vm.uiState.value.filterState.selectedTag)
        assertEquals("xyz", vm.uiState.value.filterState.searchQuery)
        assertEquals("", vm.uiState.value.filterState.packageName)
    }

    private companion object {
        const val TAG_FIELD = 0
        const val PACKAGE_FIELD = 1
        const val SEARCH_FIELD = 2
    }
}
