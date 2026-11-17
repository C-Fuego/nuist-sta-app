package dev.duohuo.nuist.sta.compose.features.score

import androidx.lifecycle.ViewModel
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

    fun selectTerm(term: String) = _state.update {
        it.copy(selectedTerm = term, report = ScoreMock.report(term))
    }

    fun refresh() = _state.update {
        it.copy(report = ScoreMock.report(it.selectedTerm))
    }
}
