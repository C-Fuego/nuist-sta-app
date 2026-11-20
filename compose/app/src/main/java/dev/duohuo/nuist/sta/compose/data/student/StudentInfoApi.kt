// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.student

import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.data.bridge.obj
import dev.duohuo.nuist.sta.compose.data.bridge.str
import kotlinx.serialization.Serializable

/** 信息门户返回的身份字段（只取展示需要的四项）。 */
@Serializable
data class StudentInfo(
    val name: String,
    val studentId: String,
    val categoryName: String,
    val deptName: String,
) {
    /** 「长望学院 · 25国科大计科1班」：去掉与类别重复的前缀后按层级拼接。 */
    val orgLine: String
        get() {
            var rest = deptName
            if (categoryName.isNotEmpty() && rest.startsWith(categoryName)) {
                rest = rest.substring(categoryName.length)
            }
            return rest.split('/')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(" · ")
        }
}

/** 信息门户身份（Dart 侧 `student.fetch`）。 */
class StudentInfoApi(private val bridge: CampusBridge) {

    suspend fun fetch(): StudentInfo {
        val data = bridge.call("student.fetch")
        // Dart 侧返回的是 {user: {...}, semester: {...}, fetchedAt: ...}，身份字段在 user 里。
        val user = data.obj("user") ?: kotlinx.serialization.json.JsonObject(emptyMap())
        return StudentInfo(
            name = user.str("userName"),
            studentId = user.str("userAccount"),
            categoryName = user.str("categoryName"),
            deptName = user.str("deptName"),
        )
    }
}
