// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.campus

/**
 * 与仓库根 `lib/core/config/campus_config.dart` 逐字节一致的校内密钥常量。
 *
 * 和根项目一样只做 **hex 编码**（不加密）：它的强度仅等于"顺手看一眼"的门槛，
 * 拿到源码或 APK 都能还原，因此按"公开"对待，不得用于真正的机密。
 *
 * 修改方式：先改根项目的 `campus_config.dart`，再同步这里；两处必须一致。
 */
private const val PASSKEY_EXPORT_KEY_HEX = "4855575173365856766c72736b41577a"

private const val VPN_SPA_KEY_HEX = "456e6c696e6b40313233"

private fun hexToAscii(hex: String): String = buildString(hex.length / 2) {
    var index = 0
    while (index + 1 < hex.length) {
        append(hex.substring(index, index + 2).toInt(16).toChar())
        index += 2
    }
}

/** 通行密钥导出/导入用的内置 AES 密钥（16 个 ASCII 字符）。 */
internal val passkeyExportKey: String by lazy { hexToAscii(PASSKEY_EXPORT_KEY_HEX) }

/** 内置 VPN 的 SPA 敲门密钥（对应根项目 `kVpnSpaKey`），本期未启用。 */
internal val vpnSpaKey: String by lazy { hexToAscii(VPN_SPA_KEY_HEX) }
