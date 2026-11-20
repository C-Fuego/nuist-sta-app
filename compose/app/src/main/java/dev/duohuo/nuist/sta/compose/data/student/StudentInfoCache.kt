// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.student

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * 学生信息（姓名 / 院系班级）的本地缓存。
 *
 * 信息门户会话过期时接口会拒绝，若不在本机留一份，界面上「姓名 / 院系班级」
 * 就会退回空值；缓存让这些字段在离线或掉登录时仍然显示上次成功读取的内容。
 */
class StudentInfoCache(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): StudentInfo? = runCatching {
        prefs.getString(KEY_INFO, null)?.let {
            json.decodeFromString<StudentInfo>(it)
        }
    }.getOrNull()

    fun save(info: StudentInfo) {
        runCatching {
            prefs.edit().putString(KEY_INFO, json.encodeToString(info)).apply()
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_INFO).apply()
    }

    private companion object {
        const val PREF_NAME = "student_info_cache"
        const val KEY_INFO = "info.v1"
    }
}
