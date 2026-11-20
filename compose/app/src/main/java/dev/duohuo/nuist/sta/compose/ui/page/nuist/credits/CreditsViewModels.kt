// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.credits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.data.credits.InnovationCreditApi
import dev.duohuo.nuist.sta.compose.data.credits.InnovationCreditData
import dev.duohuo.nuist.sta.compose.data.credits.LaborScoreApi
import dev.duohuo.nuist.sta.compose.data.credits.LaborScoreData
import dev.duohuo.nuist.sta.compose.data.portal.PortalException
import dev.duohuo.nuist.sta.compose.data.portal.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 双创学分页面的加载状态。 */
data class InnovationUiState(
    val data: InnovationCreditData? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val needsCredential: Boolean = false,
)

class InnovationCreditViewModel(private val api: InnovationCreditApi) : ViewModel() {

    private val _state = MutableStateFlow(InnovationUiState())
    val state: StateFlow<InnovationUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.fetch() }
                .onSuccess { data ->
                    _state.update { it.copy(data = data, loading = false, error = null, needsCredential = false) }
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

/** 劳动积分页面的加载状态。 */
data class LaborUiState(
    val data: LaborScoreData? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val needsCredential: Boolean = false,
)

class LaborScoreViewModel(private val api: LaborScoreApi) : ViewModel() {

    private val _state = MutableStateFlow(LaborUiState())
    val state: StateFlow<LaborUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.fetch() }
                .onSuccess { data ->
                    _state.update { it.copy(data = data, loading = false, error = null, needsCredential = false) }
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
