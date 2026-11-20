// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.data.bulletin

import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.Bulletin
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinCategories
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinCategory
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinDetail
import dev.duohuo.nuist.sta.compose.ui.page.nuist.bulletin.BulletinPage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 公告数据的应用内缓存层。
 *
 * 首页卡片与列表页共用同一份“全部公告第一页”，详情按 URL 做 LRU 缓存，
 * 避免来回翻页时重复请求站点。
 */
class BulletinRepository(private val api: BulletinApi) {

    private val mutex = Mutex()
    /** 列表里见过的公告，详情页需要它来显示标题与发布信息。 */
    private val seen = LinkedHashMap<String, Bulletin>()
    private val detailCache = LinkedHashMap<String, BulletinDetail>()
    private var previewItems: List<Bulletin> = emptyList()
    private var previewTotal: Int = 0

    suspend fun firstPage(category: BulletinCategory): BulletinPage {
        val page = api.firstPage(category)
        remember(page.items)
        if (category.path.isEmpty()) {
            mutex.withLock {
                previewItems = page.items.take(PREVIEW_LIMIT)
                previewTotal = page.totalCount
            }
        }
        return page
    }

    suspend fun nextPage(page: BulletinPage): BulletinPage? =
        api.nextPage(page)?.also { remember(it.items) }

    suspend fun detail(url: String, force: Boolean = false): BulletinDetail {
        if (!force) mutex.withLock { detailCache[url] }?.let { return it }
        val bulletin = mutex.withLock { seen[url] } ?: Bulletin(
            id = url,
            title = "",
            officialCategory = "",
            publisher = "",
            publishedAt = "",
            topics = emptyList(),
        )
        val detail = api.detail(bulletin)
        mutex.withLock {
            detailCache[url] = detail
            while (detailCache.size > DETAIL_CACHE_LIMIT) {
                detailCache.remove(detailCache.keys.first())
            }
        }
        return detail
    }

    /** 首页“最新公告”：优先用列表页已经拉到的数据。 */
    suspend fun preview(): List<Bulletin> {
        mutex.withLock { previewItems }.takeIf { it.isNotEmpty() }?.let { return it }
        return firstPage(BulletinCategories.first()).items.take(PREVIEW_LIMIT)
    }

    /** 公告总条数，用于首页“查看全部”的说明文案；未加载过时为 0。 */
    suspend fun previewTotalCount(): Int = mutex.withLock { previewTotal }

    private suspend fun remember(items: List<Bulletin>) {
        mutex.withLock {
            items.forEach { seen[it.url] = it }
            while (seen.size > DETAIL_CACHE_LIMIT) {
                seen.remove(seen.keys.first())
            }
        }
    }

    private companion object {
        const val PREVIEW_LIMIT = 3
        const val DETAIL_CACHE_LIMIT = 30
    }
}