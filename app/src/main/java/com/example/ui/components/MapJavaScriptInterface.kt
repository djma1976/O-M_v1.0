package com.example.data.ui.components

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.data.model.TaskEntity
import com.example.data.model.TaskStatus
import com.example.data.model.TechnicianEntity
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Represents the reactive state of the bottom inspector card on the operations map,
 * updated whenever `onMarkerTap` is invoked from the Leaflet WebView.
 */
sealed class MapInspectorState {
    data object None : MapInspectorState()
    data class TechnicianInspector(val technician: TechnicianEntity) : MapInspectorState()
    data class TaskInspector(val task: TaskEntity) : MapInspectorState()
}

/**
 * Android [JavascriptInterface] bridge (`window.AndroidMapBridge`) that connects the
 * Leaflet [WebView] map to Kotlin and [com.example.viewmodel.FieldOpsViewModel].
 *
 * Supports:
 * 1. **Dynamic Kotlin/ViewModel → JavaScript Coordinate Push**:
 *    Pushes live technician and work order coordinates from `FieldOpsViewModel` StateFlows
 *    directly into the WebView JavaScript runtime via `evaluateJavascript`.
 * 2. **Dynamic JavaScript → Kotlin Coordinate Pull**:
 *    Exposes `@JavascriptInterface` getters ([getMapPayloadJson], [getTechniciansCoordinatesJson],
 *    [getWorkOrdersCoordinatesJson]) so the JavaScript runtime can pull the latest coordinates on demand.
 * 3. **Interactive JavaScript → ViewModel Callbacks (`onMarkerTap`)**:
 *    Handles marker tap selection (`onMarkerTap`), coordinate drag updates, and base-layer changes
 *    on the Android main thread so the ViewModel reactively updates the bottom inspector card.
 */
