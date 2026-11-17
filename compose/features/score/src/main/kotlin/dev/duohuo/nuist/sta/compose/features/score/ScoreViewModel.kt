package dev.duohuo.nuist.sta.compose.features.score

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScoreUiState(
    val terms: List<String> = ScoreMock.terms,
    val selectedTerm: String = ScoreMock.terms.first(),
    val report: ScoreReport = ScoreMock.report(ScoreMock.terms.first()),
    val loading: Boolean = false,
)

class ScoreViewModel : ViewModel() {
    private val _state = MutableStateFlow(ScoreUiState())
    val state: StateFlow<ScoreUiState> = _state.asStateFlow()

    /** mock 异步延时：真实数据接入后替换为网络请求，期间展示 M3E LoadingIndicator。 */
    private suspend fun simulateFetch() = kotlinx.coroutines.delay(500)

    fun selectTerm(term: String) {
        viewModelScope.launch {
            _state.update { it.copy(selectedTerm = term, loading = true) }
            simulateFetch()
            _state.update { it.copy(report = ScoreMock.report(term), loading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            simulateFetch()
            _state.update { it.copy(report = ScoreMock.report(it.selectedTerm), loading = false) }
        }
    }
}
