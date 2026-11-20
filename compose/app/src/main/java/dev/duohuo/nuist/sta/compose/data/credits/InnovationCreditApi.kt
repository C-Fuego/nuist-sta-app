// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.credits

import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.data.bridge.arr
import dev.duohuo.nuist.sta.compose.data.bridge.objects
import dev.duohuo.nuist.sta.compose.data.bridge.str

/**
 * 双创学分：汇总页总分/成绩 + 明细页全部条目。
 *
 * 抓取链路（登录跳转、Referer、防伪 token）在 Dart 侧完成，这一层只搬字段。
 */
class InnovationCreditApi(private val bridge: CampusBridge) {

    suspend fun fetch(): InnovationCreditData {
        val data = bridge.call("innovation.fetch")
        val items = data.arr("items").objects().map { row ->
            CreditItem(
                name = row.str("name"),
                date = row.str("date"),
                category1 = row.str("category1"),
                category2 = row.str("category2"),
                level = row.str("level"),
                standardScore = row.str("standardScore"),
                score = row.str("score"),
                batch = row.str("batch"),
                status = row.str("status"),
            )
        }
        return InnovationCreditData(
            total = data.str("total"),
            grade = data.str("grade"),
            items = items,
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }
}
