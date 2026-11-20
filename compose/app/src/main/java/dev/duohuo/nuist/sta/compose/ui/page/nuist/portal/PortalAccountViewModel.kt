// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.portal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.data.bridge.PortalStatus
import dev.duohuo.nuist.sta.compose.data.portal.PortalException
import dev.duohuo.nuist.sta.compose.data.portal.toUserMessage
import dev.duohuo.nuist.sta.compose.data.student.StudentInfoApi
import dev.duohuo.nuist.sta.compose.data.student.StudentInfoCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 「测试登录」的结果，对应原项目 `_TestResult`。 */
data class PortalTestResult(
    val ok: Boolean,
    val title: String,
    val details: List<String> = emptyList(),
)

data class PortalAccountUiState(
    val bound: PortalStatus? = null,
    val mask: String = "",
    val bindingTime: String = "",
    val testing: Boolean = false,
    val testStage: String? = null,
    val testResult: PortalTestResult? = null,
    val revokedMessage: String? = null,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val busy: Boolean = false,
    val studentName: String = "",
    val studentOrg: String = "",
    val studentLoading: Boolean = false,
    val studentError: String? = null,
)

/**
 * 「统一门户」状态页（对齐原项目 `portal_status_page.dart`）：
 * 展示绑定状态、凭据信息与操作入口；凭据本身由 Dart 核心保管，这里不做任何加解密。
 */
class PortalAccountViewModel(
    private val bridge: CampusBridge,
    private val studentApi: StudentInfoApi,
    private val studentCache: StudentInfoCache,
) : ViewModel() {

    private val _state = MutableStateFlow(PortalAccountUiState())
    val state: StateFlow<PortalAccountUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val cached = studentCache.load()
        _state.update {
            it.copy(
                studentName = cached?.name ?: it.studentName,
                studentOrg = cached?.orgLine ?: it.studentOrg,
            )
        }
        viewModelScope.launch {
            val status = runCatching { bridge.portalStatus() }.getOrNull()
            val bound = status?.takeIf { it.bound }
            _state.update {
                it.copy(
                    bound = bound,
                    mask = bound?.maskedStudentId.orEmpty(),
                    bindingTime = formatBindingTime(bound?.createdAt),
                )
            }
            if (bound != null) refreshStudent()
        }
    }

    /**
     * 学生信息（姓名 / 院系班级）只在「统一门户」页展示，其它页面不再重复。
     * 未绑定或信息门户没数据时保持空白，界面显示占位。
     */
    fun refreshStudent() {
        if (_state.value.studentLoading) return
        _state.update { it.copy(studentLoading = true, studentError = null) }
        viewModelScope.launch {
            try {
                val info = studentApi.fetch()
                studentCache.save(info)
                _state.update {
                    it.copy(
                        studentName = info.name,
                        studentOrg = info.orgLine,
                        studentLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        studentLoading = false,
                        studentError = e.toUserMessage(),
                    )
                }
            }
        }
    }

    /** 删除本机凭据（不动门户上的通行密钥记录）。 */
    fun unbind() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { bridge.forgetCredential() }
            studentCache.clear()
            _state.value = PortalAccountUiState(message = "已解除绑定", messageIsError = false)
        }
    }

    /**
     * 跑一次真实登录。走 Dart 侧的 `portal.verify`，它绕过票根 SSO 快路径，
     * 因此验证的是通行密钥本身而不是缓存会话。
     */
    fun testLogin() {
        if (_state.value.testing || _state.value.bound == null) return
        _state.update { it.copy(testing = true, testStage = "正在登录…", testResult = null) }
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            runCatching { bridge.verifyCredential() }
                .onSuccess { landed ->
                    val elapsed = System.currentTimeMillis() - startedAt
                    val host = runCatching { java.net.URI(landed).host }.getOrNull().orEmpty()
                    _state.update {
                        it.copy(
                            testing = false,
                            testStage = null,
                            revokedMessage = null,
                            testResult = PortalTestResult(
                                ok = true,
                                title = "登录成功（${elapsed} ms）",
                                details = listOfNotNull(
                                    "落地页：${host.ifEmpty { landed }}",
                                    it.bound?.deviceName?.ifEmpty { "未记录" }?.let { name ->
                                        "凭据：$name"
                                    },
                                ),
                            ),
                        )
                    }
                }
                .onFailure { e ->
                    val elapsed = System.currentTimeMillis() - startedAt
                    val title = when (e) {
                        is PortalException.Network -> "网络不可用（${elapsed} ms）"
                        is PortalException.Credential -> "凭据已失效（${elapsed} ms）"
                        is PortalException.Login -> "登录失败（${elapsed} ms）"
                        else -> "测试登录未完成（${e.javaClass.simpleName}）"
                    }
                    _state.update {
                        it.copy(
                            testing = false,
                            testStage = null,
                            revokedMessage = (e as? PortalException.Credential)?.message,
                            testResult = PortalTestResult(
                                ok = false,
                                title = title,
                                details = listOf(e.toUserMessage(), e.javaClass.simpleName),
                            ),
                        )
                    }
                }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun formatBindingTime(raw: String?): String {
        if (raw.isNullOrBlank() || raw.startsWith("1970-01-01")) return "未记录"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return runCatching {
            OffsetDateTime.parse(raw)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(formatter)
        }.recoverCatching {
            // Dart 侧导出的时间可能不带时区偏移（形如 2026-11-18T16:55:58.510）。
            LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME).format(formatter)
        }.getOrElse { raw }
    }
}