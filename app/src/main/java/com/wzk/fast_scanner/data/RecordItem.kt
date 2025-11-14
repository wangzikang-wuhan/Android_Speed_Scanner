package com.wzk.fast_scanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author wangzikang
 * @date 2025/11/12 17:59
 */

@Entity(tableName = "records")
data class RecordEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: String,

    val content: String,

    val imagePath: String,

    val createTime: Long = System.currentTimeMillis(),

    val updateTime: Long = System.currentTimeMillis()

)

enum class ScanModel(val modelCode: String){
    LOCAL_EN_ZH("local_en_zh"),
    EN("en"),
    ZH("zh"),
    KO("ko"),
    JA("ja"),
}



enum class ScanType(val mode: String){
    OCR("ocr"),
    SCAN_CODE("scan code"),
    DOCUMENT_SCAN("document")
}