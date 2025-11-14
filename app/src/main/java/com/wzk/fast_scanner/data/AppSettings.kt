package com.wzk.fast_scanner.data

/**
 * @author wangzikang
 * @date 2025/9/14 14:10
 */

//存储app的各种设置
data class AppSettings(

    //app语言
    val language: String = "zh",

    //ocr识别语言
    val ocrLanguage: String = "local_en_zh",

    //是否为首次启动
    val isFirstLaunch: Boolean = true,

    //暗黑模式
    val isDarkMode: Boolean = false
)