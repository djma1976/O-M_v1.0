package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskPriority(val label: String, val level: Int) {
    P1_CRITICAL("P1 Critical Outage", 1),
    P2_HIGH("P2 High Urgency", 2),
    P3_MEDIUM("P3 Scheduled Repair", 3),
    P4_ROUTINE("P4 Routine Inspection", 4)
}

enum class TaskStatus(val label: String) {
    PENDING_DISPATCH("Pending Dispatch"),
    DISPATCHED("Dispatched (Awaiting Acceptance)"),
    ACCEPTED("Accepted by Tech"),
    EN_ROUTE("Technician En Route"),
    ON_SITE_WORKING("In Progress On-Site"),
    AWAITING_PARTS("Awaiting Parts"),
    COMPLETED("Completed & Verified"),
    REJECTED("Declined by Tech")
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val assetTag: String,
    val equipmentCategory: String, // e.g., "Industrial Chiller", "345kV Transformer", "Main Water Pump"
    val siteName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val priority: TaskPriority,
    val status: TaskStatus,
    val assignedTechId: String?,
    val assignedTechName: String?,
    val slaDeadline: String,
    val hazardAlert: String?, // e.g., "Arc Flash Category 3 - PPE Required", "High Pressure Ammonia"
    val requiredTools: String, // e.g., "Calibrated Torque Wrench, Fluke 87V Multimeter, Refrigerant Gauge"
    val checklistJson: String, // Formatted checklist lines e.g. "Lockout Tagout Applied::true;Isolate pressure valve::false"
    val resolutionNotes: String = "",
    val signatureName: String = "",
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val completedAt: Long? = null
)
