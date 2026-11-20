// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.data.bulletin.BulletinApi
import dev.duohuo.nuist.sta.compose.data.portal.toUserMessage
import dev.duohuo.nuist.sta.compose.data.bulletin.BulletinRepository
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 公告列表页状态。 */
data class BulletinListUiState(
    val category: BulletinCategory = BulletinCategories.first(),
    val items: List<Bulletin> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val totalCount: Int = 0,
    val pageNumber: Int = 0,
    val totalPages: Int = 0,
    val hasMore: Boolean = false,
)

/** 公告列表：栏目切换、下拉刷新、滚到底自动翻页。 */
class BulletinListViewModel(private val repository: BulletinRepository) : ViewModel() {

    private val _state = MutableStateFlow(BulletinListUiState())
    val state = _state.asStateFlow()

    private var currentPage: BulletinPage? = null
    private var job: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        job?.cancel()
        currentPage = null
        _state.update {
            it.copy(isLoading = true, isLoadingMore = false, error = null, items = emptyList(), hasMore = false)
        }
        job = viewModelScope.launch {
            try {
                val page = repository.firstPage(_state.value.category)
                currentPage = page
                _state.update {
                    it.copy(
                        items = page.items,
                        isLoading = false,
                        totalCount = page.totalCount,
                        pageNumber = page.pageNumber,
                        totalPages = page.totalPages,
                        hasMore = page.nextPath != null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.toReadableMessage()) }
            }
        }
    }

    fun selectCategory(category: BulletinCategory) {
        if (category == _state.value.category) return
        _state.update { it.copy(category = category) }
        refresh()
    }

    fun loadMore() {
        val page = currentPage ?: return
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isLoadingMore || !snapshot.hasMore) return
        _state.update { it.copy(isLoadingMore = true) }
        job = viewModelScope.launch {
            try {
                val next = repository.nextPage(page)
                if (next == null) {
                    _state.update { it.copy(isLoadingMore = false, hasMore = false) }
                    return@launch
                }
                currentPage = next
                _state.update {
                    it.copy(
                        items = it.items + next.items,
                        isLoadingMore = false,
                        pageNumber = next.pageNumber,
                        totalPages = next.totalPages,
                        totalCount = next.totalCount,
                        hasMore = next.nextPath != null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingMore = false, error = e.toReadableMessage()) }
            }
        }
    }
}

/** 公告详情页状态。 */
sealed interface BulletinDetailUiState {
    data object Loading : BulletinDetailUiState
    data class Content(val detail: BulletinDetail) : BulletinDetailUiState
    data class Error(val message: String) : BulletinDetailUiState
}

/** 公告详情：按 URL 加载，重复进入同一篇时复用缓存。 */
class BulletinDetailViewModel(private val repository: BulletinRepository) : ViewModel() {

    private val _state = MutableStateFlow<BulletinDetailUiState>(BulletinDetailUiState.Loading)
    val state = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()
    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError = _refreshError.asStateFlow()

    private var loadedUrl: String? = null
    private var job: Job? = null

    fun load(url: String) {
        if (url == loadedUrl && _state.value is BulletinDetailUiState.Content) return
        loadedUrl = url
        job?.cancel()
        _state.value = BulletinDetailUiState.Loading
        job = viewModelScope.launch {
            try {
                _state.value = BulletinDetailUiState.Content(repository.detail(url))
            } catch (e: Exception) {
                _state.value = BulletinDetailUiState.Error(e.toReadableMessage())
            }
        }
    }

    fun retry() {
        val url = loadedUrl ?: return
        loadedUrl = null
        load(url)
    }

    /** 下拉刷新：正文还在，等新内容回来再替换，不闪回加载页。 */
    fun refresh() {
        val url = loadedUrl ?: return
        if (_refreshing.value) return
        _refreshing.value = true
        job?.cancel()
        job = viewModelScope.launch {
            try {
                _state.value = BulletinDetailUiState.Content(repository.detail(url, force = true))
                _refreshError.value = null
            } catch (e: Exception) {
                // 已看过正文的话不把它清掉，只在顶部提示一条
                if (_state.value !is BulletinDetailUiState.Content) {
                    _state.value = BulletinDetailUiState.Error(e.toReadableMessage())
                }
                _refreshError.value = e.toReadableMessage()
            } finally {
                _refreshing.value = false
            }
        }
    }
}

/** 首页“信息公告”卡片：最新三条 + 总条数。 */
data class HomeBulletinUiState(
    val items: List<Bulletin> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class HomeBulletinViewModel(private val repository: BulletinRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeBulletinUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val items = repository.preview()
                val total = repository.previewTotalCount()
                _state.update { it.copy(items = items, totalCount = total, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.toReadableMessage()) }
            }
        }
    }
}

internal fun Throwable.toReadableMessage(): String = when (this) {
    is BulletinApi.BulletinAccessException -> message ?: "这条公告暂时无法访问"
    else -> toUserMessage()
}