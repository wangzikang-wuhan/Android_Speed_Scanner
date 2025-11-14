package com.wzk.fast_scanner.data

import android.content.Context
import android.content.SharedPreferences

/**
 * @author wangzikang
 * @date 2025/9/12 12:23
 */

object SettingsManager{
    //保存整个设置的key
    private const val PREF_NAME = "app_settings"
    //App语言
    private const val LANGUAGE = "language"
    //OCR语言
    private const val OCR_LANGUAGE = "ocr_language"
    //启动记录
    private const val IS_FIRST_LUNCH = "is_first_launch"
    //暗黑模式
    private const val IS_DARK_MODE = "is_dark_mode"

    //SharedPreferences
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    //加载设置
    fun loadSettings(context: Context): AppSettings{
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        //加载设置
        val settings = AppSettings(
            language = prefs.getString(LANGUAGE, "zh") ?: "zh",
            ocrLanguage = prefs.getString(OCR_LANGUAGE, "local_en_zh") ?: "local_en_zh",
            isFirstLaunch = prefs.getBoolean(IS_FIRST_LUNCH, true),
            isDarkMode = prefs.getBoolean(IS_DARK_MODE, false)
        )
        return settings
    }

    //保存设置
    fun saveSettings(context: Context, settings: AppSettings){
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        //修改设置
        prefs.edit().apply{
            putString(LANGUAGE, settings.language)
            putString(OCR_LANGUAGE, settings.ocrLanguage)
            putBoolean(IS_FIRST_LUNCH, settings.isFirstLaunch)
            putBoolean(IS_DARK_MODE, settings.isDarkMode)
        }.apply()
    }


}