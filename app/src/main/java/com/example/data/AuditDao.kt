package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {

    @Query("SELECT * FROM audit_records ORDER BY date DESC")
    fun getAllAudits(): Flow<List<AuditRecord>>

    @Query("SELECT * FROM audit_records WHERE id = :id LIMIT 1")
    suspend fun getAuditById(id: Int): AuditRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(record: AuditRecord): Long

    @Delete
    suspend fun deleteAudit(record: AuditRecord)

    @Query("DELETE FROM audit_records WHERE id = :id")
    suspend fun deleteAuditById(id: Int)
}
