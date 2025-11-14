package com.wzk.fast_scanner.data

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @author wangzikang
 * @date 2025/11/02 14:35
 */
class MainViewModel : ViewModel() {

    //内部可变
    private val _settings = MutableStateFlow(AppSettings())

    // 外部只读
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private var context: Context? = null

    //初始化
    fun init(context: Context){
        this.context = context
        loadSettings()
    }

    //加载设置
    private fun loadSettings() {
        context?.let { ctx ->
            //从本地per中加载设置
            _settings.value = SettingsManager.loadSettings(ctx)
        }
    }

    //更新设置
    fun updateSettings(newSettings: AppSettings){
        //老的配置
        val oldSettings = _settings.value
        //新的配置存到类里面
        _settings.value = newSettings
        //保存到手机本地
        SettingsManager.saveSettings(context!!, newSettings)
    }


}