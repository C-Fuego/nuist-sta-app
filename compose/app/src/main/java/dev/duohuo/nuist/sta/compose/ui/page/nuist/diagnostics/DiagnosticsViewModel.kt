// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors

package dev.duohuo.nuist.sta.compose.ui.page.nuist.diagnostics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 诊断与日志页：把 [dev.duohuo.nuist.sta.compose.util.timber.FileLoggingTree] 落在
 * `cacheDir/logs` 里的日志读出来给用户看 / 复制 / 导出。
 *
 * 页面上只展示「有没有出错、能不能重试」，原始的异常文本（主机名、端口、栈）都留在这里，
 * 需要排查时再导出。
 */
class DiagnosticsViewModel(private val context: Context) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val fileCount: Int = 0,
        val byteCount: Long = 0,
        val preview: String = "",
        val truncated: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val logDir: File
        get() = File(context.cacheDir, LOG_DIR_NAME)

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.loading && _state.value.preview.isNotEmpty()) return
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) { readLogs() }
            _state.value = snapshot.copy(loading = false)
        }
    }

    fun clear() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                logDir.listFiles()?.forEach { runCatching { it.delete() } }
            }
            _state.value = UiState(loading = false, message = "已清空本机日志")
        }
    }

    private fun readLogs(): UiState {
        val files = logDir.listFiles { file -> file.isFile }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        if (files.isEmpty()) {
            return UiState(loading = false, preview = "", fileCount = 0, byteCount = 0)
        }
        val totalBytes = files.sumOf { it.length() }
        val newest = files.first()
        val text = runCatching {
            newest.bufferedReader().use { reader ->
                // 预览只取尾部，避免大文件把页面卡住
                val content = reader.readText()
                if (content.length > PREVIEW_CHARS) {
                    content.takeLast(PREVIEW_CHARS)
                } else {
                    content
                }
            }
        }.getOrElse { "读取日志失败：${it.message}" }
        return UiState(
            loading = false,
            fileCount = files.size,
            byteCount = totalBytes,
            preview = text,
            truncated = text.isNotEmpty() && newest.length() > PREVIEW_CHARS,
        )
    }

    fun exportFileName(): String =
        "nuist-logs-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.log"

    /** 导出用的是完整内容（不是页面上的尾部预览）。 */
    fun exportText(): String = runCatching {
        logDir.listFiles { file -> file.isFile }
            ?.sortedBy { it.lastModified() }
            ?.joinToString("\n") { file -> runCatching { file.readText() }.getOrDefault("") }
            .orEmpty()
    }.getOrDefault("")

    private companion object {
        const val LOG_DIR_NAME = "logs"
        const val PREVIEW_CHARS = 60_000
    }
}