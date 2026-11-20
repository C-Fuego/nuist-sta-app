// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors

package dev.duohuo.nuist.sta.compose.data.electricity

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 本机保存的宿舍绑定（只存显示名与服务端 value）。 */
@Serializable
private data class StoredBinding(
    val campusName: String,
    val campusValue: String,
    val buildingName: String,
    val buildingValue: String,
    val roomName: String,
    val roomValue: String,
)

/** 宿舍绑定的本地持久化；解不出来就当未绑定。 */
class ElectricityStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("electricity_settings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun read(): ElecBinding? {
        val raw = prefs.getString(KEY_BINDING, null) ?: return null
        val stored = runCatching { json.decodeFromString<StoredBinding>(raw) }.getOrNull() ?: return null
        return ElecBinding(
            campus = ElecOption(stored.campusName, stored.campusValue),
            building = ElecOption(stored.buildingName, stored.buildingValue),
            room = ElecOption(stored.roomName, stored.roomValue),
        )
    }

    fun save(binding: ElecBinding) {
        val stored = StoredBinding(
            campusName = binding.campus.name,
            campusValue = binding.campus.value,
            buildingName = binding.building.name,
            buildingValue = binding.building.value,
            roomName = binding.room.name,
            roomValue = binding.room.value,
        )
        prefs.edit().putString(KEY_BINDING, json.encodeToString(StoredBinding.serializer(), stored)).apply()
    }

    private companion object {
        const val KEY_BINDING = "binding.v1"
    }
}