// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.academic

import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.data.bridge.str

/** 学业概览（信息门户 `czjl`）。抓取与解析在 Dart 侧，这里只做字段搬运。 */
data class AcademicSnapshot(
    val studentId: String,
    val requiredCredits: String,
    val earnedCredits: String,
    val averageGradePoint: String,
    val gpa: String,
    val averageScore: String,
    val weightedAverageScore: String,
    val classRank: String,
    val majorRank: String,
    val passRate: String,
)

class AcademicApi(private val bridge: CampusBridge) {

    suspend fun fetch(): AcademicSnapshot {
        val data = bridge.call("academic.fetch")
        return AcademicSnapshot(
            studentId = data.str("XH"),
            requiredCredits = data.str("YXZXF"),
            earnedCredits = data.str("YHXF"),
            averageGradePoint = data.str("PJJD"),
            gpa = data.str("GPA"),
            averageScore = data.str("PJF"),
            weightedAverageScore = data.str("JQPJF"),
            classRank = data.str("BJPM"),
            majorRank = data.str("ZYPM"),
            passRate = data.str("KCTGL"),
        )
    }
}
