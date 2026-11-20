// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.bulletin

import dev.duohuo.nuist.sta.compose.data.bridge.CampusBridge
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.Bulletin
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinAttachment
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinCategory
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinDetail
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinPage
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.DetailBlock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import dev.duohuo.nuist.sta.compose.data.bridge.numberOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

/**
 * 学校信息公告栏。抓取、正文切块、附件识别都在 Dart 核心（`bulletin.*`），
 * 本类只把 JSON 映射成界面模型。
 */
class BulletinApi(private val bridge: CampusBridge) {

    /** 栏目被校园网 / 统一身份认证拦住时抛出，调用方据此给出“连校园网或登录”的提示。 */
    class BulletinAccessException(message: String) : IOException(message)

    /** 拉栏目的第一页。 */
    suspend fun firstPage(category: BulletinCategory): BulletinPage =
        load(category.path, pageNumber = 1)

    /** 拉列表的下一页；没有下一页时返回 null。 */
    suspend fun nextPage(page: BulletinPage): BulletinPage? {
        val path = page.nextPath ?: return null
        return load(path, pageNumber = page.pageNumber + 1)
    }

    /** 拉详情页。 */
    suspend fun detail(bulletin: Bulletin): BulletinDetail {
        val data = bridge.call(
            "bulletin.detail",
            mapOf(
                "url" to bulletin.url,
                "id" to bulletin.id,
                "title" to bulletin.title,
                "category" to bulletin.officialCategory,
                "publisher" to bulletin.publisher,
                "publishedAt" to bulletin.publishedAt,
            ),
        )
        if (data["found"]?.jsonPrimitive?.booleanOrNull != true) {
            throw IOException("公告正文解析失败")
        }
        val blocks = (data["blocks"] as? JsonArray).orEmpty().mapNotNull { element ->
            val block = element as? JsonObject ?: return@mapNotNull null
            fun s(key: String) = block[key]?.jsonPrimitive?.content.orEmpty()
            val text = s("text")
            when (s("kind")) {
                "heading" -> DetailBlock.Heading(text, block["level"]?.jsonPrimitive?.numberOrNull()?.toInt() ?: 0)
                "bulletList", "numberedList" -> DetailBlock.Bullets(
                    (block["items"] as? JsonArray).orEmpty().map { it.jsonPrimitive.content },
                )
                "image" -> text.takeIf { it.isNotEmpty() }?.let { DetailBlock.Paragraph(it) }
                else -> text.takeIf { it.isNotEmpty() }?.let { DetailBlock.Paragraph(it) }
            }
        }
        val attachments = (data["attachments"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            BulletinAttachment(
                name = item["name"]?.jsonPrimitive?.content.orEmpty(),
                url = item["url"]?.jsonPrimitive?.content.orEmpty(),
            )
        }
        val meta = listOf(bulletin.publisher, bulletin.publishedAt)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        return BulletinDetail(
            bulletin = bulletin,
            meta = meta,
            blocks = blocks,
            attachments = attachments,
            sourceUrl = bulletin.url,
        )
    }

    private suspend fun load(path: String, pageNumber: Int): BulletinPage {
        val data = bridge.call("bulletin.list", mapOf("category" to path, "page" to pageNumber))
        val items = (data["items"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            fun s(key: String) = item[key]?.jsonPrimitive?.content.orEmpty()
            val url = s("url")
            Bulletin(
                id = s("id").ifEmpty { url },
                title = s("title"),
                officialCategory = s("officialCategory"),
                publisher = s("publisher"),
                publishedAt = s("publishedAt"),
                topics = (item["topics"] as? JsonArray).orEmpty().map { it.jsonPrimitive.content },
                url = url,
            )
        }
        val total = data["totalCount"]?.jsonPrimitive?.numberOrNull()?.toInt() ?: items.size
        val perPage = items.size.coerceAtLeast(1)
        return BulletinPage(
            items = items,
            pageNumber = data["pageNumber"]?.jsonPrimitive?.numberOrNull()?.toInt() ?: pageNumber,
            totalPages = ((total + perPage - 1) / perPage).coerceAtLeast(1),
            totalCount = total,
            nextPath = data["nextPath"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
        )
    }
}
