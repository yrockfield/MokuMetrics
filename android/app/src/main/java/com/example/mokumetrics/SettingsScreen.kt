package com.example.mokumetrics

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    onClearData: () -> Unit,
    onExportData: () -> String,
    onImportData: (String) -> Boolean
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val customColors = LocalAppThemeColors.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                val json = onExportData()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter().use { it.write(json) }
                    Toast.makeText(context, "エクスポートが完了しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    pendingImportJson = jsonString
                    showImportConfirmDialog = true
                }
            }
        }
    )

    val themesList = listOf(
        ThemeOption(
            id = "aurora",
            name = "Aurora Green (オーロラ)",
            description = "神秘的なグリーンとダークネオンの癒やし系テーマ",
            color = Color(0xFF10B981)
        ),
        ThemeOption(
            id = "neon",
            name = "Dark Neon (ダークネオン)",
            description = "近未来を漂わせる紫とシアンのサイバーダークテーマ",
            color = Color(0xFF8B5CF6)
        ),
        ThemeOption(
            id = "cyberpunk",
            name = "Cyberpunk (サイバーパンク)",
            description = "ビビッドな黄色とマゼンタピンクが映える漆黒テーマ",
            color = Color(0xFFFACC15)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. テーマ選択セクション
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, customColors.cardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "テーマの選択",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "アプリの視覚デザインを変更します。すべてのテーマが最初から選択可能です。",
                    style = MaterialTheme.typography.bodySmall,
                    color = customColors.textSecondary
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    themesList.forEach { t ->
                        val isSelected = currentTheme == t.id
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else customColors.cardBorder
                        val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable { onThemeChange(t.id) }
                                .padding(14.dp, 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = t.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = t.description,
                                    fontSize = 11.sp,
                                    color = customColors.textSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // プレビューの丸
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(t.color)
                            )
                        }
                    }
                }
            }
        }

        // 2. データの管理セクション（インポート・エクスポート）
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, customColors.cardBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "データの管理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "喫煙履歴データのバックアップ（エクスポート）や、過去のバックアップデータの復元（インポート）が行えます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = customColors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                          onClick = { exportLauncher.launch("mokumetrics_data_${System.currentTimeMillis()}.json") },
                          modifier = Modifier.weight(1f),
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                          shape = RoundedCornerShape(12.dp),
                          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "エクスポート",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                          onClick = { importLauncher.launch(arrayOf("application/json")) },
                          modifier = Modifier.weight(1f),
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                          shape = RoundedCornerShape(12.dp),
                          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "インポート",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. データの削除セクション
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "データの削除",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "データベースに保存されているすべての喫煙履歴をリセットして初期状態に戻します。この操作は取り消せません。",
                    style = MaterialTheme.typography.bodySmall,
                    color = customColors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onError
                        )
                        Text(
                            text = "全データを消去してリセット",
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // アプリバージョン情報
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MokuMetrics v1.0.0 (Android Client)",
                fontSize = 11.sp,
                color = customColors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "「また吸っちまった」を価値あるデータに変えるダッシュボード",
                fontSize = 11.sp,
                color = customColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    // リセット確認ダイアログ
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = "警告") },
            text = { Text(text = "これまでのすべての喫煙記録が消去されます。よろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearData()
                        showResetDialog = false
                    }
                ) {
                    Text(text = "消去する", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = "キャンセル")
                }
            }
        )
    }

    // インポート確認ダイアログ
    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { 
                showImportConfirmDialog = false 
                pendingImportJson = null
            },
            title = { Text(text = "データのインポート") },
            text = { Text(text = "データをインポートすると、現在のすべての記録が上書きされます。よろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportJson?.let { json ->
                            val success = onImportData(json)
                            if (success) {
                                Toast.makeText(context, "インポートが完了しました", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "インポートに失敗しました。正しいJSONファイルか確認してください", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showImportConfirmDialog = false
                        pendingImportJson = null
                    }
                ) {
                    Text(text = "インポートする", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showImportConfirmDialog = false 
                        pendingImportJson = null
                    }
                ) {
                    Text(text = "キャンセル")
                }
            }
        )
    }
}

data class ThemeOption(
    val id: String,
    val name: String,
    val description: String,
    val color: Color
)
