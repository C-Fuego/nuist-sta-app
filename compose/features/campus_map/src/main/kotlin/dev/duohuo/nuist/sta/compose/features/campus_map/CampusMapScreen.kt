package dev.duohuo.nuist.sta.compose.features.campus_map

import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.core.theme.ui.SymbolIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseItemContainer
import dev.duohuo.nuist.sta.compose.core.theme.settings.BaseWidget
import dev.duohuo.nuist.sta.compose.core.theme.settings.MiniAppBackButton
import dev.duohuo.nuist.sta.compose.core.theme.settings.SegmentedColumn


/**
 * 校园地图页（占位壳，MapLibre 待接入）。
 *
 * 说明（初开发阶段）：真实地图依赖 MapLibre GL 原生 SDK + 服务器下发的 style/tile_url，
 * 以及定位、朝向、小公交实时图层等。这里先立入口与工具条，标明后续接入范围，
 * 不在此阶段引入 native 地图依赖，避免过早踩崩溃/性能坑。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CampusMapScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("校园地图", modifier = Modifier.padding(start = 12.dp)) },
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
        Column(Modifier.fillMaxSize().padding(inner)) {
            SegmentedColumn {
                // 搜索行（真实搜索待地图接入后启用）
                item {
                    BaseWidget(
                        icon = "search",
                        title = "搜索地点、楼宇",
                        description = "教学楼、宿舍、服务点",
                    )
                }
                // 地图占位区（MapLibre 待接入），工具条悬浮其上
                item {
                    BaseItemContainer {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(320.dp).padding(24.dp),
                        ) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.large)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SymbolIcon("place",
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp),
                                        fontSize = 32.sp,
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "地图模块接入中",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "基于 MapLibre GL 的校园底图将在后续迭代接入：\n地点搜索、楼宇详情、从课表直达教室、小公交实时位置。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            // M3E 悬浮工具条（接入地图后原位保留）
                            HorizontalFloatingToolbar(
                                expanded = true,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            ) {
                                IconButton(onClick = {}) {
                                    SymbolIcon("layers", contentDescription = "图层")
                                }
                                IconButton(onClick = {}) {
                                    SymbolIcon("my_location", contentDescription = "定位")
                                }
                                IconButton(onClick = {}) {
                                    SymbolIcon("directions_bus", contentDescription = "小公交")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

