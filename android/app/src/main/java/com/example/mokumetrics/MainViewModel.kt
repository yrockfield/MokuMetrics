package com.example.mokumetrics

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SmokeRepository
    val records: StateFlow<List<SmokeRecord>>

    private val sharedPreferences = application.getSharedPreferences("mokumetrics_prefs", Context.MODE_PRIVATE)
    
    private val _theme = mutableStateOf("aurora")
    val theme: State<String> = _theme

    private val _apiKey = mutableStateOf("")
    val apiKey: State<String> = _apiKey

    private val _character = mutableStateOf("uncle")
    val character: State<String> = _character

    private val _llmSmartInsight = mutableStateOf<String?>(null)
    val llmSmartInsight: State<String?> = _llmSmartInsight

    private val _llmPatternAnalysis = mutableStateOf<String?>(null)
    val llmPatternAnalysis: State<String?> = _llmPatternAnalysis

    private val _llmOneLiners = mutableStateOf<List<String>?>(null)
    val llmOneLiners: State<List<String>?> = _llmOneLiners

    private val _llmLastUpdateTime = mutableStateOf<Long>(0L)
    val llmLastUpdateTime: State<Long> = _llmLastUpdateTime

    init {
        val database = SmokeDatabase.getDatabase(application)
        repository = RoomSmokeRepositoryImpl(database.smokeDao())
        
        // Room データを StateFlow に変換
        records = repository.getAllRecords()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // 保存されたテーマをロード
        _theme.value = sharedPreferences.getString("theme", "aurora") ?: "aurora"
        
        // APIキーをロード
        _apiKey.value = sharedPreferences.getString("llm_api_key", "") ?: ""

        // キャラクターをロード
        _character.value = sharedPreferences.getString("llm_character", "uncle") ?: "uncle"

        // LLMデータをロード
        loadLlmData()
    }

    fun addRecord(timestamp: Long, memo: String) {
        viewModelScope.launch {
            val record = SmokeRecord(
                id = (System.currentTimeMillis() + (1..1000).random()).toString(),
                timestamp = timestamp,
                memo = memo
            )
            repository.insertRecord(record)
            notifyWidgetUpdate()
            
            // バックグラウンドで Gemini API 更新を要求
            viewModelScope.launch(Dispatchers.IO) {
                SmokeAnalytics.updateLlmDataIfNeeded(
                    context = getApplication(),
                    records = records.value,
                    apiKey = _apiKey.value,
                    characterId = _character.value
                )
                launch(Dispatchers.Main) {
                    loadLlmData()
                }
            }
        }
    }

    fun setApiKey(apiKey: String) {
        _apiKey.value = apiKey
        sharedPreferences.edit().putString("llm_api_key", apiKey).apply()
    }

    fun setCharacter(characterId: String) {
        _character.value = characterId
        sharedPreferences.edit().putString("llm_character", characterId).apply()
        // キャラクター変更時は最終更新日時をクリアして即座にインサイトを再生成させる
        sharedPreferences.edit().putLong("llm_last_insight_update_time", 0L).apply()
        _llmLastUpdateTime.value = 0L
    }

    private fun loadLlmData() {
        _llmSmartInsight.value = sharedPreferences.getString("llm_smart_insight", null)
        _llmPatternAnalysis.value = sharedPreferences.getString("llm_pattern_analysis", null)
        _llmLastUpdateTime.value = sharedPreferences.getLong("llm_last_insight_update_time", 0L)
        
        val oneLinersJson = sharedPreferences.getString("llm_oneliners", null)
        _llmOneLiners.value = if (oneLinersJson != null) {
            parseOneLiners(oneLinersJson)
        } else {
            null
        }
    }

    private fun parseOneLiners(jsonStr: String): List<String>? {
        return try {
            val arr = org.json.JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch {
            repository.deleteRecord(id)
            notifyWidgetUpdate()
        }
    }

    fun updateRecord(id: String, memo: String) {
        viewModelScope.launch {
            repository.updateMemo(id, memo)
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            repository.clearAllRecords()
            notifyWidgetUpdate()
        }
    }

    fun setTheme(themeName: String) {
        _theme.value = themeName
        sharedPreferences.edit().putString("theme", themeName).apply()
    }

    /**
     * ウィジェットの更新をOSに要求するブロードキャストを送信
     */
    private fun notifyWidgetUpdate() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, SmokeWidgetReceiver::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, SmokeWidgetReceiver::class.java)
        )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    fun exportToJson(): String {
        val jsonArray = org.json.JSONArray()
        records.value.forEach { record ->
            val obj = org.json.JSONObject()
            obj.put("id", record.id)
            obj.put("timestamp", record.timestamp)
            obj.put("memo", record.memo)
            jsonArray.put(obj)
        }
        return jsonArray.toString(4)
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val jsonArray = org.json.JSONArray(jsonString)
            val newRecords = mutableListOf<SmokeRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", (System.currentTimeMillis() + i).toString())
                val timestamp = obj.getLong("timestamp")
                val memo = obj.optString("memo", "")
                newRecords.add(SmokeRecord(id, timestamp, memo))
            }
            viewModelScope.launch {
                repository.clearAllRecords()
                newRecords.forEach { repository.insertRecord(it) }
                notifyWidgetUpdate()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
