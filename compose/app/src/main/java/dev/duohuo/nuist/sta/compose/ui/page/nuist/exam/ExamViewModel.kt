// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.data.exam.ExamApi
import dev.duohuo.nuist.sta.compose.data.exam.ExamItem
import dev.duohuo.nuist.sta.compose.data.portal.PortalException
import dev.duohuo.nuist.sta.compose.data.portal.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ExamUiState(
    val items: List<ExamItem> = emptyList(),
    val termCode: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val needsCredential: Boolean = false,
) {
    /** 还没考的场次（列表已按时间升序），首页「待考科目」用它。 */
    val upcoming: List<ExamItem>
        get() {
            val now = LocalDateTime.now()
            return items.filter { LocalDateTime.of(it.date, it.end).isAfter(now) }
        }

    /** 最近一场待考。 */
    val next: ExamItem? get() = upcoming.firstOrNull()
}

class ExamViewModel(private val api: ExamApi) : ViewModel() {

    private val _state = MutableStateFlow(ExamUiState())
    val state: StateFlow<ExamUiState> = _state.asStateFlow()

    /**
     * 读一次考试安排。
     *
     * 数据源是课表的本机日程（不再单独查教务），所以重读的代价很低：每次进入页面都重读，
     * 不按学期码缓存——否则在课表里改过时间的考试会一直显示旧时间。
     * [force] 表示用户主动下拉刷新，此时即使已有内容也显示刷新指示。
     */
    fun refresh(termCode: String, force: Boolean = false) {
        if (termCode.isEmpty()) return
        val current = _state.value
        if (current.loading) return
        // 已有内容时的自动重读是「悄悄刷新」，不把列表退回加载态。
        _state.update {
            it.copy(loading = force || it.items.isEmpty(), error = null, termCode = termCode)
        }
        viewModelScope.launch {
            runCatching { api.fetch() }
                .onSuccess { items ->
                    _state.update {
                        it.copy(items = items, loading = false, error = null, needsCredential = false)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = e.toUserMessage(),
                            needsCredential = e is PortalException.Credential,
                        )
                    }
                }
        }
    }
}
