package com.example.mokumetrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun StatsScreen(records: List<SmokeRecord>) {
    val totalCount = records.size
    val uniqueDays = records.map {
        java.time.Instant.ofEpochMilli(it.timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
    }.distinct().size
    val dailyAverage = if (uniqueDays > 0) totalCount.toDouble() / uniqueDays else 0.0

    val dayStats = SmokeAnalytics.getDayOfWeekStats(records)
    val maxDayCount = max(dayStats.maxOfOrNull { it.count } ?: 0, 1)

    val heatmapData = SmokeAnalytics.getHourlyHeatmapStats(records)
    val days = listOf("日", "月", "火", "水", "木", "金", "土")

    // 最多時間帯の計算
    val hourCounts = IntArray(24)
    records.forEach {
        val hr = java.time.Instant.ofEpochMilli(it.timestamp)
            .atZone(java.time.ZoneId.systemDefault()).hour
        hourCounts[hr]++
    }
    val maxHourVal = hourCounts.maxOrNull() ?: 0
    val peakHour = if (maxHourVal > 0) hourCounts.indexOf(maxHourVal) else null

    val customColors = LocalAppThemeColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 概要メトリクス
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, customColors.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$totalCount",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "累計喫煙本数",
                        style = MaterialTheme.typography.bodySmall,
                        color = customColors.textSecondary
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, customColors.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format("%.1f", dailyAverage),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1日平均本数",
                        style = MaterialTheme.typography.bodySmall,
                        color = customColors.textSecondary
                    )
                }
            }
        }

        // 2. 曜日別棒グラフ
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, customColors.cardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "曜日別喫煙トレンド",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "曜日ごとの合計本数",
                    style = MaterialTheme.typography.bodySmall,
                    color = customColors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dayStats.forEach { day ->
                        val ratio = day.count.toFloat() / maxDayCount.toFloat()
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (day.count > 0) {
                                Text(
                                    text = "${day.count}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxHeight(ratio)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = day.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = customColors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. 曜日×時間帯ヒートマップ
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, customColors.cardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "曜日×時間帯ヒートマップ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "どの曜日・時間帯に多く吸っているか (GitHub風)",
                    style = MaterialTheme.typography.bodySmall,
                    color = customColors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // スクロール可能なグリッド
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.width(420.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 時間ヘッダー (0, 4, 8, 12, 16, 20)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp)
                        ) {
                            for (hr in 0..23) {
                                Text(
                                    text = if (hr % 4 == 0) "$hr" else "",
                                    fontSize = 9.sp,
                                    color = customColors.textSecondary,
                                    modifier = Modifier.width(15.dp),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                        }

                        // 各曜日の行
                        days.forEachIndexed { dayIdx, dayName ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 曜日ラベル
                                Text(
                                    text = dayName,
                                    fontSize = 10.sp,
                                    color = customColors.textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(24.dp)
                                )

                                // 24時間のセル
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    for (hr in 0..23) {
                                        val count = heatmapData[dayIdx][hr]
                                        val level = SmokeAnalytics.getHeatmapLevel(count)
                                        
                                        // レベルに応じた背景色
                                        val cellColor = when (level) {
                                            0 -> Color(0xFFFFFFFF).copy(alpha = 0.05f)
                                            1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                            else -> MaterialTheme.colorScheme.primary
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(15.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(cellColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 凡例
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "少", fontSize = 10.sp, color = customColors.textSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    listOf(0.05f, 0.15f, 0.35f, 0.65f, 1.0f).forEach { alpha ->
                        val color = if (alpha == 0.05f) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color.copy(alpha = alpha))
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "多", fontSize = 10.sp, color = customColors.textSecondary)
                }
            }
        }

        // 4. 分析インサイト
        if (peakHour != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Yellow.copy(alpha = 0.04f)
                ),
                border = BorderStroke(1.dp, customColors.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Analysis",
                        tint = customColors.warningColor
                    )
                    Column {
                        Text(
                            text = "パターン分析",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "あなたの喫煙ピーク時間帯は ${peakHour}時台 です。この時間帯に行動パターンを変える（例：散歩する、お茶を飲む）ことで、喫煙本数の削減が期待できます。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(60.dp))
    }
}
