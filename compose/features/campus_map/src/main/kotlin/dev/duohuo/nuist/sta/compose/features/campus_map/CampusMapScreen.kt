package dev.duohuo.nuist.sta.compose.features.campus_map

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("校园地图") },
                subtitle = { Text("搜索地点、楼宇详情、小公交实时位置") },
                navigationIcon = { MiniAppBackButton() },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            SegmentedColumn {
                // 搜索行（真实搜索待地图接入后启用）
                item {
                    BaseWidget(
                        icon = Icons.Filled.Search,
                        title = "搜索地点、楼宇",
                        description = "教学楼、宿舍、服务点",
                    )
                }
                // 地图占位区（MapLibre 待接入）
                item {
                    BaseItemContainer {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(320.dp).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.large)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp),
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
                    }
                }
                // 工具条（接入地图后变为悬浮工具钮）
                item {
                    BaseItemContainer {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ToolChip("图层", Icons.Filled.Layers)
                            ToolChip("定位", Icons.Filled.MyLocation)
                            ToolChip("小公交", Icons.Filled.DirectionsBus)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(scheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
    }
}
