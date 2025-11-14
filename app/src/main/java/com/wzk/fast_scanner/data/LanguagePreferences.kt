package com.wzk.fast_scanner.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * @author wangzikang
 * @date 2025/9/13 11:25
 */

//语言存储对象 object对象相当于单例类
object LanguagePreferences {

    //定义各种key
    private const val PREF_NAME = "language_preferences"
    private const val KEY_LANGUAGE = "selected_language"

    //获取SharedPreferences实例
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    //保存语言
    fun saveLanguage(context: Context, languageCode: String) {
        //apply异步操作
        getSharedPreferences(context).edit().putString(KEY_LANGUAGE, languageCode).apply()
        Log.d("LanguagePreferences", "Saved language: $languageCode")
    }

    //获取SharedPreferences语言
    fun getLanguage(context: Context): String {
        //获取语言 没有的话默认是英语
        val language = getSharedPreferences(context).getString(KEY_LANGUAGE, "en") ?: "en"
        Log.d("LanguagePreferences", "Retrieved language: $language")
        return language
    }
} 