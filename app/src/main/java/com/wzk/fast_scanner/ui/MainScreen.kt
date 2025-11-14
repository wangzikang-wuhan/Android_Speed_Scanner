package com.wzk.fast_scanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wzk.fast_scanner.R
import com.wzk.fast_scanner.data.OCRRecordViewModel
import com.wzk.fast_scanner.data.RecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @author wangzikang
 * @date 2025/9/21 15:09
 */

/**
 * OCR 首页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    ocrRecordViewModel: OCRRecordViewModel,
    onCameraClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onDocumentScanClick: () -> Unit = {},
    onQRScanClick: () -> Unit = {},
    onToggleDarkMode: (Boolean) -> Unit = {},
    isDarkMode: Boolean = false,
    onRecordClick: (RecordEntity) -> Unit = {}
) {
    var showDarkModeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    // 左侧应用图标
                    Icon(
                        painter = painterResource(R.drawable.home_logo),
                        contentDescription = "logo",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(65.dp)
                            .padding(start = 4.dp)
                    )
                },
                actions = {
                    // 暗黑模式切换按钮
                    IconButton(onClick = { showDarkModeDialog = true }) {
                        Icon(
                            painter = if (isDarkMode)
                                painterResource(R.drawable.ic_dark)
                            else
                                painterResource(R.drawable.ic_light),
                            contentDescription = "change dark",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(40.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                // 设置较小的顶部间距
                windowInsets = WindowInsets(top = 8.dp)
            )
        },
        // 减少Scaffold的内容区域的内边距
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 功能按钮 - 一行四个
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureCard(
                    title = stringResource(R.string.caemra_scan),
                    icon = R.drawable.ic_camera,
                    gradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF667EEA),
                            Color(0xFF764BA2)
                        )
                    ),
                    onClick = onCameraClick,
                    modifier = Modifier.weight(1f)
                )
                
                FeatureCard(
                    title = stringResource(R.string.pictrue_scan),
                    icon = R.drawable.ic_picture,
                    gradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF093FB),
                            Color(0xFFF5576C)
                        )
                    ),
                    onClick = onGalleryClick,
                    modifier = Modifier.weight(1f)
                )
                
                FeatureCard(
                    title = stringResource(R.string.document_scan),
                    icon = R.drawable.ic_file,
                    gradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4FACFE),
                            Color(0xFF00F2FE)
                        )
                    ),
                    onClick = onDocumentScanClick,
                    modifier = Modifier.weight(1f)
                )
                
                FeatureCard(
                    title = stringResource(R.string.qr_code_scan),
                    icon = R.drawable.ic_qrcode,
                    gradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF43E97B),
                            Color(0xFF38F9D7)
                        )
                    ),
                    onClick = onQRScanClick,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 最近记录
            RecentRecordsSection(
                ocrRecordViewModel = ocrRecordViewModel,
                onRecordClick = onRecordClick
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // 暗黑模式切换对话框
    if (showDarkModeDialog) {
        AlertDialog(
            onDismissRequest = { showDarkModeDialog = false },
            title = { Text(stringResource(R.string.topic_setting)) },
            text = {
                Column {
                    Text(stringResource(R.string.selected_topic))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = !isDarkMode,
                            onClick = {
                                onToggleDarkMode(false)
                                showDarkModeDialog = false
                            },
                            label = { Text(stringResource(R.string.light_mode)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_light),
                                    contentDescription = null,
                                    tint = Color.Unspecified
                                )
                            }
                        )
                        FilterChip(
                            selected = isDarkMode,
                            onClick = {
                                onToggleDarkMode(true)
                                showDarkModeDialog = false
                            },
                            label = { Text(stringResource(R.string.drak_mode)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_dark),
                                    contentDescription = null,
                                    tint = Color.Unspecified
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDarkModeDialog = false }) {
                    Text(stringResource(R.string.confrim))
                }
            }
        )
    }
}

/**
 * 功能卡片
 */
@Composable
fun FeatureCard(
    title: String,
    icon: Int,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = title,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 最近记录区域
 */
@Composable
fun RecentRecordsSection(
    ocrRecordViewModel: OCRRecordViewModel,
    onRecordClick: (RecordEntity) -> Unit = {}
) {
    // 观察历史记录，按时间倒序排列（最新的在前面）
    val records by ocrRecordViewModel.records.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentRecords = records.sortedByDescending { it.createTime }.take(5)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            if (recentRecords.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.items,records.size),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (recentRecords.isEmpty()) {
            // 空状态
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_history),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.not_recent),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // 显示记录列表
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentRecords.forEach { record ->
                    RecordItemCard(
                        record = record,
                        onClick = { onRecordClick(record) }
                    )
                }
            }
        }
    }
}

/**
 * 历史记录条目卡片
 */
@Composable
fun RecordItemCard(
    record: RecordEntity,
    onClick: () -> Unit = {}
) {
    // 格式化时间
    val timeFormatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val timeText = timeFormatter.format(Date(record.createTime))
    
    // 获取类型图标和颜色
    val (typeIcon, typeColor, typeText) = when (record.type) {
        "scan code" -> Triple(R.drawable.ic_qrcode, Color(0xFF43E97B), "QR Code")
        "document" -> Triple(R.drawable.ic_file, Color(0xFF4FACFE), "Document")
        else -> Triple(R.drawable.ic_camera, Color(0xFF667EEA), "OCR")
    }
    
    // 截取内容，最多30个字符
    val displayContent = if (record.content.length > 30) {
        record.content.take(30) + "..."
    } else {
        record.content
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = typeColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(typeIcon),
                    contentDescription = typeText,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 内容区域
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 类型标签
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = typeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeText,
                            fontSize = 10.sp,
                            color = typeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 时间
                    Text(
                        text = timeText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // 内容预览
                Text(
                    text = displayContent,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
