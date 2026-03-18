package io.github.yashkasera.alohomora.utils.paging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class FlowPager<Key, Item>(
    private val config: PagingConfig,
    private val initialKey: Key,
    private val getNextKey: (Key) -> Key,
    private val pagingSourceFactory: () -> FlowPagingSource<Key, Item>,
) {
    private var pagingSource = pagingSourceFactory()

    private val loadedPages = mutableMapOf<Key, List<Item>>()
    private val pageJobs = mutableMapOf<Key, Job>()
    private var currentKey: Key = initialKey
    private var isEndReached = false
    private var scope: CoroutineScope? = null

    // Use a MutableStateFlow to trigger updates when pages change
    private val _pagesUpdateTrigger = MutableStateFlow(0)

    private val _pagingData = MutableStateFlow(
        PagingData<Item>(
            items = emptyList(),
            loadState = LoadState.Idle,
            loadedPages = 0,
        ),
    )
    val pagingData: StateFlow<PagingData<Item>> = _pagingData.asStateFlow()

    fun loadNextPage() {
        val scope = this.scope ?: return
        if (isEndReached || _pagingData.value.loadState is LoadState.Loading) return

        _pagingData.update { it.copy(loadState = LoadState.Loading) }

        val key = currentKey

        val job = pagingSource.load(LoadParams(key, config.pageSize))
            .map { result ->
                // Store the items for this page
                loadedPages[key] = result.items

                if (result.items.size < config.pageSize) {
                    isEndReached = true
                } else {
                    currentKey = result.nextKey ?: getNextKey(key)
                }

                // Trigger update
                _pagesUpdateTrigger.value += 1

                result
            }
            .launchIn(scope)

        pageJobs[key] = job
    }

    private fun updatePagingData() {
        // Flatten all loaded pages - LinkedHashMap preserves insertion order
        val allItems = loadedPages.values.flatten()

        _pagingData.value = PagingData(
            items = allItems,
            loadState = LoadState.Idle,
            loadedPages = loadedPages.size,
        )
    }

    fun refresh() {
        val scope = this.scope ?: return

        scope.launch {
            pageJobs.values.forEach { it.cancelAndJoin() }
            pageJobs.clear()
            loadedPages.clear()

            currentKey = initialKey
            isEndReached = false
            pagingSource = pagingSourceFactory()

            _pagingData.value = PagingData(
                items = emptyList(),
                loadState = LoadState.Idle,
                loadedPages = 0,
            )

            _pagesUpdateTrigger.value += 1

            loadNextPage()
        }
    }

    fun cachedIn(scope: CoroutineScope): FlowPager<Key, Item> {
        this.scope = scope

        // Collect the trigger to update paging data when pages change
        scope.launch {
            _pagesUpdateTrigger.collect {
                updatePagingData()
            }
        }

        return this
    }
}
