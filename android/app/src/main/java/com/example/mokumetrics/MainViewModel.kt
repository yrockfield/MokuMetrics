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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SmokeRepository
    val records: StateFlow<List<SmokeRecord>>

    private val sharedPreferences = application.getSharedPreferences("mokumetrics_prefs", Context.MODE_PRIVATE)
    
    private val _theme = mutableStateOf("aurora")
    val theme: State<String> = _theme

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
}
