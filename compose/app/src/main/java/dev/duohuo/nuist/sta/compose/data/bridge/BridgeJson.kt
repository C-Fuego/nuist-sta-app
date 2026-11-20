// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.bridge

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** 读取桥返回 JSON 的小工具：字段缺失一律给默认值，不抛异常。 */
internal fun JsonObject.str(key: String, fallback: String = ""): String =
    (this[key] as? JsonPrimitive)
        ?.takeIf { it !is kotlinx.serialization.json.JsonNull && it.content != "null" }
        ?.content
        ?: fallback

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.arr(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

/**
 * 桥返回的数字可能是 `1` 也可能是 `1.0`（Dart 侧经过一次 JSON 往返后浮点化），
 * 所以整数一律先按 double 取再截断，避免 `intOrNull` 直接把浮点判成缺失。
 */
internal fun JsonPrimitive.numberOrNull(): Double? =
    doubleOrNull ?: longOrNull?.toDouble()

internal fun JsonObject.int(key: String, fallback: Int = 0): Int =
    (this[key] as? JsonPrimitive)?.numberOrNull()?.toInt() ?: fallback

internal fun JsonObject.long(key: String, fallback: Long = 0L): Long =
    (this[key] as? JsonPrimitive)?.numberOrNull()?.toLong() ?: fallback

internal fun JsonObject.bool(key: String, fallback: Boolean = false): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull ?: fallback

internal fun JsonArray.objects(): List<JsonObject> = mapNotNull { it as? JsonObject }
