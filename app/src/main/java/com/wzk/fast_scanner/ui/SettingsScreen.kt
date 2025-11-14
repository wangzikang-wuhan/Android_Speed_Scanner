package com.wzk.fast_scanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wzk.fast_scanner.R
import com.wzk.fast_scanner.data.Language
import com.wzk.fast_scanner.data.LanguageManage

/**
 * @author wangzikang
 * @date 2025/9/20 15:09
 */

/**
 * 设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentLanguage: String = "zh",
    onLanguageChange: (String) -> Unit = {},
    currentOCRLanguage: String = "local_en_zh",
    onOCRLanguageChange: (String) -> Unit = {},
    onClearCache: () -> Unit = {},
    onClearHistory: () -> Unit = {}
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showOCRModeDialog by remember { mutableStateOf(false) }
    var showOCRLanguageDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                // 设置较小的顶部间距
                windowInsets = WindowInsets(top = 8.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // 常规设置
            SettingsSection(title = stringResource(R.string.gs)) {
                SettingsItem(
                    icon = R.drawable.ic_language,
                    title = stringResource(R.string.languages),
                    subtitle = LanguageManage.getDisplayName(currentLanguage),
                    onClick = { showLanguageDialog = true }
                )
                
                Divider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                
                SettingsItem(
                    icon = R.drawable.ic_scan,
                    title = stringResource(R.string.ocr_mode),
                    subtitle = when(currentOCRLanguage) {
                        "local_en_zh" -> stringResource(R.string.zh_en_local)
                        "en" -> stringResource(R.string.kit_en)
                        "zh" -> stringResource(R.string.kit_zh)
                        "ko" -> stringResource(R.string.kit_ko)
                        "ja" -> stringResource(R.string.kit_ja)
                        else -> stringResource(R.string.kit_other)
                    },
                    onClick = { showOCRLanguageDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 数据管理
            SettingsSection(title = stringResource(R.string.data_manage)) {
                SettingsItem(
                    icon = R.drawable.ic_clean,
                    title = stringResource(R.string.clear_record),
                    subtitle = stringResource(R.string.delete_all),
                    onClick = { showClearHistoryDialog = true },
                    isDangerous = true
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 关于
            SettingsSection(title = stringResource(R.string.about)) {
                SettingsItem(
                    icon = R.drawable.ic_about,
                    title = stringResource(R.string.app_version),
                    subtitle = "v1.0.0",
                    onClick = {}
                )
                
                Divider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                
                SettingsItem(
                    icon = R.drawable.ic_statement,
                    title = stringResource(R.string.terms_use),
                    subtitle = stringResource(R.string.terms_use_and_developer),
                    onClick = { showTermsDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

        }
    }
    
    // 语言选择对话框
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { lang ->
                onLanguageChange(lang)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    
    // OCR 语言选择对话框
    if (showOCRLanguageDialog) {
        OCRLanguageSelectionDialog(
            currentLanguage = currentOCRLanguage,
            onLanguageSelected = { language ->
                onOCRLanguageChange(language)
                showOCRLanguageDialog = false
            },
            onDismiss = { showOCRLanguageDialog = false }
        )
    }
    
    // 清除缓存确认对话框
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = { Icon(
                painter = painterResource(R.drawable.ic_clean),
                contentDescription = null) },
            title = { Text(stringResource(R.string.clear_cache)) },
            text = { Text(stringResource(R.string.clear_sure)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearCache()
                        showClearCacheDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confrim))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 清除记录确认对话框
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.clear_record_all)) },
            text = { Text(stringResource(R.string.clear_record_all_sure)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.confrim))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // 使用条款和开发者信息对话框
    if (showTermsDialog) {
        TermsAndDeveloperDialog(
            onDismiss = { showTermsDialog = false }
        )
    }
}

/**
 * 设置分组
 */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

/**
 * 设置项
 */
@Composable
fun SettingsItem(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDangerous: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isDangerous)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = Color.Unspecified,
                modifier = Modifier.size(35.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDangerous)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        Icon(
            painter = painterResource(R.drawable.ic_in),
            contentDescription = null,
            tint = Color.Unspecified
        )
    }
}

/**
 * 语言选择对话框
 */
