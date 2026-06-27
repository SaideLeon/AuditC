package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_records")
data class AuditRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val repoName: String,
    val repoUrl: String,
    val score: Int,
    val status: String,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val date: Long = System.currentTimeMillis(),
    val report: String,
    val blueprint: String
)
