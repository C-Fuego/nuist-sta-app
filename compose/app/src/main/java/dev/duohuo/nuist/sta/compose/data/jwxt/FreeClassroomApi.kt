// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 原作作者与贡献者
package dev.duohuo.nuist.sta.compose.data.jwxt

import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.ui.page.nuist.free_classroom.Building
import dev.duohuo.nuist.sta.compose.ui.page.nuist.free_classroom.Classroom
import dev.duohuo.nuist.sta.compose.ui.page.nuist.free_classroom.ClassroomDay
import dev.duohuo.nuist.sta.compose.ui.page.nuist.free_classroom.PeriodGroup
import dev.duohuo.nuist.sta.compose.ui.page.nuist.free_classroom.PeriodState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 空教室查询，抓取由 `dart_bridge` 里的 `free_classroom_api.dart` 完成。
 *
 * 这里只把教学楼、日期、教室占用矩阵搬进 UI 模型；EMAP 会话、节流与解析都在 Dart 侧。
 */
class FreeClassroomApi(private val bridge: CampusBridge) {

    private var buildingsCache: List<Building>? = null

    /** 教学楼列表。 */
    suspend fun buildings(): List<Building> = buildingsCache ?: bridge
        .call("classroom.buildings")
        .let { it["buildings"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList()) }
        .map { element ->
            val item = element.jsonObject
            Building(
                code = item["code"]?.jsonPrimitive?.content.orEmpty(),
                name = item["name"]?.jsonPrimitive?.content.orEmpty(),
            )
        }
        .also { buildingsCache = it }

    /** 某教学楼某天的空教室情况。 */
    suspend fun day(building: Building, date: LocalDate): ClassroomDay {
        val data = bridge.call(
            "classroom.day",
            mapOf("code" to building.code, "date" to date.toString()),
        )
        val calendar = data["calendar"]?.jsonObject
        val groups = calendar?.get("groups")?.jsonArray.orEmpty().map { element ->
            val item = element.jsonObject
            PeriodGroup(
                start = item["start"]?.jsonPrimitive?.int ?: 0,
                end = item["end"]?.jsonPrimitive?.int ?: 0,
                startTime = item["startTime"]?.jsonPrimitive?.content.orEmpty(),
                endTime = item["endTime"]?.jsonPrimitive?.content.orEmpty(),
            )
        }
        val rooms = data["rooms"]?.jsonArray.orEmpty().map { element ->
            val item = element.jsonObject
            val occupancy = item["occupancy"]?.jsonArray.orEmpty().map { period ->
                period.jsonArray.map { cell -> cell.jsonPrimitive.content }
            }
            Classroom(
                name = item["name"]?.jsonPrimitive?.content.orEmpty(),
                floor = item["floor"]?.jsonPrimitive?.int ?: 0,
                zone = item["zone"]?.jsonPrimitive?.content.orEmpty(),
                seats = item["seats"]?.jsonPrimitive?.int ?: 0,
                periods = groups.indices.map { index ->
                    val names = occupancy.getOrNull(index).orEmpty()
                    PeriodState(
                        occupied = names.isNotEmpty(),
                        typeName = names.joinToString(" ").ifBlank { null },
                    )
                },
            )
        }
        val weekday = calendar?.get("weekday")?.jsonPrimitive?.int ?: date.dayOfWeek.value
        return ClassroomDay(
            dateLabel = date.format(DateTimeFormatter.ofPattern("M月d日")),
            weekdayName = WEEKDAYS.getOrElse(weekday - 1) { "" },
            week = calendar?.get("week")?.jsonPrimitive?.int ?: 1,
            groups = groups,
            rooms = rooms,
        )
    }

    private companion object {
        val WEEKDAYS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    }
}
