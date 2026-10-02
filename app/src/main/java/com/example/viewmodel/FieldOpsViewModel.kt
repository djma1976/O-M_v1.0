package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLogEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TechStatus
import com.example.data.model.TechnicianEntity
import com.example.data.repository.FieldOpsRepository
import com.example.data.ui.components.MapInspectorState
import com.example.data.ui.components.MapJavaScriptInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FieldOpsViewModel(
    private val repository: FieldOpsRepository
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val technicians: StateFlow<List<TechnicianEntity>> = repository.allTechnicians
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.recentAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTaskId = MutableStateFlow<String?>(null)
    val selectedTaskId: StateFlow<String?> = _selectedTaskId.asStateFlow()

    private val _selectedTechId = MutableStateFlow<String?>(null)
    val selectedTechId: StateFlow<String?> = _selectedTechId.asStateFlow()

    private val _inspectionTask = MutableStateFlow<TaskEntity?>(null)
    val inspectionTask: StateFlow<TaskEntity?> = _inspectionTask.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val isFleetSimulationActive: StateFlow<Boolean> = repository.statusUpdateService.isSimulationActive
    val lastStatusEvent: StateFlow<String?> = repository.statusUpdateService.lastUpdateEvent

    /**
     * Reactive inspector card state that updates automatically whenever `onMarkerTap` selects
     * a technician or task marker on the map, or when the selected item's live telemetry changes.
     */
    val activeMapInspector: StateFlow<MapInspectorState> = combine(
        technicians,
        tasks,
        _selectedTechId,
        _selectedTaskId
    ) { techList, taskList, selTechId, selTaskId ->
        val matchedTech = if (!selTechId.isNullOrBlank()) {
            techList.firstOrNull { it.id == selTechId }
        } else null

        val matchedTask = if (!selTaskId.isNullOrBlank()) {
            taskList.firstOrNull { it.id == selTaskId }
        } else null

        when {
            matchedTech != null -> MapInspectorState.TechnicianInspector(matchedTech)
            matchedTask != null -> MapInspectorState.TaskInspector(matchedTask)
            else -> MapInspectorState.None
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapInspectorState.None
    )

    /**
     * Reactive JSON stream of technician coordinates, work order coordinates, and active dispatch
     * routes emitted dynamically whenever the ViewModel state updates.
     */
    val mapCoordinatesPayload: StateFlow<String> = combine(
        technicians,
        tasks,
        _selectedTechId,
        _selectedTaskId
    ) { techList, taskList, selTechId, selTaskId ->
        MapJavaScriptInterface.buildMapPayloadJson(
            filteredTechs = techList,
            filteredTasks = taskList,
            allTechs = techList,
            allTasks = taskList,
            selectedTechId = selTechId,
            selectedTaskId = selTaskId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = """{"selectedTechId":"","selectedTaskId":"","technicians":[],"workOrders":[],"routes":[]}"""
    )

    init {
        // Continuous Telemetry Movement simulation for en-route vehicles
        viewModelScope.launch {
            while (true) {
                delay(4000)
                try {
                    repository.simulateTelemetryPulse()
                } catch (e: Exception) {
                    // Ignore transient errors
                }
            }
        }
    }

    /**
     * Handles `onMarkerTap` events forwarded from the Leaflet WebView `JavascriptInterface`,
     * reactively updating `_selectedTechId`, `_selectedTaskId`, and [activeMapInspector]
     * so the bottom inspector card immediately reflects the tapped technician or task marker.
     */
    fun onMarkerTap(markerType: String, markerId: String) {
        val normalizedType = markerType.trim().uppercase()
        val cleanId = markerId.trim()
        if (cleanId.isEmpty()) {
            clearMapSelection()
            return
        }

        val isTech = when (normalizedType) {
            "TECHNICIAN", "TECH" -> true
            "TASK", "WORK_ORDER", "WO" -> false
            else -> technicians.value.any { it.id.equals(cleanId, ignoreCase = true) }
        }

        if (isTech) {
            val foundTech = technicians.value.firstOrNull { it.id.equals(cleanId, ignoreCase = true) }
            _selectedTechId.value = foundTech?.id ?: cleanId
            _selectedTaskId.value = null
        } else {
            val foundTask = tasks.value.firstOrNull { it.id.equals(cleanId, ignoreCase = true) }
            _selectedTaskId.value = foundTask?.id ?: cleanId
            _selectedTechId.value = null
            if (foundTask != null) {
                _inspectionTask.value = foundTask
            }
        }
    }

    fun clearMapSelection() {
        _selectedTechId.value = null
        _selectedTaskId.value = null
    }

    fun selectTask(task: TaskEntity?) {
        _selectedTaskId.value = task?.id
        if (task != null) {
            _selectedTechId.value = null
        }
        _inspectionTask.value = task
    }

    fun selectTech(tech: TechnicianEntity?) {
        _selectedTechId.value = tech?.id
        if (tech != null) {
            _selectedTaskId.value = null
        }
    }

    fun acceptTask(taskId: String, techId: String, techName: String) {
        viewModelScope.launch {
            repository.acceptTask(taskId, techId, techName)
            _snackbarMessage.value = "Work Order $taskId accepted! Route initiated."
        }
    }

    fun declineTask(taskId: String, techId: String, techName: String, reason: String) {
        viewModelScope.launch {
            repository.declineTask(taskId, techId, techName, reason)
            _snackbarMessage.value = "Task declined. Dispatch desk notified."
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: TaskStatus, techId: String, techName: String) {
        viewModelScope.launch {
            repository.updateTaskStatus(taskId, newStatus, techId, techName)
            _snackbarMessage.value = "Status updated to ${newStatus.label}"
        }
    }

    fun updateTechStatus(techId: String, status: TechStatus, techName: String) {
        viewModelScope.launch {
            repository.statusUpdateService.updateAvailability(techId, status, techName)
            _snackbarMessage.value = "Your status is now ${status.label}"
        }
    }

    fun updateTechnicianCoordinates(
        techId: String,
        latitude: Double,
        longitude: Double,
        headingDegrees: Float = 0f,
        speedKmh: Float = 0f
    ) {
        viewModelScope.launch {
            repository.updateTechnicianCoordinates(
                techId = techId,
                latitude = latitude,
                longitude = longitude,
                headingDegrees = headingDegrees,
                speedKmh = speedKmh
            )
            _snackbarMessage.value = "Updated coordinates for $techId (${"%.4f".format(latitude)}, ${"%.4f".format(longitude)})"
        }
    }

    fun updateWorkOrderCoordinates(
        taskId: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            repository.updateWorkOrderCoordinates(
                taskId = taskId,
                latitude = latitude,
                longitude = longitude
            )
            _snackbarMessage.value = "Updated work order $taskId coordinates (${"%.4f".format(latitude)}, ${"%.4f".format(longitude)})"
        }
    }

    fun toggleFleetSimulation() {
        repository.statusUpdateService.toggleSimulation()
        val running = repository.statusUpdateService.isSimulationActive.value
        _snackbarMessage.value = if (running) "Fleet availability simulation active" else "Fleet availability simulation paused"
    }

    fun triggerSimulatedShift() {
        viewModelScope.launch {
            repository.statusUpdateService.triggerInstantShift()
            _snackbarMessage.value = "Simulated status shift triggered"
        }
    }

    fun cycleTechnicianAvailability(techId: String, actor: String) {
        viewModelScope.launch {
            repository.statusUpdateService.cycleTechnicianState(techId, actor)
        }
    }

    fun toggleChecklistItem(taskId: String, index: Int) {
        viewModelScope.launch {
            repository.toggleChecklistItem(taskId, index)
        }
    }

    fun completeTask(taskId: String, notes: String, signature: String, techId: String, techName: String) {
        viewModelScope.launch {
            repository.completeTask(taskId, notes, signature, techId, techName)
            _inspectionTask.value = null
            _snackbarMessage.value = "Work Order $taskId successfully signed and closed!"
        }
    }

    fun dispatchTask(taskId: String, techId: String, techName: String, dispatcherName: String) {
        viewModelScope.launch {
            repository.dispatchTaskToTech(taskId, techId, techName, dispatcherName)
            _snackbarMessage.value = "Task dispatched to $techName"
        }
    }

    fun createWorkOrder(
        title: String,
        description: String,
        assetTag: String,
        category: String,
        siteName: String,
        address: String,
        lat: Double,
        lng: Double,
        priority: TaskPriority,
        hazard: String?,
        tools: String,
        sla: String,
        techId: String?,
        techName: String?,
        actor: String
    ) {
        viewModelScope.launch {
            repository.createNewTask(
                title, description, assetTag, category, siteName, address,
                lat, lng, priority, hazard, tools, sla, techId, techName, actor
            )
            _snackbarMessage.value = "New Work Order dispatched to queue."
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}

class FieldOpsViewModelFactory(private val repository: FieldOpsRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FieldOpsViewModel(repository) as T
    }
}
