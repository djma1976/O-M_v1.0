package com.example.data.service

import com.example.data.local.AuditDao
import com.example.data.local.TechnicianDao
import com.example.data.model.AuditLogEntity
import com.example.data.model.TechStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service that manages and reflects real-time technician availability on the map dashboard,
 * with mock state management for 'Available', 'On-Site', and 'Offline' states.
 */
class TechnicianStatusUpdateService(
    private val technicianDao: TechnicianDao,
    private val auditDao: AuditDao,
    private val externalScope: CoroutineScope
) {
    private val _isSimulationActive = MutableStateFlow(true)
    val isSimulationActive: StateFlow<Boolean> = _isSimulationActive.asStateFlow()

    private val _lastUpdateEvent = MutableStateFlow<String?>(null)
    val lastUpdateEvent: StateFlow<String?> = _lastUpdateEvent.asStateFlow()

    private var simulationJob: Job? = null

    init {
        startMockStateSimulation()
    }

    /**
     * Start the background mock state management simulation loop that cycles technician
     * availability across 'Available', 'On-Site', and 'Offline' states to simulate real-world fleet shifts.
     */
    fun startMockStateSimulation() {
        _isSimulationActive.value = true
        simulationJob?.cancel()
        simulationJob = externalScope.launch(Dispatchers.IO) {
            while (isActive && _isSimulationActive.value) {
                // Heartbeat interval for simulated shifts (every 7 seconds)
                delay(7000)
                performSimulatedAvailabilityShift()
            }
        }
    }

    /**
     * Pause or resume the automatic mock state management simulation.
     */
    fun toggleSimulation() {
        if (_isSimulationActive.value) {
            stopMockStateSimulation()
        } else {
            startMockStateSimulation()
        }
    }

    fun stopMockStateSimulation() {
        _isSimulationActive.value = false
        simulationJob?.cancel()
        simulationJob = null
    }

    /**
     * Manually updates a technician's availability status.
     * Reflects immediately in the Room database and logs an audit trail event.
     */
    suspend fun updateAvailability(
        techId: String,
        newStatus: TechStatus,
        actorName: String = "Dispatcher",
        notes: String? = null
    ) = withContext(Dispatchers.IO) {
        val tech = technicianDao.getTechnicianById(techId) ?: return@withContext
        val oldStatus = tech.status

        technicianDao.updateTechStatus(
            techId = techId,
            status = newStatus,
            taskId = if (newStatus == TechStatus.OFFLINE) null else tech.currentTaskId,
            taskTitle = if (newStatus == TechStatus.OFFLINE) null else tech.currentTaskTitle,
            timestamp = System.currentTimeMillis()
        )

        val logSummary = "Technician ${tech.name} status updated to ${newStatus.label}"
        _lastUpdateEvent.value = logSummary

        auditDao.insertLog(
            AuditLogEntity(
                eventCategory = "TECH_AVAILABILITY",
                actor = actorName,
                summary = logSummary,
                details = notes ?: "Availability shifted from ${oldStatus.label} to ${newStatus.label}.",
                isSecurityAlert = newStatus == TechStatus.OFFLINE
            )
        )
    }

    /**
     * Quick 1-tap state cycler: AVAILABLE -> ON_SITE -> OFFLINE -> AVAILABLE.
     */
    suspend fun cycleTechnicianState(techId: String, actorName: String) = withContext(Dispatchers.IO) {
        val tech = technicianDao.getTechnicianById(techId) ?: return@withContext
        val nextState = when (tech.status) {
            TechStatus.AVAILABLE -> TechStatus.ON_SITE
            TechStatus.ON_SITE -> TechStatus.OFFLINE
            TechStatus.OFFLINE, TechStatus.OFF_DUTY -> TechStatus.AVAILABLE
            else -> TechStatus.AVAILABLE
        }
        updateAvailability(techId, nextState, actorName, "Manual availability cycle via Map Dashboard.")
    }

    /**
     * Forces an instant simulated shift for a random technician to demonstrate real-time map updates.
     */
    suspend fun triggerInstantShift() = withContext(Dispatchers.IO) {
        performSimulatedAvailabilityShift()
    }

    private suspend fun performSimulatedAvailabilityShift() {
        try {
            val techs = technicianDao.getAllTechniciansFlow().first()
            if (techs.isEmpty()) return

            // Choose a candidate technician to transition (skip primary user USR-001 if desired, or pick from others)
            val candidate = techs.filter { it.id != "USR-001" }.randomOrNull() ?: techs.random()
            val targetStatus = when (candidate.status) {
                TechStatus.AVAILABLE -> if (Math.random() > 0.4) TechStatus.ON_SITE else TechStatus.OFFLINE
                TechStatus.ON_SITE -> if (Math.random() > 0.5) TechStatus.AVAILABLE else TechStatus.OFFLINE
                TechStatus.OFFLINE, TechStatus.OFF_DUTY -> TechStatus.AVAILABLE
                TechStatus.EN_ROUTE -> TechStatus.ON_SITE
                else -> TechStatus.AVAILABLE
            }

            technicianDao.updateTechStatus(
                techId = candidate.id,
                status = targetStatus,
                taskId = if (targetStatus == TechStatus.OFFLINE) null else candidate.currentTaskId,
                taskTitle = if (targetStatus == TechStatus.OFFLINE) null else candidate.currentTaskTitle,
                timestamp = System.currentTimeMillis()
            )

            val event = "${candidate.name} is now ${targetStatus.label}"
            _lastUpdateEvent.value = event

            auditDao.insertLog(
                AuditLogEntity(
                    eventCategory = "GEO_TRACKING",
                    actor = "Radio Beacon System",
                    summary = "Real-Time Telemetry: $event",
                    details = "Automated radio keep-alive ping reflected on live map dashboard.",
                    isSecurityAlert = targetStatus == TechStatus.OFFLINE
                )
            )
        } catch (e: Exception) {
            // Ignore transient simulation errors
        }
    }
}
