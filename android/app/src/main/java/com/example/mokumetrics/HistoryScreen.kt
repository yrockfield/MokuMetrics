package com.example.mokumetrics

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    records: List<SmokeRecord>,
    onAddRecord: (Long, String) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onUpdateRecord: (String, String) -> Unit
) {
    val context = LocalContext.current
    var showAddForm by remember { mutableStateOf(false) }

    // 表示対象の日付 (初期値は今日)
    var displayDate by remember { mutableStateOf(LocalDate.now()) }

    // 手動追加の内部状態 (初期値は表示中の日付に連動)
    var selectedDate by remember { mutableStateOf(displayDate) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var manualMemo by remember { mutableStateOf("") }

    // 表示中日付が切り替わったら、手動追加のデフォルト日付も連動させる
    LaunchedEffect(displayDate) {
        selectedDate = displayDate
    }

    // 編集モード管理
    var editingId by remember { mutableStateOf<String?>(null) }
    var editMemoText by remember { mutableStateOf("") }

    val customColors = LocalAppThemeColors.current

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd (E)")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // 表示日付直接選択用の DatePickerDialog
    val displayDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            displayDate = LocalDate.of(year, month + 1, dayOfMonth)
        },
        displayDate.year,
        displayDate.monthValue - 1,
        displayDate.dayOfMonth
    )

    // 手動追加フォーム用の DatePickerDialog
    val manualDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    )

    // TimePickerDialog の表示
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            selectedTime = LocalTime.of(hourOfDay, minute)
        },
        selectedTime.hour,
        selectedTime.minute,
        true
    )

    // 表示中日付に一致するレコードをフィルタリング (タイムスタンプ降順)
    val filteredRecords = remember(records, displayDate) {
        records.filter { record ->
            val recordDate = Instant.ofEpochMilli(record.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            recordDate == displayDate
        }.sortedByDescending { it.timestamp }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 日付選択ナビゲーションヘッダー
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, customColors.cardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 前日へボタン
                IconButton(
                    onClick = { displayDate = displayDate.minusDays(1) }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "前日へ",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.rotate(180f)
                    )
                }

                // カレンダー直接選択用ボタン
                TextButton(
                    onClick = { displayDatePickerDialog.show() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = displayDate.format(dateFormatter),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "カレンダー選択",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 翌日へボタン
                IconButton(
                    onClick = { displayDate = displayDate.plusDays(1) }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "翌日へ",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 2. 手動追加の切り替えボタン
        Button(
            onClick = { showAddForm = !showAddForm },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, customColors.cardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (showAddForm) "閉じる" else "過去の喫煙を手動で追加",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 3. 手動追加フォーム
        if (showAddForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, customColors.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "過去データを追加",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // 日付選択
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "日付: ${selectedDate.format(dateFormatter)}", fontSize = 14.sp)
                        Button(
                            onClick = { manualDatePickerDialog.show() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Text(text = "日付を変更", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // 時間選択
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "時間: ${selectedTime.format(timeFormatter)}", fontSize = 14.sp)
                        Button(
                            onClick = { timePickerDialog.show() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Text(text = "時間を変更", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // メモ入力
                    OutlinedTextField(
                        value = manualMemo,
                        onValueChange = { manualMemo = it },
                        placeholder = { Text("例: 会食中、イライラしたため") },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("メモ") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val localDateTime = selectedDate.atTime(selectedTime)
                            val zonedDateTime = localDateTime.atZone(ZoneId.systemDefault())
                            val timestamp = zonedDateTime.toInstant().toEpochMilli()
                            onAddRecord(timestamp, manualMemo.trim())
                            manualMemo = ""
                            showAddForm = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = "追加する", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. 履歴リスト
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, customColors.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "喫煙履歴 (${filteredRecords.size}件)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (filteredRecords.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "この日の喫煙データはありません。",
                            color = customColors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredRecords, key = { it.id }) { record ->
                            val zdt = Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault())
                            val timeStr = zdt.format(timeFormatter)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = timeStr,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // メモ編集処理
                                    if (editingId == record.id) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = editMemoText,
                                                onValueChange = { editMemoText = it },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    onUpdateRecord(record.id, editMemoText.trim())
                                                    editingId = null
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Save",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { editingId = null }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Cancel"
                                                )
                                            }
                                        }
                                    } else {
                                        if (record.memo.isNotEmpty()) {
                                            Text(
                                                text = "「${record.memo}」",
                                                fontSize = 12.sp,
                                                color = customColors.textSecondary,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }

                                if (editingId != record.id) {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingId = record.id
                                                editMemoText = record.memo
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = customColors.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteRecord(record.id) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
    }
}
