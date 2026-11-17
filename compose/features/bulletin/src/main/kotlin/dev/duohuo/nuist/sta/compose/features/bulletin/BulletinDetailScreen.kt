package dev.duohuo.nuist.sta.compose.features.bulletin

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duohuo.nuist.sta.compose.core.theme.settings.ExpressiveBackButton

/** 公告详情页（Compose + MD3E）。对应 Flutter `BulletinDetailPage`（纯文本块，附件后置）。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BulletinDetailScreen(detail: BulletinDetail, onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("公告详情", modifier = Modifier.padding(start = 12.dp)) },
                navigationIcon = {
                    Row {
                        ExpressiveBackButton { onBack() }
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                },
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    detail.bulletin.title,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = scheme.onSurface,
                    lineHeight = 34.sp,
                )
            }
            item {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        detail.bulletin.publisher,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                    )
                    Text(
                        "  ·  ",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        detail.bulletin.publishedAt,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            item {
                androidx.compose.material3.HorizontalDivider(
                    color = scheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
            items(detail.blocks.size) { i ->
                when (val block = detail.blocks[i]) {
                    is DetailBlock.Heading -> Text(
                        block.text,
                        style = if (block.level <= 2) MaterialTheme.typography.titleMediumEmphasized
                        else MaterialTheme.typography.titleSmallEmphasized,
                        color = scheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    is DetailBlock.Paragraph -> Text(
                        block.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface,
                        lineHeight = 28.sp,
                    )
                    is DetailBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        block.items.forEach { item ->
                            Row {
                                Text("•  ", style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.primary)
                                Text(item, style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurface, lineHeight = 24.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
