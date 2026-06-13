package com.example.mokumetrics

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    data class IntervalStats(
        val under30: Int,
        val between30And60: Int,
        val between60And120: Int,
        val over120: Int,
        val total: Int
    )

    /**
     * 同じ日のレコード同士で喫煙間隔を計算し、ばらつき分布を集計する
     */
    fun getSmokingIntervalStats(records: List<SmokeRecord>): IntervalStats {
        var under30 = 0
        var between30And60 = 0
        var between60And120 = 0
        var over120 = 0
        var total = 0

        val zone = ZoneId.systemDefault()
        // 日付でグループ化
        val groups = records.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
        }

        groups.values.forEach { dayRecords ->
            if (dayRecords.size >= 2) {
                val sorted = dayRecords.sortedBy { it.timestamp }
                for (i in 0 until sorted.size - 1) {
                    val diffMs = sorted[i+1].timestamp - sorted[i].timestamp
                    val diffMins = diffMs / (60 * 1000)
                    when {
                        diffMins < 30 -> under30++
                        diffMins <= 60 -> between30And60++
                        diffMins <= 120 -> between60And120++
                        else -> over120++
                    }
                    total++
                }
            }
        }

        return IntervalStats(under30, between30And60, between60And120, over120, total)
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

    /**
     * 曜日 (7) × 時間帯大区分 (4: 深夜, 朝, 昼, 夜) のヒートマップデータを集計
     * 戻り値: 7行4列 of Int
     */
    fun getPeriodHeatmapStats(records: List<SmokeRecord>): List<List<Int>> {
        val matrix = MutableList(7) { MutableList(4) { 0 } }
        val zone = ZoneId.systemDefault()

        records.forEach { record ->
            val zdt = Instant.ofEpochMilli(record.timestamp).atZone(zone)
            val dayOfWeekValue = zdt.dayOfWeek.value // 1:月, ..., 7:日
            val dayIdx = if (dayOfWeekValue == 7) 0 else dayOfWeekValue
            val hour = zdt.hour // 0-23
            
            val periodIdx = when (hour) {
                in 8..11 -> 1 // 朝 (8-12)
                in 12..17 -> 2 // 昼 (12-18)
                in 18..23 -> 3 // 夜 (18-24)
                else -> 0 // 深夜・早朝 (0-8)
            }
            matrix[dayIdx][periodIdx]++
        }

        return matrix
    }

    /**
     * 期間ヒートマップの件数を 0〜4 のレベルに変換
     */
    fun getPeriodHeatmapLevel(count: Int): Int {
        return when {
            count == 0 -> 0
            count <= 2 -> 1
            count <= 5 -> 2
            count <= 9 -> 3
            else -> 4
        }
    }

    /**
     * 必要に応じてGemini APIを叩き、インサイト、パターン分析、一言を更新する
     */
    suspend fun updateLlmDataIfNeeded(context: Context, records: List<SmokeRecord>, apiKey: String, characterId: String = "uncle") {
        if (apiKey.isEmpty() || records.isEmpty()) return

        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences("mokumetrics_prefs", Context.MODE_PRIVATE)
        val lastUpdateTime = prefs.getLong("llm_last_insight_update_time", 0L)

        // 更新頻度: 4時間 (4 * 60 * 60 * 1000)
        val UPDATE_INTERVAL = 4 * 60 * 60 * 1000L
        if (now - lastUpdateTime < UPDATE_INTERVAL) {
            return
        }

        val characterPrompts = mapOf(
            "uncle" to "あなたは「フランクなおっちゃん」としてふるまってください。ぶっきらぼうだけど人情味のある焼き鳥屋のオヤジのような口調（関西弁混じりのフランクな口調）で、ユーザーを優しく受け流して励ましてください。分析やアドバイスもおっちゃんのキャラクターらしく、温かみがありつつぶっきらぼうな言い回しにしてください。",
            "tsundere" to "あなたは「ツンデレ秘書」としてふるまってください。クールで丁寧な敬語を使う優秀なアシスタントで、基本は冷徹にデータを分析し、ややトゲのある言い方をしますが、最後にはユーザーの体のことや健康を本気で心配している強い優しさ（デレ）をはっきりと見せてください。最初はツンツンしていても、後半や最後のアドバイス部分では、心配するあまり感情がどうしても漏れ出てしまうような、強めのデレ感を意識してください。",
            "gal" to "あなたは「明るくフランクな女性キャラ（マイルドなギャル）」としてふるまってください。明るく超ポジティブでフランクな話し言葉（「〜じゃん」「〜だし！」「ヤバい」など）を使い、絵文字や感嘆符を多く交えながら、ユーザーの記録行為自体を褒めちぎり、モチベーションを爆上げする全肯定の応援をしてください。ただし、コテコテすぎる表現（「ウチ」という一人称や、過剰なギャル特有の略語など）は避け、あくまで「親しみやすくてノリが良い、ポジティブな女友達」のような自然なフランクさを意識してください。"
        )

        withContext(Dispatchers.IO) {
            try {
                // 直近50件の喫煙記録を時系列順にフォーマット
                val recentRecords = records
                    .sortedByDescending { it.timestamp }
                    .take(50)
                    .reversed()

                val zone = ZoneId.systemDefault()
                val formattedRecords = recentRecords.mapIndexed { idx, r ->
                    val zdt = Instant.ofEpochMilli(r.timestamp).atZone(zone)
                    val dateStr = String.format(
                        "%d/%02d/%02d %02d:%02d",
                        zdt.year, zdt.monthValue, zdt.dayOfMonth, zdt.hour, zdt.minute
                    )
                    val intervalStr = if (idx > 0) {
                        val diffMins = (r.timestamp - recentRecords[idx - 1].timestamp) / (60 * 1000)
                        "${diffMins}分"
                    } else {
                        "なし"
                    }
                    "${idx + 1}. 日時: $dateStr, メモ: ${r.memo.ifEmpty { "なし" }}, 直前の喫煙からの経過時間: $intervalStr"
                }.joinToString("\n")

                val charPrompt = characterPrompts[characterId] ?: characterPrompts["uncle"]!!

                val prompt = """
                    $charPrompt
                    ユーザーの直近の喫煙記録を分析し、以下の3つの要素を生成してください。

                    1. スマートインサイト (smartInsight):
                       直近の喫煙本数の変化や、最近のペースなどに対する、キャラクターの個性を強く反映した簡潔で的確な禁煙・減煙のアドバイス（日本語で2〜3文）。
                    2. パターン分析 (patternAnalysis):
                       曜日別や喫煙間隔の傾向に基づき、ユーザーの行動傾向（例：「ついつい30分未満で吸っている回数が多い」「特定の時間帯に集中している」など）をキャラクターらしく指摘し、具体的な対策を提案する文章（日本語で2〜3文）。
                    3. 一言メッセージ (oneLiners):
                       「吸っちまった」ボタンを押した直後にユーザーに提示する、合計10個のメッセージ。キャラクターの口調を完璧に維持し、それぞれ20〜40文字程度で、以下の構成にしてください。
                       - 喫煙したことに対するコメント（励まし、ツッコミ、アドバイスなど）: 2件
                       - 喫煙とは全く関係のない、脈絡のない雑談や日常のどうでもいいコメント（例：キャラクターの好きな食べ物の話、今考えていること、どうでもいい豆知識など、喫煙とは完全に無関係な内容）: 8件

                    【ユーザーの喫煙記録】
                    $formattedRecords

                    必ず以下のJSONスキーマに従ってJSONを出力してください。他の余計な説明文やマークダウンタグ（```json等）は一切含めず、純粋なJSONオブジェクトのみを返してください。

                    {
                      "smartInsight": "文字列",
                      "patternAnalysis": "文字列",
                      "oneLiners": ["一言1", "一言2", "一言3", "一言4", "一言5", "一言6", "一言7", "一言8", "一言9", "一言10"]
                    }
                """.trimIndent()

                val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val requestBody = org.json.JSONObject().apply {
                    put("contents", org.json.JSONArray().put(org.json.JSONObject().apply {
                        put("parts", org.json.JSONArray().put(org.json.JSONObject().apply {
                            put("text", prompt)
                        }))
                    }))
                    put("generationConfig", org.json.JSONObject().apply {
                        put("responseMimeType", "application/json")
                    })
                }

                conn.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                android.util.Log.d("MokuMetricsLLM", "Gemini API Response Code: $responseCode")
                if (responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val resObj = org.json.JSONObject(responseText)
                    val candidate = resObj.getJSONArray("candidates").getJSONObject(0)
                    val text = candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")

                    val data = org.json.JSONObject(text)
                    val smartInsight = data.getString("smartInsight")
                    val patternAnalysis = data.getString("patternAnalysis")
                    val oneLinersArray = data.getJSONArray("oneLiners")

                    val editor = prefs.edit()
                    editor.putString("llm_smart_insight", smartInsight)
                    editor.putString("llm_pattern_analysis", patternAnalysis)
                    editor.putString("llm_oneliners", oneLinersArray.toString())
                    editor.putLong("llm_last_insight_update_time", now)
                    editor.apply()
                    android.util.Log.d("MokuMetricsLLM", "Gemini LLM data successfully updated and saved.")
                } else {
                    val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error stream available"
                    android.util.Log.e("MokuMetricsLLM", "Gemini API error ($responseCode): $errorText")
                }
            } catch (e: Exception) {
                android.util.Log.e("MokuMetricsLLM", "Exception during Gemini API request", e)
            }
        }
    }
}
