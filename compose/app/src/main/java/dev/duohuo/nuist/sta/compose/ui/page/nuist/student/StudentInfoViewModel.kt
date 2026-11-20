// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors

package dev.duohuo.nuist.sta.compose.ui.page.nuist.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.data.portal.PortalException
import dev.duohuo.nuist.sta.compose.data.portal.toUserMessage
import dev.duohuo.nuist.sta.compose.data.student.StudentInfo
import dev.duohuo.nuist.sta.compose.data.student.StudentInfoApi
import dev.duohuo.nuist.sta.compose.data.student.StudentInfoCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 「我的」页学生信息卡的状态。
 *
 * 已绑定时先读本机凭据里的学号占位显示，再用信息门户补上姓名与院系班级；
 * 未绑定则直接显示引导文案。
 */
data class StudentInfoUiState(
    val name: String = "",
    val studentId: String = "",
    val orgLine: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val needsCredential: Boolean = false,
    val bound: Boolean = false,
)

class StudentInfoViewModel(
    private val api: StudentInfoApi,
    private val bridge: CampusBridge,
    private val cache: StudentInfoCache,
) : ViewModel() {

    private val _state = MutableStateFlow(
        cache.load()?.let {
            StudentInfoUiState(name = it.name, studentId = it.studentId, orgLine = it.orgLine)
        } ?: StudentInfoUiState(),
    )
    val state: StateFlow<StudentInfoUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null, needsCredential = false) }
        viewModelScope.launch {
            val status = runCatching { bridge.portalStatus() }.getOrNull()
            if (status == null || !status.bound) {
                _state.update {
                    it.copy(bound = false, loading = false, error = null, needsCredential = false)
                }
                return@launch
            }
            _state.update {
                it.copy(
                    bound = true,
                    studentId = it.studentId.ifEmpty { status.studentId.orEmpty() },
                    loading = true,
                )
            }
            try {
                val info: StudentInfo = api.fetch()
                cache.save(info)
                _state.update {
                    it.copy(
                        name = info.name,
                        studentId = info.studentId.ifEmpty { it.studentId },
                        orgLine = info.orgLine,
                        loading = false,
                    )
                }
            } catch (e: Exception) {
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