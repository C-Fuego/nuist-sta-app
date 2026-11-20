// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.portal

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.duohuo.nuist.sta.compose.data.campus.CampusHosts
import dev.duohuo.nuist.sta.compose.data.campus.campusHttps
import dev.duohuo.nuist.sta.compose.ui.kit.MiniAppBackButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.data.bridge.toChannelValue
import org.koin.compose.koinInject

/**
 * 绑定统一门户：内嵌系统 WebView 打开门户登录页，登录后落到个人中心即注入
 * `assets/js/portal_passkey.js` 完成软件 Passkey 注册，结果经 JavaScript 桥回传。
 *
 * 对齐根项目 `portal_bind_page.dart`：桌面 UA、http→https 改写、脚本幂等、
 * 注册成功后保存凭据并把旧会话清掉。
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PortalBindScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val bridge: CampusBridge = koinInject()
    val scope = remember { CoroutineScope(Dispatchers.Main.immediate) }
    val json = remember { Json { ignoreUnknownKeys = true } }
    val context = LocalContext.current

    val loginUrl = "${campusHttps(CampusHosts.authserver)}/authserver/login"
    val allowedOrigin = campusHttps(CampusHosts.authserver)
    val deviceName = remember { newDeviceName() }

    var progress by remember { mutableFloatStateOf(0f) }
    var hint by remember {
        mutableStateOf(
            "请登录统一门户\n\n" +
                "正常登录后再次验证密码即可完成注册。\n\n" +
                "本机只保存通行密钥，不保存门户密码，改密码不受影响。",
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var bound by remember { mutableStateOf(false) }

    val currentOnDone by rememberUpdatedState(onDone)

    fun handleMessage(raw: String) {
        val element = runCatching { json.parseToJsonElement(raw) }.getOrNull()
        val obj = element as? JsonObject ?: return
        when ((obj["type"] as? JsonPrimitive)?.content) {
            "status" -> {
                hint = when ((obj["stage"] as? JsonPrimitive)?.content) {
                    "navigating" -> "正在跳转到通行密钥页…"
                    "waiting_page" -> "等待页面加载…"
                    "enabling_biometrics" -> "正在开启生物识别登录…"
                    "verifying" -> "请在页面中完成身份验证（登录密码 + 验证码）"
                    "registering" -> "正在注册通行密钥…"
                    else -> hint
                }
                error = null
            }

            "error" -> error = (obj["message"] as? JsonPrimitive)?.content ?: "注册脚本失败"

            "success" -> {
                val bundleObj = obj["bundle"] as? JsonObject
                if (bundleObj == null) {
                    error = "注册结果格式异常"
                    return
                }
                val payload = bundleObj.toChannelValue() as? Map<String, Any?>
                if (payload == null) {
                    error = "注册结果解析失败"
                    return
                }
                val withMeta = payload.toMutableMap().apply {
                    put("deviceName", deviceName)
                    put("createdAt", java.time.Instant.now().toString())
                }
                saving = true
                scope.launch {
                    runCatching { bridge.bindCredential(withMeta) }
                        .onSuccess { bound = true }
                        .onFailure { error = "绑定未完成，请重试" }
                    saving = false
                }
            }
        }
    }

    fun injectScript(view: WebView) {
        val source = runCatching {
            context.assets.open("js/portal_passkey.js").bufferedReader().use { it.readText() }
        }.getOrNull()
        if (source == null) {
            error = "注册脚本未能加载，请重新打开绑定页面"
            return
        }
        val script = source
            .replaceFirst("__DEVICE_NAME__", jsonString(deviceName))
            .replaceFirst("__PORTAL_ALLOWED_ORIGIN__", jsonString(allowedOrigin))
        view.evaluateJavascript(script, null)
    }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = DESKTOP_UA
            // 门户是 PC 版页面：按桌面视口渲染再整页缩放，否则表单会挤成一条、
            // 点击位置和看到的位置对不上（表现为「点不到密码框」）。
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.setSupportZoom(true)
            settings.textZoom = 100
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun postMessage(raw: String) {
                        Handler(Looper.getMainLooper()).post { handleMessage(raw) }
                    }
                },
                "NuistPasskey",
            )
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress = newProgress / 100f
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    val https = rewriteToHttps(url) ?: return false
                    view.loadUrl(https)
                    return true
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) = Unit

                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                    if (url != null && url.contains("personcenter", ignoreCase = true)) injectScript(view)
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    if (url != null && url.contains("personcenter", ignoreCase = true)) injectScript(view)
                }
            }
            loadUrl(loginUrl)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("绑定通行密钥", modifier = Modifier.padding(start = 12.dp)) },
                navigationIcon = { MiniAppBackButton(onBack = onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
    Column(Modifier.fillMaxSize().padding(inner)) {
        if (progress < 1f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = error ?: hint,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (error != null) {
                TextButton(onClick = {
                    error = null
                    webView.reload()
                }) { Text("重试") }
            } else {
                TextButton(onClick = {
                    clearPortalCookies()
                    webView.loadUrl(loginUrl)
                    hint = "已退出登录，请用要绑定的账号重新登录"
                }) { Text("换个账号") }
            }
        }
        // 键盘弹出时收缩 WebView，避免密码框被输入法盖住（edge-to-edge 下
        // adjustResize 不再自动生效，必须自己吃 imePadding）。
        Box(Modifier.weight(1f).fillMaxWidth().imePadding()) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    }

    if (bound) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("绑定成功") },
            text = { Text("已注册通行密钥「$deviceName」并保存到本机安全存储。") },
            confirmButton = {
                TextButton(onClick = {
                    bound = false
                    currentOnDone()
                }) { Text("完成") }
            },
        )
    }
    if (saving) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("正在保存") },
            text = { Text("正在把凭据写入本机安全存储…") },
            confirmButton = {},
        )
    }
}

private const val DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

private fun rewriteToHttps(url: String): String? {
    val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return null
    if (!uri.scheme.equals("http", ignoreCase = true)) return null
    if (!uri.host.equals(CampusHosts.authserver, ignoreCase = true)) return null
    return url.replaceFirst("http://", "https://", ignoreCase = true)
}

private fun clearPortalCookies() {
    val manager = CookieManager.getInstance()
    manager.removeAllCookies(null)
    manager.flush()
}

private fun jsonString(value: String): String = buildString {
    append('"')
    for (ch in value) {
        when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            else -> append(ch)
        }
    }
    append('"')
}

private fun newDeviceName(): String {
    val letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val random = java.security.SecureRandom()
    val suffix = (1..4).map { letters[random.nextInt(letters.length)] }.joinToString("")
    return "NUIST++(Android)_$suffix"
}