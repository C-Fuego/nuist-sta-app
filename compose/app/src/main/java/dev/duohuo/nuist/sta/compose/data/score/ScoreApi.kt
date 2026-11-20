// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.score

import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.ui.page.nuist.score.ScoreRecord
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 教务「学生成绩」。抓取与解析都在 Dart 核心（`score.fetch`），这里只做字段映射。
 */
class ScoreApi(private val bridge: CampusBridge) {

    suspend fun fetch(): List<ScoreRecord> {
        val data = bridge.call("score.fetch")
        val records = data["records"] as? JsonArray ?: return emptyList()
        return records.mapNotNull { element ->
            val row = element as? JsonObject ?: return@mapNotNull null
            fun s(key: String) = row[key]?.jsonPrimitive?.content?.trim().orEmpty()
            ScoreRecord(
                courseName = s("courseName"),
                courseCode = s("courseCode"),
                score = s("score"),
                credit = s("credit"),
                gradePoint = s("gradePoint"),
                passStatus = s("passStatus"),
                examType = s("examType"),
                usualScore = s("usualScore"),
                finalScore = s("finalScore"),
                retakeType = s("retakeType"),
                term = s("term"),
            )
        }
    }
}
