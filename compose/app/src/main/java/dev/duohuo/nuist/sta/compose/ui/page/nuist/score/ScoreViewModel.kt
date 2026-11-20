// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.score

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.data.portal.PortalException
import dev.duohuo.nuist.sta.compose.data.portal.toUserMessage
import dev.duohuo.nuist.sta.compose.data.score.ScoreApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScoreUiState(
    val terms: List<String> = emptyList(),
    val selectedTerm: String = "",
    val report: ScoreReport = ScoreReport("", emptyList()),
    val loading: Boolean = false,
    val error: String? = null,
    val needsCredential: Boolean = false,
)

class ScoreViewModel(private val api: ScoreApi) : ViewModel() {

    private val _state = MutableStateFlow(ScoreUiState(loading = true))
    val state: StateFlow<ScoreUiState> = _state.asStateFlow()

    private var reports: Map<String, ScoreReport> = emptyMap()
    private var loading = false

    init {
        refresh()
    }

    fun selectTerm(term: String) {
        val report = reports[term] ?: return
        _state.update { it.copy(selectedTerm = term, report = report) }
    }

    fun refresh() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, needsCredential = false) }
            try {
                val records = api.fetch()
                val grouped = records.groupBy { it.term.ifEmpty { "未知学期" } }
                val ordered = grouped.entries
                    .sortedByDescending { termCode(it.key) }
                    .associate { entry -> termLabel(entry.key) to ScoreReport(termLabel(entry.key), entry.value) }
                reports = ordered
                val first = ordered.keys.firstOrNull().orEmpty()
                _state.update {
                    it.copy(
                        terms = ordered.keys.toList(),
                        selectedTerm = first,
                        report = ordered[first] ?: ScoreReport(first, emptyList()),
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
            } finally {
                loading = false
            }
        }
    }

    /** 学期码用于排序：`2026-2027-1` → `2026-2027-1`，非规范值原样。 */
    private fun termCode(term: String): String = term.filter { it.isDigit() || it == '-' }

    /** `2026-2027-1` → `2026-2027 秋`；已经是中文标签时原样返回。 */
    private fun termLabel(term: String): String {
        if (term.any { it.isLetter() } && !term.contains('-')) return term
        val parts = term.split('-')
        if (parts.size < 3) return term
        val suffix = when (parts.last()) {
            "1" -> "秋"
            "2" -> "春"
            "3" -> "夏"
            else -> parts.last()
        }
        return "${parts[0]}-${parts[1]} $suffix"
    }
}