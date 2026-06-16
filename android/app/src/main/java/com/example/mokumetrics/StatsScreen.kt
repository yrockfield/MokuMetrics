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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

@Composable
fun StatsScreen(
    records: List<SmokeRecord>,
    llmPatternAnalysis: String? = null,
    lastUpdateTime: Long = 0L
) {
    val totalCount = records.size
    val uniqueDays = records.map {
        java.time.Instant.ofEpochMilli(it.timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
    }.distinct().size
    val dailyAverage = if (uniqueDays > 0) totalCount.toDouble() / uniqueDays else 0.0

    val dayStats = SmokeAnalytics.getDayOfWeekStats(records)
    val maxDayCount = maxOf(dayStats.maxOfOrNull { it.count } ?: 0.0, 1.0)

    val intervalStats = SmokeAnalytics.getSmokingIntervalStats(records)
    val heatmapData = SmokeAnalytics.getPeriodHeatmapStats(records)
    val days = listOf("日", "月", "火", "水", "木", "金", "土")
    val periods = listOf(
        "深夜 (0-8)",
        "朝 (8-12)",
        "昼 (12-18)",
        "夜 (18-24)"
    )

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

    val formattedUpdateTime = remember(lastUpdateTime) {
        if (lastUpdateTime == 0L) {
            ""
        } else {
            try {
                val zdt = java.time.Instant.ofEpochMilli(lastUpdateTime)
                    .atZone(java.time.ZoneId.systemDefault())
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
                zdt.format(formatter)
            } catch (e: Exception) {
                ""
            }
        }
    }

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
                    text = "曜日ごとの平均本数",
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
                        val ratio = (day.count / maxDayCount).toFloat()
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // 棒と数値のエリア (曜日ラベルを押し出さないように隔離)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    if (day.count > 0.0) {
                                        val displayVal = if (day.count % 1.0 == 0.0) "${day.count.toInt()}" else String.format("%.1f", day.count)
                                        Text(
                                            text = displayVal,
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
                                }
                            }
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

        // 3. 喫煙間隔のばらつき分布
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
                    text = "喫煙間隔のばらつき分布",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "同じ日の中での喫煙間隔（前回の喫煙から何分空いたか）の分布",
                    style = MaterialTheme.typography.bodySmall,
                    color = customColors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (intervalStats.total == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "間隔を算出するためのデータが不足しています（1日2回以上の喫煙記録が必要です）。",
                            color = customColors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val total = intervalStats.total.toFloat()
                    
                    // スタックバー
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        if (intervalStats.under30 > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(intervalStats.under30.toFloat() / total)
                                    .background(Color(0xFFEF4444))
                            )
                        }
                        if (intervalStats.between30And60 > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(intervalStats.between30And60.toFloat() / total)
                                    .background(Color(0xFFF97316))
                            )
                        }
                        if (intervalStats.between60And120 > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(intervalStats.between60And120.toFloat() / total)
                                    .background(Color(0xFF10B981))
                            )
                        }
                        if (intervalStats.over120 > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(intervalStats.over120.toFloat() / total)
                                    .background(Color(0xFF3B82F6))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 凡例・詳細 (2x2 グリッド的配置)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 30分未満
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFEF4444))
                                )
                                Column {
                                    Text(
                                        text = "30分未満",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${intervalStats.under30}件 (${String.format("%.1f", (intervalStats.under30 / total) * 100)}%)",
                                        fontSize = 11.sp,
                                        color = customColors.textSecondary
                                    )
                                }
                            }

                            // 30分〜60分
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFF97316))
                                )
                                Column {
                                    Text(
                                        text = "30分〜60分",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${intervalStats.between30And60}件 (${String.format("%.1f", (intervalStats.between30And60 / total) * 100)}%)",
                                        fontSize = 11.sp,
                                        color = customColors.textSecondary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 60分〜120分
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF10B981))
                                )
                                Column {
                                    Text(
                                        text = "60分〜120分",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${intervalStats.between60And120}件 (${String.format("%.1f", (intervalStats.between60And120 / total) * 100)}%)",
                                        fontSize = 11.sp,
                                        color = customColors.textSecondary
                                    )
                                }
                            }

                            // 120分以上
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF3B82F6))
                                )
                                Column {
                                    Text(
                                        text = "120分以上",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${intervalStats.over120}件 (${String.format("%.1f", (intervalStats.over120 / total) * 100)}%)",
                                        fontSize = 11.sp,
                                        color = customColors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3.5. 曜日×時間帯ヒートマップ (大区分)
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
                    text = "どの曜日・どの時間帯に多く吸っているかの分布 (時間帯大区分)",
                    style = MaterialTheme.typography.bodySmall,
                    color = customColors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ヘッダー
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    periods.forEach { periodLabel ->
                        Text(
                            text = periodLabel,
                            fontSize = 9.sp,
                            color = customColors.textSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 各曜日の行
                days.forEachIndexed { dayIdx, dayName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 曜日ラベル
                        Text(
                            text = dayName,
                            fontSize = 11.sp,
                            color = customColors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(16.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // 4つのセル
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (pIdx in 0..3) {
                                val count = heatmapData[dayIdx][pIdx]
                                val level = SmokeAnalytics.getPeriodHeatmapLevel(count)
                                val cellColor = when (level) {
                                    0 -> Color.White.copy(alpha = 0.05f)
                                    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                val textColor = if (level > 2) Color.White else MaterialTheme.colorScheme.onSurface

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(cellColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (count > 0) "${count}本" else "0",
                                        fontSize = 10.sp,
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.alpha(if (count > 0) 1f else 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 凡例
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
        if (llmPatternAnalysis != null || peakHour != null) {
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "パターン分析",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (llmPatternAnalysis != null && formattedUpdateTime.isNotEmpty()) {
                                Text(
                                    text = "更新: $formattedUpdateTime",
                                    fontSize = 10.sp,
                                    color = customColors.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = llmPatternAnalysis ?: "あなたの喫煙ピーク時間帯は ${peakHour}時台 です。この時間帯に行動パターンを変える（例：散歩する、お茶を飲む）ことで、喫煙本数の削減が期待できます。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
        
    }
}
