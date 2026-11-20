// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors

package dev.duohuo.nuist.sta.compose.ui.page.nuist.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.data.academic.AcademicApi
import dev.duohuo.nuist.sta.compose.data.academic.AcademicSnapshot
import dev.duohuo.nuist.sta.compose.data.portal.PortalException
import dev.duohuo.nuist.sta.compose.data.portal.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 学习页「学业进度」的真实数据状态；没有数据时整卡显示 `--`。 */
data class AcademicUiState(
    val summary: AcademicSummary? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val needsCredential: Boolean = false,
) {
    /** 接口没数据时的空壳，避免界面出现上一版的假数字。 */
    val display: AcademicSummary
        get() = summary ?: EMPTY
    companion object {
        val EMPTY = AcademicSummary(
            averageGradePoint = "",
            gpa = "",
            averageScore = "",
            earnedCredits = "",
            requiredCredits = "",
            classRank = "",
            majorRank = "",
            passRate = "",
            weightedAverageScore = "",
        )
    }
}

class AcademicViewModel(private val api: AcademicApi) : ViewModel() {

    private val _state = MutableStateFlow(AcademicUiState())
    val state: StateFlow<AcademicUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null, needsCredential = false) }
        viewModelScope.launch {
            try {
                _state.update {
                    it.copy(summary = api.fetch().toUi(), loading = false)
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

    private fun AcademicSnapshot.toUi() = AcademicSummary(
        averageGradePoint = averageGradePoint,
        gpa = gpa,
        averageScore = averageScore,
        earnedCredits = earnedCredits,
        requiredCredits = requiredCredits,
        classRank = classRank,
        majorRank = majorRank,
        passRate = passRate,
        weightedAverageScore = weightedAverageScore,
    )
}