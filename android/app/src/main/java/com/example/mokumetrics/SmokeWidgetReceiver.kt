package com.example.mokumetrics

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmokeWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // バックグラウンドでデータベースからデータを取得してウィジェットを更新
        CoroutineScope(Dispatchers.IO).launch {
            val database = SmokeDatabase.getDatabase(context)
            val repository = RoomSmokeRepositoryImpl(database.smokeDao())
            val records = repository.getAllRecordsSync()
            val todayCount = SmokeAnalytics.getTodayCount(records)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, todayCount)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // 「吸っちまった」ボタンが押された場合のアクション
        if (intent.action == "com.example.mokumetrics.ADD_SMOKE") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = SmokeDatabase.getDatabase(context)
                    val repository = RoomSmokeRepositoryImpl(database.smokeDao())
                    
                    // 新しい喫煙記録を追加
                    val newRecord = SmokeRecord(
                        id = (System.currentTimeMillis() + (1..1000).random()).toString(),
                        timestamp = System.currentTimeMillis(),
                        memo = "ウィジェットより"
                    )
                    repository.insertRecord(newRecord)

                    // データを更新するためにウィジェット自身を再更新
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val thisAppWidget = ComponentName(context.packageName, SmokeWidgetReceiver::class.java.name)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
                    
                    val updatedRecords = repository.getAllRecordsSync()
                    val todayCount = SmokeAnalytics.getTodayCount(updatedRecords)
                    
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId, todayCount)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            todayCount: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            // 件数を更新
            views.setTextViewText(R.id.widget_count_text, "$todayCount 本")

            // ボタンタップ時の Intent 設定
            val intent = Intent(context, SmokeWidgetReceiver::class.java).apply {
                action = "com.example.mokumetrics.ADD_SMOKE"
            }
            
            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                flag
            )
            
            views.setOnClickPendingIntent(R.id.widget_button_record, pendingIntent)

            // アプリ起動用のタイトルタップインテント
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context,
                1,
                appIntent,
                flag
            )
            views.setOnClickPendingIntent(R.id.widget_container, appPendingIntent)

            // ウィジェットマネージャーに反映
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
