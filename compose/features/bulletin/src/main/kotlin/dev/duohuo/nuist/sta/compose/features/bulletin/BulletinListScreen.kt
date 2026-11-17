package dev.duohuo.nuist.sta.compose.features.bulletin

import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseWidget
import dev.duohuo.nuist.sta.compose.core.theme.settings.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn
import dev.duohuo.nuist.sta.compose.core.theme.settings.SingleChoiceDialog

/**
 * 信息公告列表页（Compose + MD3E）。对应 Flutter `BulletinPage`。
 *
 * 主题筛选照搬参考实现的 `SegmentedColumn` + `BaseWidget` 分段卡片形态，
 * 点击弹 M3 单选窗；搜索框是可用状态，按标题/发布单位/主题实时过滤。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BulletinListScreen(
    items: List<Bulletin> = BulletinMock.items,
    onOpen: (Bulletin) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var topic by rememberSaveable { mutableStateOf("全部") }
    var query by rememberSaveable { mutableStateOf("") }
    var showTopicDialog by rememberSaveable { mutableStateOf(false) }

    val visible = items.filter { b ->
        val matchTopic = topic == "全部" || b.topics.contains(topic)
        val matchQuery = query.isBlank() ||
            b.title.contains(query, ignoreCase = true) ||
            b.publisher.contains(query, ignoreCase = true) ||
            b.officialCategory.contains(query, ignoreCase = true) ||
            b.topics.any { it.contains(query, ignoreCase = true) }
        matchTopic && matchQuery
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("信息公告", modifier = Modifier.padding(start = 12.dp)) },
                navigationIcon = { MiniAppBackButton() },
                                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SearchField(query = query, onQueryChange = { query = it })
            }
            item {
                SegmentedColumn(title = "主题") {
                    item {
                        BaseWidget(
                            icon = "topic",
                            title = topic,
                            description = "按主题筛选公告",
                            trailingContent = {
                                SymbolIcon("chevron_right",
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onClick = { showTopicDialog = true },
                        )
                    }
                }
            }
            // 公告列表：与首页/设置页同构的行式分组（不再用 ElevatedCard）
            item {
                SegmentedColumn(title = "公告列表 · ${visible.size} 条") {
                    visible.forEach { b ->
                        item {
                            BaseWidget(
                                icon = if (b.actions.contains("置顶")) "push_pin" else "campaign",
                                title = b.title,
                                titleStyle = MaterialTheme.typography.titleMedium,
                                description = buildString {
                                    b.actions.filter { it != "置顶" }.forEach { append(it).append(" · ") }
                                    if (b.officialCategory.isNotEmpty()) append(b.officialCategory).append(" · ")
                                    b.topics.take(1).forEach { append(it).append(" · ") }
                                    append(b.publisher.ifEmpty { "学校信息公告栏" })
                                    append(" · ")
                                    append(b.publishedAt)
                                },
                                onClick = { onOpen(b) },
                                trailingContent = {
                                    SymbolIcon(
                                        "chevron_right",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                        fontSize = 20.sp,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        placeholder = {
            Text(
                "搜索公告标题 / 单位 / 主题",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = { SymbolIcon("search", contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    SymbolIcon("close", contentDescription = "清除")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun Tag(text: String, highlighted: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val color = if (highlighted) scheme.primary else scheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (highlighted) scheme.primaryContainer else scheme.surfaceContainerHighest)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
