package com.example.mokumetrics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentTheme by viewModel.theme
            val records by viewModel.records.collectAsState()

            MokuMetricsTheme(themeName = currentTheme) {
                var activeTab by remember { mutableStateOf("home") }

                val customColors = LocalAppThemeColors.current

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "🚬", fontSize = 24.sp)
                                    Text(
                                        text = "MokuMetrics",
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            },
                            actions = {
                                Text(
                                    text = when (activeTab) {
                                        "home" -> "ダッシュボード"
                                        "stats" -> "分析グラフ"
                                        "history" -> "喫煙履歴"
                                        "settings" -> "アプリ設定"
                                        else -> ""
                                    },
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = customColors.textSecondary
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = activeTab == "home",
                                onClick = { activeTab = "home" },
                                icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("ホーム") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = customColors.textSecondary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                            NavigationBarItem(
                                selected = activeTab == "stats",
                                onClick = { activeTab = "stats" },
                                icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Stats") },
                                label = { Text("統計") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = customColors.textSecondary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                            NavigationBarItem(
                                selected = activeTab == "history",
                                onClick = { activeTab = "history" },
                                icon = { Icon(imageVector = Icons.Default.DateRange, contentDescription = "History") },
                                label = { Text("履歴") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = customColors.textSecondary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                            NavigationBarItem(
                                selected = activeTab == "settings",
                                onClick = { activeTab = "settings" },
                                icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("設定") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = customColors.textSecondary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = when (currentTheme) {
                                        "aurora" -> listOf(Color(0xFF0C2B1F), Color(0xFF07120E))
                                        "neon" -> listOf(Color(0xFF0E122B), Color(0xFF05050C))
                                        "cyberpunk" -> listOf(Color(0xFF050505), Color(0xFF15051A))
                                        else -> listOf(Color(0xFF0C2B1F), Color(0xFF07120E))
                                    }
                                )
                            )
                    ) {
                        when (activeTab) {
                            "home" -> HomeScreen(
                                records = records,
                                onAddRecord = { ts, memo -> viewModel.addRecord(ts, memo) }
                            )
                            "stats" -> StatsScreen(
                                records = records
                            )
                            "history" -> HistoryScreen(
                                records = records,
                                onAddRecord = { ts, memo -> viewModel.addRecord(ts, memo) },
                                onDeleteRecord = { id -> viewModel.deleteRecord(id) },
                                onUpdateRecord = { id, memo -> viewModel.updateRecord(id, memo) }
                            )
                            "settings" -> SettingsScreen(
                                currentTheme = currentTheme,
                                onThemeChange = { theme -> viewModel.setTheme(theme) },
                                onClearData = { viewModel.clearAllRecords() }
                            )
                        }
                    }
                }
            }
        }
    }
}
