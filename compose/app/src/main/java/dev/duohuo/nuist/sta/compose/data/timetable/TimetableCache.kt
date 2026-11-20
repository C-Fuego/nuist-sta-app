// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.timetable

import android.content.Context
import dev.duohuo.nuist.sta.compose.ui.page.nuist.timetable.TimetableSnapshot
import kotlinx.serialization.json.Json

/**
 * 课表快照的本地缓存。
 *
 * 对齐参考实现：本机已有课表就先显示，不在进页面时无条件重新抓取；
 * 只有本机没有、或者用户下拉刷新（force）时才请求教务处。
 */
class TimetableCache(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): TimetableSnapshot? = runCatching {
        prefs.getString(KEY_SNAPSHOT, null)?.let {
            json.decodeFromString<TimetableSnapshot>(it)
        }
    }.getOrNull()

    fun save(snapshot: TimetableSnapshot) {
        runCatching {
            prefs.edit().putString(KEY_SNAPSHOT, json.encodeToString(snapshot)).apply()
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_SNAPSHOT).apply()
    }

    private companion object {
        const val PREF_NAME = "timetable_cache"
        const val KEY_SNAPSHOT = "snapshot.v1"
    }
}
