package com.wzk.fast_scanner.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wzk.fast_scanner.data.dao.RecordDAO

/**
 * @author wangzikang
 * @date 2025/11/13 14:10
 */

/**
 * OCRRecordViewModel工厂类
 */
class OCRRecordViewModelFactory(
    private val dao: RecordDAO
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OCRRecordViewModel::class.java)) {
            return OCRRecordViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

