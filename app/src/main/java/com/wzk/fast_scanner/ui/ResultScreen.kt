package com.wzk.fast_scanner.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wzk.fast_scanner.R
import com.wzk.fast_scanner.data.OCRRecordViewModel
import com.wzk.fast_scanner.data.ScanType
import kotlinx.coroutines.delay

/**
 * @author wangzikang
 * @date 2025/9/26 17:17
 */

/**
 * 扫描结果页面
 * @param bitmap 扫描的图片
 * @param recognizedText 识别出的文字
 * @param scanType 扫描类型
 * @param imagePath 图片保存路径
 * @param viewModel OCR记录ViewModel，用于保存结果
 * @param onBackClick 返回按钮点击事件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    bitmap: Bitmap? = null,
    recognizedText: String,
    scanType: ScanType = ScanType.OCR,
    imagePath: String? = null,
    viewModel: OCRRecordViewModel? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    
    // 自动保存到数据库
    LaunchedEffect(recognizedText) {
        viewModel?.addRecord(
            type = scanType,
            content = recognizedText,
            imagePath = imagePath
        )
        // 延迟显示内容，产生动画效果
        delay(100)
        showContent = true
    }
    
    // 复制成功后自动恢复图标
    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.scan_res),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "back",
                            tint = Color.Unspecified
                        )
                    }
                },
                actions = {
                    // 一键复制按钮
                    IconButton(
                        onClick = {
                            if (recognizedText.isNotEmpty()) {
                                copyToClipboard(context, recognizedText)
                                isCopied = true
                            } else {
                            }
                        }
                    ) {
                        Icon(
                            painter = if (isCopied) painterResource(R.drawable.ic_copy) else painterResource(R.drawable.ic_uncopy),
                            contentDescription = "copy",
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 显示图片（如果有）
                    bitmap?.let {
                        ImagePreviewCard(bitmap = it)
                    }
                    
                    // 显示识别的文字
                    RecognizedTextCard(text = recognizedText)
                    
                    // 统计信息
                    StatisticsCard(
                        characterCount = recognizedText.length,
                        wordCount = recognizedText.split(Regex("\\s+")).filter { it.isNotEmpty() }.size,
                        lineCount = recognizedText.lines().size
                    )
                }
            }
        }
    }
}

/**
 * 图片预览卡片
 */
@Composable
private fun ImagePreviewCard(bitmap: Bitmap) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * 识别文字卡片
 */
@Composable
private fun RecognizedTextCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.scan_content),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (text.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_text_rec),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Text(
                    text = text,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 统计信息卡片
 */
@Composable
private fun StatisticsCard(
    characterCount: Int,
    wordCount: Int,
    lineCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatisticItem(
                label = stringResource(R.string.char_number),
                value = characterCount.toString(),
                gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                )
            )
            
            StatisticItem(
                label = stringResource(R.string.poem_number),
                value = wordCount.toString(),
                gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFFF093FB), Color(0xFFF5576C))
                )
            )
            
            StatisticItem(
                label = stringResource(R.string.line_number),
                value = lineCount.toString(),
                gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))
                )
            )
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun StatisticItem(
    label: String,
    value: String,
    gradient: Brush
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(gradient, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 复制文本到剪贴板
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(context.getString(R.string.scan_res), text)
    clipboard.setPrimaryClip(clip)
}
