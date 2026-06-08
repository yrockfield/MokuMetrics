package com.example.mokumetrics

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max

object SmokeAnalytics {

    /**
     * 今日（ローカル日付）の喫煙本数を取得
     */
    fun getTodayCount(records: List<SmokeRecord>, nowMs: Long = System.currentTimeMillis()): Int {
        val zone = ZoneId.systemDefault()
        val todayDate = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        
        return records.count { record ->
            val recordDate = Instant.ofEpochMilli(record.timestamp).atZone(zone).toLocalDate()
            recordDate == todayDate
        }
    }

    /**
     * 前回の喫煙から現在までの経過時間を秒数で取得
     */
    fun getSecondsSinceLastSmoke(records: List<SmokeRecord>, nowMs: Long = System.currentTimeMillis()): Long? {
        if (records.isEmpty()) return null
        val lastTimestamp = records.maxOf { it.timestamp }
        if (lastTimestamp > nowMs) return 0L
        return (nowMs - lastTimestamp) / 1000L
    }

    /**
     * 経過秒数を "HH:MM:SS" または "DD日 HH:MM:SS" にフォーマット
     */
    fun formatDuration(seconds: Long?): String {
        if (seconds == null) return "--:--:--"
        val d = seconds / (3600 * 24)
        val h = (seconds % (3600 * 24)) / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60

        val formattedTime = String.format("%02d:%02d:%02d", h, m, s)
        return if (d > 0) {
            "${d}日 $formattedTime"
        } else {
            formattedTime
        }
    }

    /**
     * 曜日別の喫煙本数を集計
     * 日曜(1)から土曜(7)または月(1)から日(7)。ここではWebと揃えるため、日〜土の並びにする。
     * 日曜(index 0)〜土曜(index 6)
     */
    fun getDayOfWeekStats(records: List<SmokeRecord>): List<DayStat> {
        val dayNames = listOf("日", "月", "火", "水", "木", "金", "土")
        val counts = IntArray(7)
        val zone = ZoneId.systemDefault()

        records.forEach { record ->
            // java.time の DayOfWeek は月曜日が1, 日曜日が7
            val zdt = Instant.ofEpochMilli(record.timestamp).atZone(zone)
            val dayOfWeekValue = zdt.dayOfWeek.value // 1:月, ..., 7:日
            val index = if (dayOfWeekValue == 7) 0 else dayOfWeekValue // 7->0 (日), 1->1 (月) ...
            counts[index]++
        }

        return dayNames.mapIndexed { idx, name ->
            DayStat(label = name, count = counts[idx])
        }
    }

    data class DayStat(val label: String, val count: Int)

    /**
     * 曜日 (7) × 時間帯 (24時間) のヒートマップデータを集計
     * 戻り値: 7行24列の2次元リスト
     */
    fun getHourlyHeatmapStats(records: List<SmokeRecord>): List<List<Int>> {
        val matrix = MutableList(7) { MutableList(24) { 0 } }
        val zone = ZoneId.systemDefault()

        records.forEach { record ->
            val zdt = Instant.ofEpochMilli(record.timestamp).atZone(zone)
            val dayOfWeekValue = zdt.dayOfWeek.value // 1:月, ..., 7:日
            val dayIdx = if (dayOfWeekValue == 7) 0 else dayOfWeekValue
            val hour = zdt.hour // 0-23
            matrix[dayIdx][hour]++
        }

        return matrix
    }

    /**
     * ヒートマップの件数を 0〜4 のレベルに変換
     */
    fun getHeatmapLevel(count: Int): Int {
        return when {
            count == 0 -> 0
            count <= 1 -> 1
            count <= 3 -> 2
            count <= 5 -> 3
            else -> 4
        }
    }

    /**
     * スマートインサイトの生成
     */
    fun generateSmartInsight(records: List<SmokeRecord>, nowMs: Long = System.currentTimeMillis()): String {
        if (records.isEmpty()) {
            return "まずは記録を始めましょう！「また吸っちまった」その瞬間を恐れずタップしてください。"
        }

        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val yesterday = today.minusDays(1)

        var todayCount = 0
        var yesterdayCount = 0

        records.forEach { record ->
            val recordDate = Instant.ofEpochMilli(record.timestamp).atZone(zone).toLocalDate()
            if (recordDate == today) todayCount++
            else if (recordDate == yesterday) yesterdayCount++
        }

        // 1. 今日と昨日の本数比較
        if (todayCount > 0 && yesterdayCount > 0) {
            if (todayCount < yesterdayCount) {
                return "昨日の ${yesterdayCount} 本に比べ、今日は ${todayCount} 本と抑えられています！素晴らしい調子です。この調子でいきましょう！"
            } else if (todayCount > yesterdayCount) {
                return "昨日の ${yesterdayCount} 本を超えて、今日は既に ${todayCount} 本吸っています。少し深呼吸して、次の1本を5分だけ遅らせてみませんか？"
            }
        }

        // 2. 最多時間帯のインサイト
        val hourCounts = IntArray(24)
        records.forEach { record ->
            val hr = Instant.ofEpochMilli(record.timestamp).atZone(zone).hour
            hourCounts[hr]++
        }

        var maxHour = 0
        var maxCount = 0
        hourCounts.forEachIndexed { hr, count ->
            if (count > maxCount) {
                maxCount = count
                maxHour = hr
            }
        }

        if (maxCount >= 3) {
            val timeframe = when (maxHour) {
                in 5..9 -> "朝（5時〜10時）"
                in 10..15 -> "昼（10時〜16時）"
                in 16..20 -> "夕方・夜（16時〜21時）"
                else -> "深夜（21時〜5時）"
            }
            return "データによると、あなたは【${timeframe}】の喫煙が最も多い傾向にあります。この時間帯は「口寂しさ」への代替手段（ガムやミントなど）を用意しておくと効果的です。"
        }

        // 3. 直近の間隔のトレンド
        if (records.size >= 3) {
            val sorted = records.sortedByDescending { it.timestamp }
            val diff1 = sorted[0].timestamp - sorted[1].timestamp
            val diff2 = sorted[1].timestamp - sorted[2].timestamp

            if (diff1 < diff2 && diff1 < 30 * 60 * 1000) {
                return "直近の喫煙ペースがやや早くなっています。「チェーンスモーク」になりそうな時は、冷たい水を一杯飲んでリフレッシュしてみてください。"
            }
        }

        return "記録が順調に蓄積されています！ダッシュボードから自分の「喫煙パターン」を把握して、自然なコントロールを目指しましょう。"
    }
}
