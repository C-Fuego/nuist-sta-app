// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors

package dev.duohuo.nuist.sta.compose.data.electricity

import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** 一卡通级联选择里的一项：区域 / 楼栋 / 房间都是这个形状。 */
data class ElecOption(val name: String, val value: String)

/** 用户绑定的宿舍：三个层级都保留服务端原始 value。 */
data class ElecBinding(val campus: ElecOption, val building: ElecOption, val room: ElecOption) {
    val key: String get() = "${campus.value}|${building.value}|${room.value}"

    /** 「沁园 · 晖园 · 315」。楼栋名往往已含区域名，重复时只留楼栋。 */
    val displayName: String
        get() = buildList {
            if (!building.name.contains(campus.name)) add(campus.name)
            add(building.name)
            add(room.name)
        }.joinToString(" · ")
}

/**
 * 一卡通电费接口，实际抓取由 `dart_bridge` 里的 `electricity_api.dart` 完成。
 *
 * 这里只负责把参数与结果搬进搬出，鉴权、会话续期、错误归类都在 Dart 那边。
 */
class ElectricityApi(private val bridge: CampusBridge) {

    suspend fun campuses(): List<ElecOption> = options("electricity.campuses", emptyMap())

    suspend fun buildings(campus: ElecOption): List<ElecOption> =
        options("electricity.buildings", mapOf("campus" to optionJson(campus)))

    suspend fun rooms(campus: ElecOption, building: ElecOption): List<ElecOption> = options(
        "electricity.rooms",
        mapOf("campus" to optionJson(campus), "building" to optionJson(building)),
    )

    /** 查一次当前剩余电量（度）。负数表示欠费。 */
    suspend fun balance(binding: ElecBinding): Double {
        val data = bridge.call(
            "electricity.balance",
            mapOf(
                "campus" to optionJson(binding.campus),
                "building" to optionJson(binding.building),
                "room" to optionJson(binding.room),
            ),
        )
        return data["kwh"]?.jsonPrimitive?.double ?: 0.0
    }

    private suspend fun options(method: String, args: Map<String, Any?>): List<ElecOption> =
        bridge.call(method, args)["options"]?.jsonArray.orEmpty().map { element ->
            val item = element.jsonObject
            ElecOption(
                name = item["name"]?.jsonPrimitive?.content.orEmpty(),
                value = item["value"]?.jsonPrimitive?.content.orEmpty(),
            )
        }

    private fun optionJson(option: ElecOption): JsonObject = buildJsonObject {
        put("name", option.name)
        put("value", option.value)
    }
}
