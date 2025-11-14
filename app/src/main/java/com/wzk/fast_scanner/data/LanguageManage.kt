package com.wzk.fast_scanner.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
import com.wzk.fast_scanner.R

/**
 * @author wangzikang
 * @date 2025/9/13 12:05
 */

data class Language(
    val code: String,
    val name: String,
    val displayName: String,
    val flagResId: Int
)

//语言管理
object LanguageManage{
    val availableLanguages = listOf(
        Language("en", "English", "English",R.drawable.ic_language_en),
        Language("zh", "中文", "中文",R.drawable.ic_language_zh),
        Language("ja", "日本語", "日本語",R.drawable.ic_language_ja),
        Language("ko", "한국어", "한국어",R.drawable.ic_language_ko),
        Language("ru", "Русский", "Русский",R.drawable.ic_language_ru),
        Language("fr", "Français", "Français",R.drawable.ic_language_fr),
        Language("es", "Español", "Español",R.drawable.ic_language_es),
        Language("ar", "عربي", "عربي",R.drawable.ic_language_ar),
        Language("hi", "हिंदी", "हिंदी",R.drawable.ic_language_hindi),
        Language("bn","বাংলা","বাংলা",R.drawable.ic_language_bengali)
    )

    //设置语言
    fun setLanguage(context: Context, languageCode: String) {
        val locale = when (languageCode) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "ru" -> Locale("ru")
            "fr" -> Locale.FRENCH
            "es" -> Locale("es")
            "ar" -> Locale("ar")
            "hi" -> Locale("hi")
            "bn" -> Locale("bn")
            else -> Locale.ENGLISH
        }

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        context.createConfigurationContext(config)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    // 获取应用配置的Context
    fun getLocalizedContext(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "ru" -> Locale("ru")
            "fr" -> Locale.FRENCH
            "es" -> Locale("es")
            "ar" -> Locale("ar")
            "hi" -> Locale("hi")
            "bn" -> Locale("bn")
            else -> Locale.ENGLISH
        }

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return context
        }
    }

    fun getCurrentLanguage(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        return when (locale.language) {
            "zh" -> "zh"
            "ja" -> "ja"
            "ko" -> "ko"
            "ru" -> "ru"
            "fr" -> "fr"
            "es" -> "es"
            "ar" -> "ar"
            "hi" -> "hi"
            "bn" -> "bn"
            else -> "en"
        }

    }

    fun getLanguageByCode(code: String): Language? {
        return LanguageManage.availableLanguages.find { it.code == code }
    }

    fun getDisplayName(code: String): String {
        return getLanguageByCode(code)?.displayName ?: "English"
    }
}


