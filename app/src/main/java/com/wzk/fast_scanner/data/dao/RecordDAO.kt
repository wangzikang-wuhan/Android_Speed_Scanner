package com.wzk.fast_scanner.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.wzk.fast_scanner.data.RecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * @author wangzikang
 * @date 2025/11/13 9:42
 */
@Dao
interface RecordDAO {

    @Insert
    suspend fun insert(record: RecordEntity)

    @Query("SELECT * FROM records ORDER BY createTime DESC")
    fun getAll(): Flow<List<RecordEntity>>

    @Delete
    suspend fun delete(record: RecordEntity)

    @Query("DELETE FROM records")
    suspend fun clearAll()

}