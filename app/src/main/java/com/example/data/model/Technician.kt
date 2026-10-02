package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TechStatus(val label: String, val canAcceptTasks: Boolean) {
    AVAILABLE("Available", true),
    ON_SITE("On-Site", false),
    OFFLINE("Offline", false),
    EN_ROUTE("En Route", false),
    AWAITING_PARTS("Awaiting Parts", false),
    ON_BREAK("On Break", false),
    OFF_DUTY("Offline", false),
    EMERGENCY_ASSIST("Emergency Assist", false)
}

@Entity(tableName = "technicians")
data class TechnicianEntity(
    @PrimaryKey val id: String,
    val name: String,
    val badgeNumber: String,
    val roleTitle: String,
    val phone: String,
    val status: TechStatus,
    val currentTaskId: String?,
    val currentTaskTitle: String?,
    val latitude: Double,
    val longitude: Double,
    val headingDegrees: Float = 0f,
    val speedKmh: Float = 0f,
    val batteryPct: Int = 92,
    val signalStrength: String = "5G Encrypted", // "5G Encrypted", "SatCom", "LTE-M", "Offline-Cached"
    val vehicleId: String = "TRUCK-408",
    val certifications: String = "NFPA-70E, OSHA-30, EPA Universal 608",
    val avatarInitials: String = "TR",
    val lastPingTimestamp: Long = System.currentTimeMillis()
)