@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.selected_language)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                LanguageManage.availableLanguages.forEach { language ->
                    LanguageItem(
                        language = language,
                        languageIcon = language.flagResId,
                        isSelected = language.code == currentLanguage,
                        onClick = { onLanguageSelected(language.code) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * 语言选项
 */
@Composable
fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    languageIcon: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painter = painterResource(languageIcon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(30.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${language.displayName} (${language.name})",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


/**
 * OCR 模式选项
 */
@Composable
fun OCRModeItem(
    title: String,
    mode: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * OCR 语言选择对话框
 */
@Composable
fun OCRLanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ocr_mode)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OCRLanguageItem(
                    title = stringResource(R.string.zh_en_local),
                    description = "",
                    languageCode = "local_en_zh",
                    isSelected = currentLanguage == "local_en_zh",
                    onClick = { onLanguageSelected("local_en_zh") }
                )
                OCRLanguageItem(
                    title = stringResource(R.string.kit_en),
                    description = "",
                    languageCode = "en",
                    isSelected = currentLanguage == "en",
                    onClick = { onLanguageSelected("en") }
                )
                OCRLanguageItem(
                    title = stringResource(R.string.kit_zh),
                    description = "",
                    languageCode = "zh",
                    isSelected = currentLanguage == "zh",
                    onClick = { onLanguageSelected("zh") }
                )
                OCRLanguageItem(
                    title = stringResource(R.string.kit_ko),
                    description = "",
                    languageCode = "ko",
                    isSelected = currentLanguage == "ko",
                    onClick = { onLanguageSelected("ko") }
                )
                OCRLanguageItem(
                    title = stringResource(R.string.kit_ja),
                    description = "",
                    languageCode = "ja",
                    isSelected = currentLanguage == "ja",
                    onClick = { onLanguageSelected("ja") }
                )
                OCRLanguageItem(
                    title = stringResource(R.string.kit_other),
                    description = "",
                    languageCode = "other",
                    isSelected = currentLanguage == "other",
                    onClick = { onLanguageSelected("other") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * OCR 语言选项
 */
@Composable
fun OCRLanguageItem(
    title: String,
    description: String,
    languageCode: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 使用条款和开发者信息对话框
 */
@Composable
fun TermsAndDeveloperDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "关于应用",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 应用信息
                InfoSection(
                    title = "📱 应用信息",
                    content = """
                        应用名称：Speed Scanner
                        版本：v1.0.0
                        类型：OCR 文字识别工具
                    """.trimIndent()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 开发者信息
                InfoSection(
                    title = "👨‍💻 开发者信息",
                    content = """
                        开发者：wangzikang
                        开发时间：2025年
                    """.trimIndent()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 功能特性
                InfoSection(
                    title = "✨ 功能特性",
                    content = """
                        • 本地中英文OCR识别
                        • 多语言OCR支持（中文、英文、日语、韩语等）
                        • 二维码/条形码扫描
                        • 文档扫描与矫正
                        • 相册图片识别
                        • 识别历史记录
                        • 国际化界面支持（10种语言随意切换）
                    """.trimIndent()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 技术说明
                InfoSection(
                    title = "🔧 技术说明",
                    content = """
                        • PaddleOCR：本地中英文离线识别引擎
                        • Google ML Kit：多语言OCR识别
                        • OpenCV：图像处理与文档矫正
                        • Jetpack Compose：现代化UI框架
                    """.trimIndent()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 联系方式
                InfoSection(
                    title = "📧 联系我",
                    content = """
                        如有问题或建议，欢迎反馈
                        开发者：wangzikang
                        邮箱: imwuhanwangzikang@gmail.com
                    """.trimIndent()
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // 联系方式
                InfoSection(
                    title = "🎈 使用说明",
                    content = """
                        静态图标来自于：https://icons8.com
                        动态图标来自于：https://lottiefiles.com/
                        PaddleOCR开源库：https://github.com/equationl/paddleocr4android/
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                // 版权信息
                Text(
                    text = "© 2025 wangzikang. All rights reserved.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 信息区块组件
 */
@Composable
fun InfoSection(
    title: String,
    content: String
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            lineHeight = 20.sp
        )
    }
}
