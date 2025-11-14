package com.wzk.fast_scanner.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzk.fast_scanner.data.dao.RecordDAO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * @author wangzikang
 * @date 2025/11/13 9:51
 */
class OCRRecordViewModel(
    private val dao: RecordDAO
) : ViewModel() {

    val records: Flow<List<RecordEntity>> = dao.getAll()

    fun addRecord(type: ScanType ,content: String, imagePath: String?) {
        viewModelScope.launch {
            dao.insert(RecordEntity(
                type = type.mode,
                content = content,
                imagePath = imagePath?.toString() ?: "")
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }
}