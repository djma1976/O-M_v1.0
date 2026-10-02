package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventCategory: String, // "AUTH_SECURITY", "MFA_VERIFY", "PASSWORD_RECOVERY", "DISPATCH_LIFECYCLE", "GEO_TRACKING"
    val actor: String,
    val summary: String,
    val details: String,
    val isSecurityAlert: Boolean = false
)
