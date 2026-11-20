// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.credits

import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.data.bridge.obj
import dev.duohuo.nuist.sta.compose.data.bridge.str

/**
 * 劳动积分：官方核算结果 + 三个分项页面的实时统计。
 *
 * 四个页面要按「先建会话再并发」的顺序取，这部分已由 Dart 侧处理。
 */
class LaborScoreApi(private val bridge: CampusBridge) {

    suspend fun fetch(): LaborScoreData {
        val data = bridge.call("labor.fetch")
        val official = data.obj("official")?.let { row ->
            LaborResultData(
                theory = row.str("theory"),
                life = row.str("life"),
                service = row.str("service"),
                majorCourse = row.str("majorCourse"),
                majorContest = row.str("majorContest"),
                majorTotal = row.str("majorTotal"),
                total = row.str("total"),
                confirmed = row.str("confirmed"),
                updatedAt = row.str("updatedAt"),
                filed = row.str("filed"),
                filedAt = row.str("filedAt"),
            )
        }
        val liveMajor = data.obj("liveMajor")?.let { row ->
            LaborMajorLive(
                course = row.str("course"),
                contest = row.str("contest"),
                total = row.str("total"),
            )
        }
        return LaborScoreData(
            official = official,
            liveLife = data.str("liveLife").ifEmpty { null },
            liveService = data.str("liveService").ifEmpty { null },
            liveMajor = liveMajor,
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }
}
