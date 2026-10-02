package com.example.data.repository

import com.example.data.local.AuditDao
import com.example.data.local.TaskDao
import com.example.data.local.TechnicianDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TechStatus
import com.example.data.model.TechnicianEntity
import com.example.data.service.TechnicianStatusUpdateService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class FieldOpsRepository(
    private val taskDao: TaskDao,
    private val technicianDao: TechnicianDao,
    private val auditDao: AuditDao
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val statusUpdateService = TechnicianStatusUpdateService(technicianDao, auditDao, serviceScope)

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasksFlow()
    val allTechnicians: Flow<List<TechnicianEntity>> = technicianDao.getAllTechniciansFlow()
    val recentAuditLogs: Flow<List<AuditLogEntity>> = auditDao.getRecentAuditLogsFlow()

    fun getTaskById(taskId: String): Flow<TaskEntity?> = taskDao.getTaskByIdFlow(taskId)

    suspend fun acceptTask(taskId: String, techId: String, techName: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(taskId) ?: return@withContext
        val now = System.currentTimeMillis()

        // 1. Update task to ACCEPTED
        taskDao.updateTaskStatus(taskId, TaskStatus.ACCEPTED, acceptedAt = now)

        // 2. Update technician status to EN_ROUTE (or ON_SITE)
        technicianDao.updateTechStatus(techId, TechStatus.EN_ROUTE, taskId = taskId, taskTitle = task.title)

        // 3. Log audit event
        auditDao.insertLog(
            AuditLogEntity(
                eventCategory = "DISPATCH_LIFECYCLE",
                actor = "$techName ($techId)",
                summary = "Task Accepted: ${task.title}",
                details = "Technician accepted dispatched assignment for ${task.equipmentCategory} at ${task.siteName}.",
                isSecurityAlert = false
            )
        )
    }

    suspend fun declineTask(taskId: String, techId: String, techName: String, reason: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(taskId) ?: return@withContext

        // 1. Update task to REJECTED
        taskDao.declineTask(taskId, TaskStatus.REJECTED, reason = reason)

        // 2. Set technician back to AVAILABLE if they were assigned to this task
        val tech = technicianDao.getTechnicianById(techId)
        if (tech?.currentTaskId == taskId) {
            technicianDao.updateTechStatus(techId, TechStatus.AVAILABLE, taskId = null, taskTitle = null)
        }

        // 3. Log audit event
        auditDao.insertLog(
            AuditLogEntity(
                eventCategory = "DISPATCH_LIFECYCLE",
                actor = "$techName ($techId)",
                summary = "Task Declined: ${task.title}",
                details = "Assignment declined. Reason: $reason",
                isSecurityAlert = true
            )
        )
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: TaskStatus, techId: String, techName: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(taskId) ?: return@withContext
        taskDao.updateTaskStatus(taskId, newStatus)

        val newTechStatus = when (newStatus) {
            TaskStatus.ACCEPTED -> TechStatus.AVAILABLE
            TaskStatus.EN_ROUTE -> TechStatus.EN_ROUTE
            TaskStatus.ON_SITE_WORKING -> TechStatus.ON_SITE
            TaskStatus.AWAITING_PARTS -> TechStatus.AWAITING_PARTS
            TaskStatus.COMPLETED -> TechStatus.AVAILABLE
            else -> null
        }

        if (newTechStatus != null && task.assignedTechId == techId) {
            val taskTitle = if (newStatus == TaskStatus.COMPLETED) null else task.title
            val currentTaskId = if (newStatus == TaskStatus.COMPLETED) null else taskId
            technicianDao.updateTechStatus(techId, newTechStatus, currentTaskId, taskTitle)
        }

        auditDao.insertLog(
            AuditLogEntity(
                eventCategory = "DISPATCH_LIFECYCLE",
                actor = "$techName ($techId)",
                summary = "Task Status Updated: ${newStatus.label}",
                details = "Work order $taskId updated to ${newStatus.label} by $techName.",
                isSecurityAlert = false
            )
        )
    }

    suspend fun updateTechStatusManual(techId: String, newStatus: TechStatus, techName: String) = withContext(Dispatchers.IO) {
        val currentTech = technicianDao.getTechnicianById(techId)
        val currentTaskId = currentTech?.currentTaskId
        val currentTaskTitle = currentTech?.currentTaskTitle

        technicianDao.updateTechStatus(techId, newStatus, currentTaskId, currentTaskTitle)

        auditDao.insertLog(
            AuditLogEntity(
                eventCategory = "GEO_TRACKING",
                actor = "$techName ($techId)",
                summary = "Technician Status Changed: ${newStatus.label}",
                details = "Manual status radio beacon updated to ${newStatus.label}.",
                isSecurityAlert = newStatus == TechStatus.EMERGENCY_ASSIST
            )
        )
    }

    suspend fun toggleChecklistItem(taskId: String, itemIndex: Int) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(taskId) ?: return@withContext
        val items = task.checklistJson.split(";").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (itemIndex in items.indices) {
            val parts = items[itemIndex].split("::")
            if (parts.size == 2) {
                val title = parts[0]
                val currentDone = parts[1].toBoolean()
                items[itemIndex] = "$title::${!currentDone}"
                val newChecklist = items.joinToString(";")
                taskDao.updateChecklist(taskId, newChecklist)
            }
        }
    }

    suspend fun completeTask(taskId: String, resolutionNotes: String, signatureName: String, techId: String, techName: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        taskDao.completeTask(taskId, TaskStatus.COMPLETED, resolutionNotes, signatureName, completedAt = now)
        technicianDao.updateTechStatus(techId, TechStatus.AVAILABLE, taskId = null, taskTitle = null)

        auditDao.insertLog(
            AuditLogEntity(
                eventCategory = "DISPATCH_LIFECYCLE",
                actor = "$techName ($techId)",
                summary = "Task Completed & Signed: $taskId",
                details = "Work order completed with digital signoff by $signatureName. Notes: $resolutionNotes",
                isSecurityAlert = false
            )
        )
    }

    suspend fun dispatchTaskToTech(taskId: String, techId: String, techName: String, dispatcherName: String) = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(taskId) ?: return@withContext
        taskDao.dispatchTask(taskId, techId, techName, TaskStatus.DISPATCHED)

        auditDao.insertLog(
            AuditLogEntity(
                eventCategory = "DISPATCH_LIFECYCLE",
                actor = dispatcherName,
                summary = "Task Dispatched to $techName",
                details = "Dispatcher assigned task '${task.title}' to technician $techName ($techId).",
                isSecurityAlert = false
            )
        )
    }

    suspend fun createNewTask(
        title: String,
        description: String,
        assetTag: String,
        equipmentCategory: String,
        siteName: String,
        address: String,
        latitude: Double,
        longitude: Double,
        priority: TaskPriority,
        hazardAlert: String?,
        requiredTools: String,
        slaDeadline: String,
        assignedTechId: String?,
        assignedTechName: String?,
        actorName: String
    ) = withContext(Dispatchers.IO) {
        val newId = "TASK-${(100..999).random()}"
        val initialStatus = if (assignedTechId != null) TaskStatus.DISPATCHED else TaskStatus.PENDING_DISPATCH
        val defaultChecklist = "Verify equipment tag and safe isolation::false;Perform hazard risk assessment::false;Execute service procedure per OEM specs::false;Perform functional test & calibration::false;Restore site to clean operational state::false"

        val task = TaskEntity(
            id = newId,
            title = title,
            description = description,
            assetTag = assetTag,
            equipmentCategory = equipmentCategory,
            siteName = siteName,
            address = address,
            latitude = latitude,
            longitude = longitude,
            priority = priority,
            status = initialStatus,
            assignedTechId = assignedTechId,
            assignedTechName = assignedTechName,
            slaDeadline = slaDeadline,
            hazardAlert = hazardAlert,
            requiredTools = requiredTools,
            checklistJson = defaultChecklist,
            createdAt = System.currentTimeMillis()
        )
        taskDao.insertTask(task)

        auditDao.insertLog(
            AuditLogEntity(
                eventCategory = "DISPATCH_LIFECYCLE",
                actor = actorName,
                summary = "New Work Order Created: $newId",
                details = "Priority: ${priority.label} for $equipmentCategory at $siteName.",
                isSecurityAlert = priority == TaskPriority.P1_CRITICAL
            )
        )
    }

    // Live GPS telemetry simulation step (moves En Route technicians towards their task)
    suspend fun simulateTelemetryPulse() = withContext(Dispatchers.IO) {
        val techs = technicianDao.getAllTechniciansFlow().first()
        val tasks = taskDao.getAllTasksFlow().first().associateBy { it.id }

        techs.forEach { tech ->
            if (tech.status == TechStatus.EN_ROUTE && tech.currentTaskId != null) {
                val targetTask = tasks[tech.currentTaskId]
                if (targetTask != null) {
                    val dLat = targetTask.latitude - tech.latitude
                    val dLng = targetTask.longitude - tech.longitude
                    val distance = Math.sqrt(dLat * dLat + dLng * dLng)

                    if (distance > 0.001) {
                        val step = 0.0006 // moving increment
                        val newLat = tech.latitude + (dLat / distance) * step
                        val newLng = tech.longitude + (dLng / distance) * step
                        val heading = (Math.toDegrees(Math.atan2(dLng, dLat)).toFloat() + 360) % 360
                        technicianDao.updateTechLocation(tech.id, newLat, newLng, heading, speed = 42f)
                    } else {
                        // Arrived on site!
                        technicianDao.updateTechStatus(tech.id, TechStatus.ON_SITE, tech.currentTaskId, tech.currentTaskTitle)
                        taskDao.updateTaskStatus(targetTask.id, TaskStatus.ON_SITE_WORKING)
                    }
                }
            }
        }
    }

    suspend fun updateTechnicianCoordinates(
        techId: String,
        latitude: Double,
        longitude: Double,
        headingDegrees: Float = 0f,
        speedKmh: Float = 0f
    ) = withContext(Dispatchers.IO) {
        technicianDao.updateTechLocation(
            techId = techId,
            lat = latitude,
            lng = longitude,
            heading = headingDegrees,
            speed = speedKmh
        )
    }

    suspend fun updateWorkOrderCoordinates(
        taskId: String,
        latitude: Double,
        longitude: Double
    ) = withContext(Dispatchers.IO) {
        taskDao.updateTaskLocation(
            taskId = taskId,
            lat = latitude,
            lng = longitude
        )
    }
}