class MapJavaScriptInterface(
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val onMarkerTapCallback: ((markerType: String, markerId: String) -> Unit)? = null,
    private val onTechSelected: (String) -> Unit = {},
    private val onTaskSelected: (String) -> Unit = {},
    private val onTechCoordinateUpdated: ((techId: String, lat: Double, lng: Double) -> Unit)? = null,
    private val onTaskCoordinateUpdated: ((taskId: String, lat: Double, lng: Double) -> Unit)? = null,
    private val onMapCleared: () -> Unit = {},
    private val onBaseLayerChanged: (String) -> Unit = {},
    private val onMapInitialized: () -> Unit = {},
    private val onGestureStateChangedCallback: (Boolean) -> Unit = {},
    private val onViewportSettledCallback: ((centerLat: Double, centerLng: Double, zoom: Int) -> Unit)? = null
) {
    @Volatile
    private var webViewRef: WebView? = null

    @Volatile
    private var isLeafletReady: Boolean = false

    @Volatile
    private var isGestureActive: Boolean = false

    @Volatile
    private var hasPendingPayloadDuringGesture: Boolean = false

    @Volatile
    private var lastPushedSelectedTechId: String? = null

    @Volatile
    private var lastPushedSelectedTaskId: String? = null

    @Volatile
    private var pendingZoomDelta: Int = 0

    @Volatile
    private var latestMapPayloadJson: String = """{"selectedTechId":"","selectedTaskId":"","technicians":[],"workOrders":[],"routes":[]}"""

    @Volatile
    private var latestTechniciansJson: String = "[]"

    @Volatile
    private var latestWorkOrdersJson: String = "[]"

    @Volatile
    private var mobileLatitude: Double? = null

    @Volatile
    private var mobileLongitude: Double? = null

    private val debouncedPayloadPushRunnable = Runnable {
        if (!isGestureActive && isLeafletReady) {
            executeImmediateMapCoordinatesPush(latestMapPayloadJson)
        } else {
            hasPendingPayloadDuringGesture = true
        }
    }

    private val debouncedZoomRunnable = Runnable {
        val delta = pendingZoomDelta
        pendingZoomDelta = 0
        if (delta != 0) {
            evaluateJsOnMainThread("window.leafletZoomByDelta && window.leafletZoomByDelta($delta);")
        }
    }

    /**
     * Binds or unbinds the active [WebView] instance to this bridge.
     */
    fun attachWebView(webView: WebView?) {
        if (webView == null) {
            mainHandler.removeCallbacks(debouncedPayloadPushRunnable)
            mainHandler.removeCallbacks(debouncedZoomRunnable)
        }
        webViewRef = webView
    }

    // =========================================================================
    // 1. JS -> Kotlin (@JavascriptInterface) Pull Methods for Coordinates
    // =========================================================================

    /**
     * Called from JavaScript (`window.AndroidMapBridge.getMapPayloadJson()`) to pull
     * the full JSON snapshot of technicians, work orders, and active dispatch routes.
     */
    @JavascriptInterface
    fun getMapPayloadJson(): String = latestMapPayloadJson

    /**
     * Called from JavaScript (`window.AndroidMapBridge.getTechniciansCoordinatesJson()`)
     * to pull the latest technician coordinates array from the ViewModel state.
     */
    @JavascriptInterface
    fun getTechniciansCoordinatesJson(): String = latestTechniciansJson

    /**
     * Called from JavaScript (`window.AndroidMapBridge.getWorkOrdersCoordinatesJson()`)
     * to pull the latest work order coordinates array from the ViewModel state.
     */
    @JavascriptInterface
    fun getWorkOrdersCoordinatesJson(): String = latestWorkOrdersJson

    /**
     * Called from JavaScript (`window.AndroidMapBridge.getMobilePositionJson()`)
     * to pull the current mobile device or user technician coordinates.
     */
    @JavascriptInterface
    fun getMobilePositionJson(): String {
        val lat = mobileLatitude
        val lng = mobileLongitude
        return if (lat != null && lng != null) {
            """{"lat":$lat,"lng":$lng}"""
        } else {
            ""
        }
    }

    // =========================================================================
    // 2. JS -> Kotlin (@JavascriptInterface) Event & Coordinate Callbacks
    // =========================================================================

    /**
     * Invoked by the JavaScript runtime once the Leaflet map instance and tile layers
     * are initialized. Automatically pushes the current ViewModel coordinates to JS.
     */
    @JavascriptInterface
    fun onLeafletReady() {
        isLeafletReady = true
        mainHandler.post {
            onMapInitialized()
            pushMapCoordinatesToJs(latestMapPayloadJson)
        }
    }

    /**
     * Unified marker tap callback invoked from the Leaflet WebView JavaScript runtime
     * (`window.AndroidMapBridge.onMarkerTap(markerType, markerId)`) whenever a technician
     * or work order (task) marker is tapped on the map surface.
     *
     * @param markerType `"TECHNICIAN"` or `"TASK"` (or `"WORK_ORDER"`)
     * @param markerId The unique ID of the selected technician or task (e.g., `"USR-001"`, `"TASK-101"`)
     */
    @JavascriptInterface
    fun onMarkerTap(markerType: String, markerId: String) {
        val normalizedType = markerType.trim().uppercase()
        val trimmedId = markerId.trim()
        mainHandler.post {
            onMarkerTapCallback?.invoke(normalizedType, trimmedId)
            when (normalizedType) {
                "TECHNICIAN", "TECH" -> onTechSelected(trimmedId)
                "TASK", "WORK_ORDER", "WO" -> onTaskSelected(trimmedId)
                else -> {
                    if (trimmedId.startsWith("TASK", ignoreCase = true) || trimmedId.startsWith("WO", ignoreCase = true)) {
                        onTaskSelected(trimmedId)
                    } else {
                        onTechSelected(trimmedId)
                    }
                }
            }
        }
    }

    /**
     * Single-argument overload (`window.AndroidMapBridge.onMarkerTap(markerId)`) that
     * automatically infers whether the tapped marker is a task or technician and notifies
     * the ViewModel to reactively update the inspector card.
     */
    @JavascriptInterface
    fun onMarkerTap(markerId: String) {
        val trimmed = markerId.trim()
        if (trimmed.contains(":")) {
            val parts = trimmed.split(":", limit = 2)
            onMarkerTap(parts[0], parts[1])
        } else {
            val inferredType = if (trimmed.startsWith("TASK", ignoreCase = true) || trimmed.startsWith("WO", ignoreCase = true)) {
                "TASK"
            } else {
                "TECHNICIAN"
            }
            onMarkerTap(inferredType, trimmed)
        }
    }

    /**
     * Invoked when a technician marker is tapped on the Leaflet map surface.
     * Delegates to [onMarkerTap] so the ViewModel reactively updates the inspector card.
     */
    @JavascriptInterface
    fun onTechnicianMarkerClick(techId: String) {
        onMarkerTap("TECHNICIAN", techId)
    }

    /**
     * Invoked when a work order marker is tapped on the Leaflet map surface.
     * Delegates to [onMarkerTap] so the ViewModel reactively updates the inspector card.
     */
    @JavascriptInterface
    fun onWorkOrderMarkerClick(taskId: String) {
        onMarkerTap("TASK", taskId)
    }

    /**
     * Invoked when a technician marker's coordinates are updated/dragged in JavaScript,
     * forwarding the new `(lat, lng)` coordinates back to the ViewModel.
     */
    @JavascriptInterface
    fun onTechnicianCoordinateChanged(techId: String, lat: Double, lng: Double) {
        mainHandler.post {
            onTechCoordinateUpdated?.invoke(techId, lat, lng)
        }
    }

    /**
     * Invoked when a work order marker's coordinates are updated/dragged in JavaScript,
     * forwarding the new `(lat, lng)` coordinates back to the ViewModel.
     */
    @JavascriptInterface
    fun onWorkOrderCoordinateChanged(taskId: String, lat: Double, lng: Double) {
        mainHandler.post {
            onTaskCoordinateUpdated?.invoke(taskId, lat, lng)
        }
    }

    /**
     * Invoked when the user clicks an empty area on the map surface to dismiss selection cards.
     */
    @JavascriptInterface
    fun onMapSurfaceClick() {
        mainHandler.post { onMapCleared() }
    }

    /**
     * Invoked when the user switches between `googleStreets` and `googleHybrid` in the Leaflet control.
     */
    @JavascriptInterface
    fun onBaseMapSelected(layerKey: String) {
        mainHandler.post { onBaseLayerChanged(layerKey) }
    }

    /**
     * Invoked from JavaScript (`window.AndroidMapBridge.onGestureStateChanged(isInteracting)`)
     * when a touch pan or pinch-zoom gesture starts (`true`) or finishes after debounce (`false`).
     * Defers non-urgent marker DOM rebuilds while the user is actively moving the map on low-end hardware.
     */
    @JavascriptInterface
    fun onGestureStateChanged(isInteracting: Boolean) {
        isGestureActive = isInteracting
        mainHandler.post {
            onGestureStateChangedCallback(isInteracting)
            if (!isInteracting && hasPendingPayloadDuringGesture && isLeafletReady) {
                hasPendingPayloadDuringGesture = false
                scheduleDebouncedMapCoordinatePush(delayMs = 60L)
            }
        }
    }

    /**
     * Invoked from JavaScript (`window.AndroidMapBridge.onPanZoomSettled(lat, lng, zoom)`)
     * once a pan or zoom gesture has settled after the debounce window.
     */
    @JavascriptInterface
    fun onPanZoomSettled(centerLat: Double, centerLng: Double, zoom: Int) {
        isGestureActive = false
        mainHandler.post {
            onGestureStateChangedCallback(false)
            onViewportSettledCallback?.invoke(centerLat, centerLng, zoom)
            if (hasPendingPayloadDuringGesture && isLeafletReady) {
                hasPendingPayloadDuringGesture = false
                executeImmediateMapCoordinatesPush(latestMapPayloadJson)
            }
        }
    }

    /**
     * Updates the gesture active state from Android touch events (`ACTION_DOWN` / `ACTION_UP`)
     * to coordinate touch debouncing between the native WebView container and Leaflet JS.
     */
    fun notifyNativeTouchGestureActive(active: Boolean) {
        if (active) {
            isGestureActive = true
            mainHandler.removeCallbacks(debouncedPayloadPushRunnable)
        } else if (isGestureActive) {
            // Allow JS moveend/zoomend debounce to settle, with a safety fallback release
            mainHandler.removeCallbacks(debouncedPayloadPushRunnable)
            mainHandler.postDelayed({
                if (isGestureActive) {
                    isGestureActive = false
                    onGestureStateChangedCallback(false)
                    if (hasPendingPayloadDuringGesture && isLeafletReady) {
                        hasPendingPayloadDuringGesture = false
                        executeImmediateMapCoordinatesPush(latestMapPayloadJson)
                    }
                }
            }, GESTURE_SETTLE_DEBOUNCE_MS + 80L)
        }
    }

    // =========================================================================
    // 3. Kotlin / ViewModel -> JS Dynamic Coordinate Push Methods
    // =========================================================================

    /**
     * Synchronizes the latest technician and work order lists from the ViewModel,
     * updates the cached JSON snapshots, and dynamically pushes the coordinates to
     * the WebView JavaScript runtime with pan/zoom gesture debouncing.
     */
    fun syncCoordinatesFromViewModel(
        filteredTechs: List<TechnicianEntity>,
        filteredTasks: List<TaskEntity>,
        allTechs: List<TechnicianEntity>,
        allTasks: List<TaskEntity>,
        selectedTechId: String?,
        selectedTaskId: String?
    ) {
        val techsArrayJson = buildTechniciansJsonArray(filteredTechs).toString()
        val tasksArrayJson = buildWorkOrdersJsonArray(filteredTasks).toString()
        val fullPayloadJson = buildMapPayloadJson(
            filteredTechs = filteredTechs,
            filteredTasks = filteredTasks,
            allTechs = allTechs,
            allTasks = allTasks,
            selectedTechId = selectedTechId,
            selectedTaskId = selectedTaskId
        )

        val selectionChanged =
            selectedTechId != lastPushedSelectedTechId || selectedTaskId != lastPushedSelectedTaskId
        lastPushedSelectedTechId = selectedTechId
        lastPushedSelectedTaskId = selectedTaskId

        latestTechniciansJson = techsArrayJson
        latestWorkOrdersJson = tasksArrayJson
        latestMapPayloadJson = fullPayloadJson

        if (isLeafletReady) {
            if (selectionChanged) {
                // Marker selection tap should highlight immediately
                pushMapCoordinatesToJs(fullPayloadJson, immediate = true)
            } else if (isGestureActive) {
                // Defer background telemetry DOM rebuilds while user is panning/zooming
                hasPendingPayloadDuringGesture = true
            } else {
                scheduleDebouncedMapCoordinatePush(delayMs = PAYLOAD_SYNC_DEBOUNCE_MS)
            }
        }
    }

    /**
     * Schedules a debounced coordinate push so rapid state updates coalesce into a single
     * WebView `evaluateJavascript` call on low-end hardware.
     */
    private fun scheduleDebouncedMapCoordinatePush(delayMs: Long = PAYLOAD_SYNC_DEBOUNCE_MS) {
        mainHandler.removeCallbacks(debouncedPayloadPushRunnable)
        mainHandler.postDelayed(debouncedPayloadPushRunnable, delayMs)
    }

    /**
     * Pushes a serialized JSON payload of technician and work order coordinates directly
     * into `window.renderMarkers(...)` inside the WebView JavaScript runtime.
     */
    fun pushMapCoordinatesToJs(
        payloadJson: String = latestMapPayloadJson,
        immediate: Boolean = false
    ) {
        latestMapPayloadJson = payloadJson
        if (!immediate && isGestureActive) {
            hasPendingPayloadDuringGesture = true
            return
        }
        mainHandler.removeCallbacks(debouncedPayloadPushRunnable)
        executeImmediateMapCoordinatesPush(payloadJson)
    }

    private fun executeImmediateMapCoordinatesPush(payloadJson: String) {
        val escapedJson = escapeForJsSingleQuote(payloadJson)
        evaluateJsOnMainThread("window.renderMarkers && window.renderMarkers('$escapedJson');")
    }

    /**
     * Dynamically updates a single technician's marker coordinates in the JavaScript runtime
     * without rebuilding all markers (`window.updateTechnicianMarkerCoordinate`).
     */
    fun pushSingleTechnicianCoordinateToJs(
        techId: String,
        latitude: Double,
        longitude: Double,
        status: String? = null
    ) {
        val safeId = escapeForJsSingleQuote(techId)
        val safeStatus = escapeForJsSingleQuote(status ?: "")
        evaluateJsOnMainThread(
            "window.updateTechnicianMarkerCoordinate && window.updateTechnicianMarkerCoordinate('$safeId', $latitude, $longitude, '$safeStatus');"
        )
    }

    /**
     * Dynamically updates a single work order's marker coordinates in the JavaScript runtime
     * without rebuilding all markers (`window.updateWorkOrderMarkerCoordinate`).
     */
    fun pushSingleWorkOrderCoordinateToJs(
        taskId: String,
        latitude: Double,
        longitude: Double
    ) {
        val safeId = escapeForJsSingleQuote(taskId)
        evaluateJsOnMainThread(
            "window.updateWorkOrderMarkerCoordinate && window.updateWorkOrderMarkerCoordinate('$safeId', $latitude, $longitude);"
        )
    }

    /**
     * Pans/flies the Leaflet map camera to a specific `(latitude, longitude)` coordinate.
     */
    fun flyToCoordinatesInJs(latitude: Double, longitude: Double, zoom: Int = 15) {
        evaluateJsOnMainThread(
            "window.leafletFlyToCoordinate && window.leafletFlyToCoordinate($latitude, $longitude, $zoom);"
        )
    }

    /**
     * Centers the Leaflet map directly on the mobile device's position with high-precision animation.
     */
    fun centerOnMobilePositionInJs(latitude: Double, longitude: Double, zoom: Int = 16) {
        mobileLatitude = latitude
        mobileLongitude = longitude
        evaluateJsOnMainThread(
            "window.centerOnMobilePosition && window.centerOnMobilePosition($latitude, $longitude, $zoom);"
        )
    }

    /**
     * Updates the mobile device's live marker position on the Leaflet map surface.
     */
    fun setMobilePosition(latitude: Double, longitude: Double) {
        mobileLatitude = latitude
        mobileLongitude = longitude
        evaluateJsOnMainThread(
            "window.updateMobileLocationMarker && window.updateMobileLocationMarker($latitude, $longitude);"
        )
    }

    /**
     * Switches the active Leaflet base tile layer (`googleStreets` or `googleHybrid`).
     */
    fun setBaseLayerInJs(layerKey: String) {
        val safeKey = escapeForJsSingleQuote(layerKey)
        evaluateJsOnMainThread("window.setBaseMapLayer && window.setBaseMapLayer('$safeKey');")
    }

    /**
     * Debounces rapid Zoom-In button taps on low-end hardware into a single coalesced zoom step.
     */
    fun zoomInInJs() {
        pendingZoomDelta += 1
        mainHandler.removeCallbacks(debouncedZoomRunnable)
        mainHandler.postDelayed(debouncedZoomRunnable, ZOOM_BUTTON_DEBOUNCE_MS)
    }

    /**
     * Debounces rapid Zoom-Out button taps on low-end hardware into a single coalesced zoom step.
     */
    fun zoomOutInJs() {
        pendingZoomDelta -= 1
        mainHandler.removeCallbacks(debouncedZoomRunnable)
        mainHandler.postDelayed(debouncedZoomRunnable, ZOOM_BUTTON_DEBOUNCE_MS)
    }

    fun fitAllMarkersInJs() {
        evaluateJsOnMainThread("window.leafletFitAllMarkers && window.leafletFitAllMarkers();")
    }

    private fun evaluateJsOnMainThread(script: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            webViewRef?.evaluateJavascript(script, null)
        } else {
            mainHandler.post {
                webViewRef?.evaluateJavascript(script, null)
            }
        }
    }

    companion object {
        const val BRIDGE_NAME = "AndroidMapBridge"
        const val GESTURE_SETTLE_DEBOUNCE_MS = 180L
        const val PAYLOAD_SYNC_DEBOUNCE_MS = 120L
        const val ZOOM_BUTTON_DEBOUNCE_MS = 90L

        private fun escapeForJsSingleQuote(raw: String): String =
            raw.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "")

        fun buildTechniciansJsonArray(technicians: List<TechnicianEntity>) = buildJsonArray {
            technicians.forEach { tech ->
                add(
                    buildJsonObject {
                        put("id", tech.id)
                        put("name", tech.name)
                        put("badge", tech.badgeNumber)
                        put("role", tech.roleTitle)
                        put("status", tech.status.name)
                        put("statusLabel", tech.status.label)
                        put("lat", tech.latitude)
                        put("lng", tech.longitude)
                        put("heading", tech.headingDegrees)
                        put("vehicleId", tech.vehicleId)
                        put("batteryPct", tech.batteryPct)
                        put("signal", tech.signalStrength)
                        put("speedKmh", tech.speedKmh.toInt())
                        put("initials", tech.avatarInitials)
                        put("taskTitle", tech.currentTaskTitle ?: "Standby")
                    }
                )
            }
        }

        fun buildWorkOrdersJsonArray(tasks: List<TaskEntity>) = buildJsonArray {
            tasks.forEach { task ->
                add(
                    buildJsonObject {
                        put("id", task.id)
                        put("title", task.title)
                        put("siteName", task.siteName)
                        put("category", task.equipmentCategory)
                        put("assetTag", task.assetTag)
                        put("priority", task.priority.name)
                        put("priorityLabel", task.priority.label)
                        put("statusLabel", task.status.label)
                        put("sla", task.slaDeadline)
                        put("assignedTech", task.assignedTechName ?: "Unassigned")
                        put("lat", task.latitude)
                        put("lng", task.longitude)
                    }
                )
            }
        }

        /**
         * Serializes technicians, work orders, and active dispatch routes into a single JSON
         * object consumable by the Leaflet JavaScript runtime.
         */
        fun buildMapPayloadJson(
            filteredTechs: List<TechnicianEntity>,
            filteredTasks: List<TaskEntity>,
            allTechs: List<TechnicianEntity>,
            allTasks: List<TaskEntity>,
            selectedTechId: String?,
            selectedTaskId: String?
        ): String {
            val payload = buildJsonObject {
                put("selectedTechId", selectedTechId ?: "")
                put("selectedTaskId", selectedTaskId ?: "")
                put("technicians", buildTechniciansJsonArray(filteredTechs))
                put("workOrders", buildWorkOrdersJsonArray(filteredTasks))
                put(
                    "routes",
                    buildJsonArray {
                        allTasks.forEach { task ->
                            if ((task.status == TaskStatus.EN_ROUTE || task.status == TaskStatus.ACCEPTED) && task.assignedTechId != null) {
                                val assignedTech = allTechs.firstOrNull { it.id == task.assignedTechId }
                                if (assignedTech != null) {
                                    add(
                                        buildJsonObject {
                                            put("fromLat", assignedTech.latitude)
                                            put("fromLng", assignedTech.longitude)
                                            put("toLat", task.latitude)
                                            put("toLng", task.longitude)
                                            put("label", "${assignedTech.name} → ${task.siteName}")
                                            put("isHighlighted", selectedTechId == assignedTech.id || selectedTaskId == task.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
            return payload.toString()
        }
    }
}
