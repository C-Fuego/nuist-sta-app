// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.campus

/**
 * 校内服务地址。
 *
 * 这里存的是 **hex 编码**，不是加密：只为让仓库里不出现明文域名。
 * 拿到源码或 APK 的人都能还原，因此这些值按“公开”对待。
 *
 * 修改步骤：把新值按 ASCII 编码成 hex（例如
 * `python -c "print('your-host'.encode().hex())"`），替换下面的常量即可。
 */
private fun decodeHex(hex: String): String = buildString(hex.length / 2) {
    var i = 0
    while (i + 1 < hex.length) {
        append(hex.substring(i, i + 2).toInt(16).toChar())
        i += 2
    }
}

// 校园根域（nuist.edu.cn）
private const val ROOT_HEX = "6e756973742e6564752e636e"

/** 校园根域。 */
val campusRootDomain: String = decodeHex(ROOT_HEX)

/**
 * 校内各业务系统主机（与根项目 `CampusHosts` 一一对应）。
 *
 * 只存主机名，不带 scheme；拼 URL 一律走 [campusHttps]。
 */
object CampusHosts {
    val authserver = "authserver.$campusRootDomain"
    val jwxt = "jwxt.$campusRootDomain"
    val icard = "icard.$campusRootDomain"
    val iportal = "i.$campusRootDomain"
    val bulletin = "bulletin.$campusRootDomain"
    val labor = "labor.$campusRootDomain"
    val cxcyjy = "cxcyjy.$campusRootDomain"
    val cxxf = "cxxf.$campusRootDomain"
    val vpnController = "client.vpn.$campusRootDomain"
}

/** `https://<host>`，不带尾斜杠。 */
fun campusHttps(host: String): String = "https://$host"

/** 信息公告栏站点根地址，例如 `https://bulletin.nuist.edu.cn/`。 */
val bulletinBaseUrl: String = "https://${CampusHosts.bulletin}/"

/** 把相对路径解析成公告栏站点的绝对地址。 */
fun bulletinAbsUrl(pathOrUrl: String): String {
    if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) return pathOrUrl
    return bulletinBaseUrl.trimEnd('/') + "/" + pathOrUrl.trimStart('/')
}