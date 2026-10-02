package com.example.data.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TechStatus
import com.example.data.model.TechnicianEntity
import com.example.ui.theme.DjezzyRed
import com.example.ui.theme.DjezzyRedLight
import com.example.ui.theme.HazardRed
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.TechBluePrimary
import com.example.ui.theme.TechBluePrimaryLight
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class MapFilter(val label: String) {
    ALL("All Units"),
    AVAILABLE("Available"),
    ON_SITE("On-Site"),
    OFFLINE("Offline"),
    CRITICAL_P1("Critical P1")
}

enum class GoogleBaseLayer(val jsKey: String, val label: String) {
    STREETS("googleStreets", "Google Streets"),
    HYBRID("googleHybrid", "Google Hybrid")
}

/**
 * Android WebView-based Leaflet map component (`OpsMapCanvas`) that integrates the
 * `googleHybrid` and `googleStreets` tile layers, renders interactive markers for
 * field technicians and work orders, and connects the WebView map to Kotlin/ViewModel
 * via [MapJavaScriptInterface] (`window.AndroidMapBridge`).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OpsMapCanvas(
    technicians: List<TechnicianEntity>,
    tasks: List<TaskEntity>,
    selectedTechId: String?,
    selectedTaskId: String?,
    onSelectTech: (TechnicianEntity?) -> Unit,
    onSelectTask: (TaskEntity?) -> Unit,
    userMobileCoordinates: Pair<Double, Double>? = null,
    onMarkerTap: ((markerType: String, markerId: String) -> Unit)? = null,
    inspectorState: MapInspectorState? = null,
    onAcceptTask: ((TaskEntity) -> Unit)? = null,
    onNavigateToTask: ((TaskEntity) -> Unit)? = null,
    onUpdateTechStatus: ((techId: String, newStatus: TechStatus) -> Unit)? = null,
    onUpdateTechCoordinates: ((techId: String, lat: Double, lng: Double) -> Unit)? = null,
    onUpdateTaskCoordinates: ((taskId: String, lat: Double, lng: Double) -> Unit)? = null,
    isSimulationActive: Boolean = true,
    onToggleSimulation: (() -> Unit)? = null,
    onTriggerInstantShift: (() -> Unit)? = null,
    lastStatusEvent: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var deviceGpsCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var hasAttemptedLocationPermission by remember { mutableStateOf(false) }

    // Runtime permission launcher for mobile device GPS positioning
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val gpsLoc = if (lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } else null
                val netLoc = if (lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true &&
                    (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                     ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } else null
                val best = gpsLoc ?: netLoc
                if (best != null) {
                    deviceGpsCoordinates = best.latitude to best.longitude
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAttemptedLocationPermission) {
            hasAttemptedLocationPermission = true
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
                try {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val gpsLoc = if (hasFine && lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                        lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    } else null
                    val netLoc = if (lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    } else null
                    val best = gpsLoc ?: netLoc
                    if (best != null) {
                        deviceGpsCoordinates = best.latitude to best.longitude
                    }
                } catch (_: Exception) {}
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    // Resolves the effective mobile position: hardware device GPS -> userMobileCoordinates -> selected/first tech -> Djezzy Algiers
    val effectiveMobilePosition = remember(deviceGpsCoordinates, userMobileCoordinates, technicians, selectedTechId) {
        deviceGpsCoordinates
            ?: userMobileCoordinates
            ?: technicians.firstOrNull { it.id == selectedTechId }?.let { it.latitude to it.longitude }
            ?: technicians.firstOrNull()?.let { it.latitude to it.longitude }
            ?: Pair(36.7538, 3.0588)
    }

    var activeFilter by remember { mutableStateOf(MapFilter.ALL) }
    var activeBaseLayer by remember { mutableStateOf(GoogleBaseLayer.STREETS) }
    var isControlsExpanded by remember { mutableStateOf(false) }
    var isMapReady by remember { mutableStateOf(false) }
    var isPanningOrZooming by remember { mutableStateOf(false) }
    var hasCenteredOnMobilePosition by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val currentTechs by rememberUpdatedState(technicians)
    val currentTasks by rememberUpdatedState(tasks)
    val currentOnSelectTech by rememberUpdatedState(onSelectTech)
    val currentOnSelectTask by rememberUpdatedState(onSelectTask)
    val currentOnMarkerTap by rememberUpdatedState(onMarkerTap)
    val currentOnUpdateTechCoords by rememberUpdatedState(onUpdateTechCoordinates)
    val currentOnUpdateTaskCoords by rememberUpdatedState(onUpdateTaskCoordinates)

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Dedicated Android JavaScriptInterface connecting the WebView map to Kotlin / ViewModel
    val mapJsInterface = remember {
        MapJavaScriptInterface(
            mainHandler = mainHandler,
            onMarkerTapCallback = { markerType, markerId ->
                currentOnMarkerTap?.invoke(markerType, markerId)
            },
            onTechSelected = { techId ->
                if (currentOnMarkerTap == null) {
                    val found = currentTechs.firstOrNull { it.id == techId }
                    currentOnSelectTech(found)
                    currentOnSelectTask(null)
                }
            },
            onTaskSelected = { taskId ->
                if (currentOnMarkerTap == null) {
                    val found = currentTasks.firstOrNull { it.id == taskId }
                    currentOnSelectTask(found)
                    currentOnSelectTech(null)
                }
            },
            onTechCoordinateUpdated = { techId, lat, lng ->
                currentOnUpdateTechCoords?.invoke(techId, lat, lng)
            },
            onTaskCoordinateUpdated = { taskId, lat, lng ->
                currentOnUpdateTaskCoords?.invoke(taskId, lat, lng)
            },
            onMapCleared = {
                currentOnMarkerTap?.invoke("NONE", "")
                currentOnSelectTech(null)
                currentOnSelectTask(null)
            },
            onBaseLayerChanged = { layerKey ->
                activeBaseLayer = if (layerKey == GoogleBaseLayer.HYBRID.jsKey) {
                    GoogleBaseLayer.HYBRID
                } else {
                    GoogleBaseLayer.STREETS
                }
            },
            onMapInitialized = {
                isMapReady = true
            },
            onGestureStateChangedCallback = { interacting ->
                isPanningOrZooming = interacting
            }
        )
    }

    // Availability breakdown counts
    val availableCount = remember(technicians) { technicians.count { it.status == TechStatus.AVAILABLE } }
    val onSiteCount = remember(technicians) { technicians.count { it.status == TechStatus.ON_SITE } }
    val offlineCount = remember(technicians) {
        technicians.count { it.status == TechStatus.OFFLINE || it.status == TechStatus.OFF_DUTY }
    }

    // Filtered technicians & work orders
    val filteredTechs = remember(technicians, activeFilter) {
        when (activeFilter) {
            MapFilter.ALL -> technicians
            MapFilter.AVAILABLE -> technicians.filter { it.status == TechStatus.AVAILABLE }
            MapFilter.ON_SITE -> technicians.filter { it.status == TechStatus.ON_SITE }
            MapFilter.OFFLINE -> technicians.filter {
                it.status == TechStatus.OFFLINE || it.status == TechStatus.OFF_DUTY
            }
            MapFilter.CRITICAL_P1 -> technicians
        }
    }

    val filteredTasks = remember(tasks, activeFilter) {
        when (activeFilter) {
            MapFilter.ALL -> tasks
            MapFilter.AVAILABLE -> tasks.filter { it.status == TaskStatus.PENDING_DISPATCH }
            MapFilter.ON_SITE -> tasks.filter { it.status == TaskStatus.ON_SITE_WORKING }
            MapFilter.OFFLINE -> emptyList()
            MapFilter.CRITICAL_P1 -> tasks.filter { it.priority == TaskPriority.P1_CRITICAL }
        }
    }

    val selectedTech = remember(technicians, selectedTechId) {
        technicians.firstOrNull { it.id == selectedTechId }
    }
    val selectedTask = remember(tasks, selectedTaskId) {
        tasks.firstOrNull { it.id == selectedTaskId }
    }

    // Dynamically synchronize technician & work order coordinates from ViewModel into MapJavaScriptInterface & JS runtime
    LaunchedEffect(filteredTechs, filteredTasks, technicians, tasks, selectedTechId, selectedTaskId, isMapReady) {
        mapJsInterface.syncCoordinatesFromViewModel(
            filteredTechs = filteredTechs,
            filteredTasks = filteredTasks,
            allTechs = technicians,
            allTasks = tasks,
            selectedTechId = selectedTechId,
            selectedTaskId = selectedTaskId
        )
    }

    // Push base layer switches (googleStreets / googleHybrid) to the Leaflet map via MapJavaScriptInterface
    LaunchedEffect(activeBaseLayer, isMapReady) {
        if (isMapReady) {
            mapJsInterface.setBaseLayerInJs(activeBaseLayer.jsKey)
        }
    }

    // Dynamically synchronize mobile position to MapJavaScriptInterface and center map on mobile position
    LaunchedEffect(effectiveMobilePosition, isMapReady) {
        if (isMapReady) {
            mapJsInterface.setMobilePosition(effectiveMobilePosition.first, effectiveMobilePosition.second)
            if (!hasCenteredOnMobilePosition) {
                hasCenteredOnMobilePosition = true
                mapJsInterface.centerOnMobilePositionInJs(
                    effectiveMobilePosition.first,
                    effectiveMobilePosition.second,
                    zoom = 15
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapJsInterface.attachWebView(null)
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
            .testTag("ops_map_canvas_container")
    ) {
        val leafletHtmlDocument = remember { createLeafletMapHtml() }

        // Android WebView rendering the Leaflet map with Google Streets & Google Hybrid layers
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("leaflet_map_webview"),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Hardware layer compositing and zero-overscroll for fluid pan/zoom on low-end GPUs
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER

                    // Prevent parent containers from stealing multi-touch pan/pinch-zoom gestures
                    // and coordinate touch gesture debouncing with MapJavaScriptInterface
                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_POINTER_DOWN,
                            MotionEvent.ACTION_MOVE -> {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                                mapJsInterface.notifyNativeTouchGestureActive(true)
                            }
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL -> {
                                view.parent?.requestDisallowInterceptTouchEvent(false)
                                mapJsInterface.notifyNativeTouchGestureActive(false)
                            }
                        }
                        false
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadsImagesAutomatically = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapReady = true
                            mapJsInterface.pushMapCoordinatesToJs(immediate = true)
                        }
                    }
                    mapJsInterface.attachWebView(this)
                    addJavascriptInterface(mapJsInterface, MapJavaScriptInterface.BRIDGE_NAME)
                    loadDataWithBaseURL(
                        "https://unpkg.com/",
                        leafletHtmlDocument,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    webViewInstance = this
                }
            },
            update = { webView ->
                webViewInstance = webView
                mapJsInterface.attachWebView(webView)
            }
        )

        // Top Mobile-Responsive Floating HUD Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xEC1E293B),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Row 1: Compact Mobile Title + Quick Layer Switch + Expand/Collapse Menu Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(DjezzyRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DJEZZY FIELD OPS MAP",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE2E8F0),
                                    letterSpacing = 0.6.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${filteredTechs.size} Techs • ${filteredTasks.size} Work Orders",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1-Tap Center on Mobile Position Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, TechBluePrimaryLight.copy(alpha = 0.8f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val target = deviceGpsCoordinates ?: effectiveMobilePosition
                                        mapJsInterface.centerOnMobilePositionInJs(target.first, target.second, zoom = 16)
                                    }
                                    .testTag("quick_mobile_center_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Center on Mobile Position",
                                        tint = TechBluePrimaryLight,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "My GPS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // 1-Tap Base Layer Toggle Pill (Streets / Hybrid)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, DjezzyRedLight.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        activeBaseLayer = if (activeBaseLayer == GoogleBaseLayer.STREETS) {
                                            GoogleBaseLayer.HYBRID
                                        } else {
                                            GoogleBaseLayer.STREETS
                                        }
                                    }
                                    .testTag("quick_layer_switch_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = "Switch Base Map Layer",
                                        tint = DjezzyRedLight,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (activeBaseLayer == GoogleBaseLayer.STREETS) "Streets" else "Hybrid",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Expand / Collapse Telemetry & Layer Menu Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isControlsExpanded) DjezzyRed else Color(0xFF0F172A),
                                border = BorderStroke(
                                    1.dp,
                                    if (isControlsExpanded) DjezzyRedLight else Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isControlsExpanded = !isControlsExpanded }
                                    .testTag("toggle_map_menu_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Map Controls",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = if (isControlsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isControlsExpanded) "Collapse Controls" else "Expand Controls",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: Single-Line Horizontally Scrollable Status Pills & Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        AvailabilitySummaryChip(count = availableCount, label = "Avail", color = OperationalEmerald)
                        AvailabilitySummaryChip(count = onSiteCount, label = "On-Site", color = SafetyAmber)
                        AvailabilitySummaryChip(count = offlineCount, label = "Offline", color = Color(0xFF94A3B8))

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFF334155))
                        )

                        MapFilter.values().forEach { filter ->
                            val isSelected = activeFilter == filter
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) DjezzyRed else Color(0xFF0F172A),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) DjezzyRedLight else Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { activeFilter = filter }
                                    .testTag("map_filter_${filter.name.lowercase()}")
                            ) {
                                Text(
                                    text = filter.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Collapsible Secondary Drawer: Base Map Switcher + Live Telemetry Controls + Ticker
                    AnimatedVisibility(visible = isControlsExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GoogleBaseLayer.values().forEach { layer ->
                                    val isSelected = activeBaseLayer == layer
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) DjezzyRed else Color(0xFF0F172A),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) DjezzyRedLight else Color(0xFF334155)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { activeBaseLayer = layer }
                                            .testTag("base_layer_${layer.jsKey}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = layer.label,
                                                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = layer.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                if (onToggleSimulation != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSimulationActive) TechBluePrimary.copy(alpha = 0.2f) else Color(0xFF0F172A),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSimulationActive) TechBluePrimaryLight else Color(0xFF334155)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(onClick = onToggleSimulation)
                                            .testTag("toggle_simulation_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (isSimulationActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = if (isSimulationActive) TechBluePrimaryLight else Color(0xFF94A3B8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isSimulationActive) "Live Shifts: ON" else "Live Shifts: PAUSED",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSimulationActive) TechBluePrimaryLight else Color(0xFF94A3B8),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                if (onTriggerInstantShift != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(onClick = onTriggerInstantShift)
                                            .testTag("trigger_shift_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = TechBluePrimaryLight,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Pulse Shift",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            // Real-time Status Event Ticker
                            if (!lastStatusEvent.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Sensors,
                                            contentDescription = null,
                                            tint = TechBluePrimaryLight,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = lastStatusEvent,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Resolve reactive inspector card state from ViewModel (or fallback to selectedTech / selectedTask)
        val effectiveInspectorState = remember(inspectorState, selectedTech, selectedTask) {
            inspectorState ?: when {
                selectedTech != null -> MapInspectorState.TechnicianInspector(selectedTech)
                selectedTask != null -> MapInspectorState.TaskInspector(selectedTask)
                else -> MapInspectorState.None
            }
        }

        // Right-Side Leaflet Zoom & Fit Bounds Controls (anchored to BottomEnd so they never collide with the top menu)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 12.dp,
                    bottom = if (effectiveInspectorState !is MapInspectorState.None) 245.dp else 28.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xDD1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = {
                        mapJsInterface.zoomInInJs()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("map_zoom_in_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xDD1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = {
                        mapJsInterface.zoomOutInJs()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("map_zoom_out_button")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xDD1E293B),
                border = BorderStroke(1.dp, TechBluePrimaryLight.copy(alpha = 0.8f)),
                shadowElevation = 6.dp
            ) {
                IconButton(
                    onClick = {
                        // Refresh device location if available
                        try {
                            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                val gpsLoc = if (hasFine && lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                                    lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                } else null
                                val netLoc = if (lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                                    lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                } else null
                                val best = gpsLoc ?: netLoc
                                if (best != null) {
                                    deviceGpsCoordinates = best.latitude to best.longitude
                                }
                            }
                        } catch (_: Exception) {}

                        val target = deviceGpsCoordinates ?: effectiveMobilePosition
                        mapJsInterface.centerOnMobilePositionInJs(target.first, target.second, zoom = 16)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("map_recenter_button")
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center on Mobile Position", tint = TechBluePrimaryLight)
                }
            }
        }

        // Bottom Selected Technician or Work Order Inspector Card (reactively updated on `onMarkerTap`)
        when (val activeInspector = effectiveInspectorState) {
            is MapInspectorState.TechnicianInspector -> {
                TechnicianInspectionCard(
                    tech = activeInspector.technician,
                    onDismiss = {
                        onMarkerTap?.invoke("NONE", "")
                        onSelectTech(null)
                    },
                    onNavigateToAssignedTask = { taskId ->
                        onMarkerTap?.invoke("TASK", taskId)
                        val task = tasks.firstOrNull { it.id == taskId }
                        if (task != null) onSelectTask(task)
                    },
                    onUpdateStatus = onUpdateTechStatus,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
            is MapInspectorState.TaskInspector -> {
                TaskInspectionCard(
                    task = activeInspector.task,
                    onDismiss = {
                        onMarkerTap?.invoke("NONE", "")
                        onSelectTask(null)
                    },
                    onAccept = onAcceptTask,
                    onNavigate = onNavigateToTask,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
            MapInspectorState.None -> Unit
        }
    }
}

/**
 * Generates the Leaflet HTML template with `googleHybrid` and `googleStreets` tile layers,
 * `leaflet.markercluster` CSS/JS, and marker rendering for technicians and work orders.
 */
private fun createLeafletMapHtml(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <title>OpsMapCanvas - Leaflet Map</title>

    <!-- Leaflet CSS -->
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/MarkerCluster.css" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/MarkerCluster.Default.css" />

    <style>
        html, body, #map {
            height: 100%;
            width: 100%;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            overflow: hidden;
        }
        /* Technician Marker Styling */
        .tech-marker-wrap {
            position: relative;
            width: 40px;
            height: 40px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .tech-pulse {
            position: absolute;
            width: 40px;
            height: 40px;
            border-radius: 50%;
            animation: techPulseAnim 1.8s ease-out infinite;
            pointer-events: none;
        }
        @keyframes techPulseAnim {
            0% { transform: scale(0.75); opacity: 0.9; }
            100% { transform: scale(1.65); opacity: 0; }
        }
        .tech-badge {
            position: relative;
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: #0F172A;
            border: 3px solid #10B981;
            color: #FFFFFF;
            font-weight: 800;
            font-size: 11px;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 3px 10px rgba(0, 0, 0, 0.55);
        }
        .tech-badge.selected {
            transform: scale(1.22);
            box-shadow: 0 0 14px rgba(226, 0, 26, 0.95);
        }
        /* Work Order Pin Marker Styling */
        .wo-pin-wrap {
            position: relative;
            width: 36px;
            height: 42px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .wo-pin-head {
            width: 28px;
            height: 28px;
            border-radius: 50% 50% 50% 0;
            transform: rotate(-45deg);
            display: flex;
            align-items: center;
            justify-content: center;
            border: 2px solid #0F172A;
            box-shadow: 0 3px 10px rgba(0, 0, 0, 0.6);
        }
        .wo-pin-head.selected {
            transform: rotate(-45deg) scale(1.24);
            border-color: #FFFFFF;
        }
        .wo-pin-label {
            transform: rotate(45deg);
            color: #FFFFFF;
            font-size: 9px;
            font-weight: 900;
        }
        /* Popup Styling */
        .leaflet-popup-content-wrapper {
            background: #182234 !important;
            color: #F8FAFC !important;
            border: 1px solid #334155;
            border-radius: 12px !important;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.55) !important;
        }
        .leaflet-popup-tip {
            background: #182234 !important;
        }
        .popup-title {
            font-weight: 800;
            font-size: 13px;
            color: #FFFFFF;
            margin-bottom: 3px;
        }
        .popup-sub {
            font-size: 11px;
            color: #94A3B8;
            margin-bottom: 6px;
        }
        .popup-meta {
            font-size: 10px;
            color: #CBD5E1;
            background: #0F172A;
            padding: 5px 8px;
            border-radius: 6px;
        }
        /* High-visibility Mobile Position Pulse Marker */
        .mobile-loc-ping {
            position: relative;
            width: 32px;
            height: 32px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .mobile-loc-radar {
            position: absolute;
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: rgba(14, 165, 233, 0.35);
            border: 2px solid #0284C7;
            animation: mobileRadarAnim 2s ease-out infinite;
            pointer-events: none;
        }
        @keyframes mobileRadarAnim {
            0% { transform: scale(0.5); opacity: 0.9; }
            100% { transform: scale(1.85); opacity: 0; }
        }
        .mobile-loc-dot {
            width: 14px;
            height: 14px;
            border-radius: 50%;
            background: #0284C7;
            border: 2.5px solid #FFFFFF;
            box-shadow: 0 0 10px rgba(2, 132, 199, 0.85);
            z-index: 2;
        }
        body.map-interacting .mobile-loc-radar {
            animation-play-state: paused !important;
            display: none !important;
        }
        /* Low-end hardware optimization: pause expensive pulse animations & drop shadows during active pan/zoom */
        body.map-interacting .tech-pulse {
            animation-play-state: paused !important;
            display: none !important;
        }
        body.map-interacting .tech-badge,
        body.map-interacting .wo-pin-head {
            box-shadow: none !important;
            transition: none !important;
        }
    </style>
</head>
<body>
    <div id="map"></div>

    <!-- Leaflet JS & MarkerCluster JS -->
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.5.3/leaflet.markercluster.js"></script>

    <script>
        // Reusable debounce utility for pan/zoom touch gestures on low-end hardware
        function debounce(fn, waitMs) {
            var timeoutId = null;
            return function() {
                var context = this;
                var args = arguments;
                if (timeoutId !== null) {
                    clearTimeout(timeoutId);
                }
                timeoutId = setTimeout(function() {
                    timeoutId = null;
                    fn.apply(context, args);
                }, waitMs);
            };
        }

        const PAN_ZOOM_DEBOUNCE_MS = 180;
        const MARKER_RENDER_DEBOUNCE_MS = 90;

        // Base maps (with low-end hardware tile debouncing: updateWhenIdle=true, updateWhenZooming=false)
        const googleHybrid = L.tileLayer('https://{s}.google.com/vt/lyrs=s,h&x={x}&y={y}&z={z}', {
            maxZoom: 20,
            subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
            updateWhenIdle: true,
            updateWhenZooming: false,
            keepBuffer: 2
        });
        const googleStreets = L.tileLayer('https://{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
            maxZoom: 20,
            subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
            updateWhenIdle: true,
            updateWhenZooming: false,
            keepBuffer: 2
        });

        // Initialize Leaflet map centered at Djezzy Telecom Headquarters (Algiers) or mobile position
        const map = L.map('map', {
            center: [36.7538, 3.0588],
            zoom: 14,
            zoomControl: false,
            preferCanvas: true,
            fadeAnimation: false,
            markerZoomAnimation: false,
            inertia: true,
            inertiaDeceleration: 3200,
            wheelDebounceTime: 140,
            tapTolerance: 15,
            layers: [googleStreets]
        });

        const baseMaps = {
            "Google Streets": googleStreets,
            "Google Hybrid": googleHybrid
        };

        L.control.layers(baseMaps, null, { position: 'bottomleft' }).addTo(map);

        var activeBaseLayer = googleStreets;
        var isUserInteracting = false;
        var pendingMarkerPayload = null;
        var mobileLocationLayer = L.layerGroup().addTo(map);

        window.updateMobileLocationMarker = function(lat, lng) {
            mobileLocationLayer.clearLayers();
            var icon = L.divIcon({
                html: '<div class="mobile-loc-ping"><div class="mobile-loc-radar"></div><div class="mobile-loc-dot"></div></div>',
                className: '',
                iconSize: [32, 32],
                iconAnchor: [16, 16],
                popupAnchor: [0, -16]
            });
            var marker = L.marker([lat, lng], { icon: icon, zIndexOffset: 2000 });
            marker.bindPopup('<div class="popup-title">Current Mobile Position</div><div class="popup-sub">GPS Active • Coordinates: ' + lat.toFixed(5) + ', ' + lng.toFixed(5) + '</div>');
            mobileLocationLayer.addLayer(marker);
        };

        window.centerOnMobilePosition = function(lat, lng, zoom) {
            var z = zoom || 16;
            map.flyTo([lat, lng], z, { duration: 0.7, animate: true });
            window.updateMobileLocationMarker(lat, lng);
        };

        // Notify Kotlin when pan/zoom gesture begins and pause heavy CSS effects
        function onPanZoomGestureStart() {
            if (!isUserInteracting) {
                isUserInteracting = true;
                document.body.classList.add('map-interacting');
                if (window.AndroidMapBridge && window.AndroidMapBridge.onGestureStateChanged) {
                    window.AndroidMapBridge.onGestureStateChanged(true);
                }
            }
        }

        // Debounced settle handler executed after pan/zoom gestures finish
        var onPanZoomGestureSettled = debounce(function() {
            isUserInteracting = false;
            document.body.classList.remove('map-interacting');

            // Flush any marker/telemetry update that was deferred during the gesture
            if (pendingMarkerPayload !== null) {
                var deferredPayload = pendingMarkerPayload;
                pendingMarkerPayload = null;
                window.requestAnimationFrame(function() {
                    executeRenderMarkers(deferredPayload);
                });
            }

            if (window.AndroidMapBridge) {
                var center = map.getCenter();
                if (window.AndroidMapBridge.onPanZoomSettled) {
                    window.AndroidMapBridge.onPanZoomSettled(center.lat, center.lng, map.getZoom());
                } else if (window.AndroidMapBridge.onGestureStateChanged) {
                    window.AndroidMapBridge.onGestureStateChanged(false);
                }
            }
        }, PAN_ZOOM_DEBOUNCE_MS);

        map.on('movestart zoomstart dragstart', onPanZoomGestureStart);
        map.on('move zoom moveend zoomend dragend', onPanZoomGestureSettled);

        map.on('baselayerchange', function(e) {
            if (e.name === "Google Hybrid") {
                activeBaseLayer = googleHybrid;
                if (window.AndroidMapBridge && window.AndroidMapBridge.onBaseMapSelected) {
                    window.AndroidMapBridge.onBaseMapSelected("googleHybrid");
                }
            } else {
                activeBaseLayer = googleStreets;
                if (window.AndroidMapBridge && window.AndroidMapBridge.onBaseMapSelected) {
                    window.AndroidMapBridge.onBaseMapSelected("googleStreets");
                }
            }
        });

        window.setBaseMapLayer = function(layerKey) {
            var nextLayer = (layerKey === 'googleHybrid') ? googleHybrid : googleStreets;
            if (nextLayer !== activeBaseLayer) {
                map.removeLayer(activeBaseLayer);
                activeBaseLayer = nextLayer;
                map.addLayer(activeBaseLayer);
            }
        };

        // Initialize MarkerClusterGroup with chunked loading & disabled cluster animations for low-end devices
        var markerLayerGroup = (typeof L.markerClusterGroup === 'function')
            ? L.markerClusterGroup({
                showCoverageOnHover: false,
                zoomToBoundsOnClick: true,
                spiderfyOnMaxZoom: true,
                animate: false,
                animateAddingMarkers: false,
                chunkedLoading: true,
                chunkInterval: 100,
                chunkDelay: 25,
                removeOutsideVisibleBounds: true,
                maxClusterRadius: 45
            })
            : L.featureGroup();
        map.addLayer(markerLayerGroup);

        var routeLayerGroup = L.layerGroup().addTo(map);
        var hasFittedInitialBounds = false;
        var techMarkersById = {};
        var workOrderMarkersById = {};
        var lastSelectedTechId = "";
        var lastSelectedTaskId = "";

        function getTechStatusColor(status) {
            switch (status) {
                case "AVAILABLE": return "#10B981";
                case "EN_ROUTE": return "#38BDF8";
                case "ON_SITE": return "#F59E0B";
                case "EMERGENCY_ASSIST": return "#EF4444";
                default: return "#64748B";
            }
        }

        function getPriorityColor(priority) {
            switch (priority) {
                case "P1_CRITICAL": return "#EF4444";
                case "P2_HIGH": return "#F59E0B";
                case "P3_MEDIUM": return "#38BDF8";
                default: return "#10B981";
            }
        }

        function executeRenderMarkers(rawJson) {
            try {
                var data = (typeof rawJson === 'string') ? JSON.parse(rawJson) : rawJson;
                lastSelectedTechId = data.selectedTechId || "";
                lastSelectedTaskId = data.selectedTaskId || "";

                markerLayerGroup.clearLayers();
                routeLayerGroup.clearLayers();
                techMarkersById = {};
                workOrderMarkersById = {};

                var allCoords = [];

                // 1. Render Active Dispatch Route Polylines
                if (data.routes && data.routes.length) {
                    data.routes.forEach(function(route) {
                        var polyline = L.polyline(
                            [[route.fromLat, route.fromLng], [route.toLat, route.toLng]],
                            {
                                color: route.isHighlighted ? '#E2001A' : '#0284C7',
                                weight: route.isHighlighted ? 4 : 3,
                                opacity: 0.9,
                                dashArray: '8, 8'
                            }
                        );
                        polyline.bindPopup('<div class="popup-title">Active Route</div><div class="popup-sub">' + route.label + '</div>');
                        routeLayerGroup.addLayer(polyline);
                    });
                }

                // 2. Render Work Order Markers
                if (data.workOrders && data.workOrders.length) {
                    data.workOrders.forEach(function(wo) {
                        var pinColor = getPriorityColor(wo.priority);
                        var isSelected = (data.selectedTaskId === wo.id);
                        var shortPriority = (wo.priority || 'WO').split('_')[0];

                        var pinHtml = '<div class="wo-pin-wrap">' +
                            '<div class="wo-pin-head' + (isSelected ? ' selected' : '') + '" style="background:' + pinColor + ';">' +
                            '<span class="wo-pin-label">' + shortPriority + '</span>' +
                            '</div></div>';

                        var woMarker = L.marker([wo.lat, wo.lng], {
                            draggable: true,
                            icon: L.divIcon({
                                html: pinHtml,
                                className: '',
                                iconSize: [36, 42],
                                iconAnchor: [18, 36],
                                popupAnchor: [0, -32]
                            })
                        });

                        woMarker.bindPopup(
                            '<div class="popup-title">' + wo.title + '</div>' +
                            '<div class="popup-sub">' + wo.siteName + ' • ' + wo.priorityLabel + '</div>' +
                            '<div class="popup-meta">Asset: ' + wo.assetTag + '<br/>Status: ' + wo.statusLabel + '<br/>Assigned: ' + wo.assignedTech + '<br/>SLA: ' + wo.sla + '</div>'
                        );

                        woMarker.on('click', function() {
                            if (window.AndroidMapBridge) {
                                if (window.AndroidMapBridge.onMarkerTap) {
                                    window.AndroidMapBridge.onMarkerTap('TASK', wo.id);
                                } else if (window.AndroidMapBridge.onWorkOrderMarkerClick) {
                                    window.AndroidMapBridge.onWorkOrderMarkerClick(wo.id);
                                }
                            }
                        });

                        woMarker.on('dragend', function(event) {
                            var pos = event.target.getLatLng();
                            if (window.AndroidMapBridge && window.AndroidMapBridge.onWorkOrderCoordinateChanged) {
                                window.AndroidMapBridge.onWorkOrderCoordinateChanged(wo.id, pos.lat, pos.lng);
                            }
                        });

                        workOrderMarkersById[wo.id] = woMarker;
                        markerLayerGroup.addLayer(woMarker);
                        allCoords.push([wo.lat, wo.lng]);
                    });
                }

                // 3. Render Technician Markers
                if (data.technicians && data.technicians.length) {
                    data.technicians.forEach(function(tech) {
                        var statusColor = getTechStatusColor(tech.status);
                        var isSelected = (data.selectedTechId === tech.id);
                        var isOffline = (tech.status === 'OFFLINE' || tech.status === 'OFF_DUTY');

                        var pulseRing = !isOffline
                            ? '<div class="tech-pulse" style="border: 2px solid ' + statusColor + ';"></div>'
                            : '';

                        var techHtml = '<div class="tech-marker-wrap">' +
                            pulseRing +
                            '<div class="tech-badge' + (isSelected ? ' selected' : '') + '" style="border-color:' + statusColor + ';">' +
                            tech.initials +
                            '</div></div>';

                        var techMarker = L.marker([tech.lat, tech.lng], {
                            draggable: true,
                            icon: L.divIcon({
                                html: techHtml,
                                className: '',
                                iconSize: [40, 40],
                                iconAnchor: [20, 20],
                                popupAnchor: [0, -20]
                            })
                        });

                        techMarker.bindPopup(
                            '<div class="popup-title">' + tech.name + ' (' + tech.badge + ')</div>' +
                            '<div class="popup-sub">' + tech.role + ' • ' + tech.statusLabel + '</div>' +
                            '<div class="popup-meta">Vehicle: ' + tech.vehicleId + ' • Battery: ' + tech.batteryPct + '%<br/>Assignment: ' + tech.taskTitle + '</div>'
                        );

                        techMarker.on('click', function() {
                            if (window.AndroidMapBridge) {
                                if (window.AndroidMapBridge.onMarkerTap) {
                                    window.AndroidMapBridge.onMarkerTap('TECHNICIAN', tech.id);
                                } else if (window.AndroidMapBridge.onTechnicianMarkerClick) {
                                    window.AndroidMapBridge.onTechnicianMarkerClick(tech.id);
                                }
                            }
                        });

                        techMarker.on('dragend', function(event) {
                            var pos = event.target.getLatLng();
                            if (window.AndroidMapBridge && window.AndroidMapBridge.onTechnicianCoordinateChanged) {
                                window.AndroidMapBridge.onTechnicianCoordinateChanged(tech.id, pos.lat, pos.lng);
                            }
                        });

                        techMarkersById[tech.id] = techMarker;
                        markerLayerGroup.addLayer(techMarker);
                        allCoords.push([tech.lat, tech.lng]);
                    });
                }

                if (!hasFittedInitialBounds && allCoords.length > 0) {
                    hasFittedInitialBounds = true;
                    map.fitBounds(allCoords, { padding: [70, 70], maxZoom: 14, animate: false });
                }
            } catch (e) {
                console.error("Failed to render markers:", e);
            }
        }

        var debouncedRenderMarkers = debounce(function(rawJson) {
            window.requestAnimationFrame(function() {
                executeRenderMarkers(rawJson);
            });
        }, MARKER_RENDER_DEBOUNCE_MS);

        window.renderMarkers = function(rawJson) {
            if (!hasFittedInitialBounds) {
                executeRenderMarkers(rawJson);
                return;
            }
            if (isUserInteracting) {
                // Defer DOM marker updates until the active pan/zoom gesture finishes
                pendingMarkerPayload = rawJson;
                return;
            }
            debouncedRenderMarkers(rawJson);
        };

        // Dynamic single-technician coordinate update from ViewModel
        window.updateTechnicianMarkerCoordinate = function(techId, lat, lng, status) {
            var marker = techMarkersById[techId];
            if (marker) {
                marker.setLatLng([lat, lng]);
            }
        };

        // Dynamic single-work-order coordinate update from ViewModel
        window.updateWorkOrderMarkerCoordinate = function(taskId, lat, lng) {
            var marker = workOrderMarkersById[taskId];
            if (marker) {
                marker.setLatLng([lat, lng]);
            }
        };

        window.leafletFlyToCoordinate = function(lat, lng, zoom) {
            map.flyTo([lat, lng], zoom || 15, { duration: 0.6 });
        };

        window.leafletZoomByDelta = function(delta) {
            map.setZoom(map.getZoom() + delta, { animate: false });
        };

        window.leafletZoomIn = debounce(function() {
            map.zoomIn(1, { animate: false });
        }, 80);

        window.leafletZoomOut = debounce(function() {
            map.zoomOut(1, { animate: false });
        }, 80);

        window.leafletFitAllMarkers = debounce(function() {
            if (markerLayerGroup.getLayers && markerLayerGroup.getLayers().length > 0) {
                map.fitBounds(markerLayerGroup.getBounds(), { padding: [70, 70], maxZoom: 14, animate: false });
            } else if (window.AndroidMapBridge && window.AndroidMapBridge.getMobilePositionJson) {
                try {
                    var mobilePosStr = window.AndroidMapBridge.getMobilePositionJson();
                    if (mobilePosStr) {
                        var mobilePos = JSON.parse(mobilePosStr);
                        if (mobilePos && typeof mobilePos.lat === 'number' && typeof mobilePos.lng === 'number') {
                            map.setView([mobilePos.lat, mobilePos.lng], 15, { animate: false });
                            return;
                        }
                    }
                } catch (e) {}
                map.setView([36.7538, 3.0588], 14, { animate: false });
            } else {
                map.setView([36.7538, 3.0588], 14, { animate: false });
            }
        }, 100);

        map.on('click', function() {
            if (window.AndroidMapBridge && window.AndroidMapBridge.onMapSurfaceClick) {
                window.AndroidMapBridge.onMapSurfaceClick();
            }
        });

        // Pull initial coordinates and mobile position from MapJavaScriptInterface when Leaflet initializes
        if (window.AndroidMapBridge) {
            if (window.AndroidMapBridge.getMobilePositionJson) {
                try {
                    var mobilePosStr = window.AndroidMapBridge.getMobilePositionJson();
                    if (mobilePosStr) {
                        var mobilePos = JSON.parse(mobilePosStr);
                        if (mobilePos && typeof mobilePos.lat === 'number' && typeof mobilePos.lng === 'number') {
                            map.setView([mobilePos.lat, mobilePos.lng], 15);
                            window.updateMobileLocationMarker(mobilePos.lat, mobilePos.lng);
                            hasFittedInitialBounds = true;
                        }
                    }
                } catch (err) {
                    console.error("Error reading mobile position from AndroidMapBridge:", err);
                }
            }
            if (window.AndroidMapBridge.getMapPayloadJson) {
                try {
                    var initialPayload = window.AndroidMapBridge.getMapPayloadJson();
                    if (initialPayload) {
                        window.renderMarkers(initialPayload);
                    }
                } catch (err) {
                    console.error("Error pulling initial payload from AndroidMapBridge:", err);
                }
            }
            if (window.AndroidMapBridge.onLeafletReady) {
                window.AndroidMapBridge.onLeafletReady();
            }
        }
    </script>
</body>
</html>
""".trimIndent()

// ---------------- INSPECTION DETAIL CARDS ----------------

@Composable
fun TechnicianInspectionCard(
    tech: TechnicianEntity,
    onDismiss: () -> Unit,
    onNavigateToAssignedTask: (String) -> Unit,
    onUpdateStatus: ((techId: String, newStatus: TechStatus) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tech_inspection_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0182234)),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(TechBluePrimary))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TechBluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tech.avatarInitials,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tech.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${tech.roleTitle} • ${tech.badgeNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (tech.status) {
                        TechStatus.AVAILABLE -> OperationalEmerald.copy(alpha = 0.2f)
                        TechStatus.EN_ROUTE -> TechBluePrimary.copy(alpha = 0.2f)
                        TechStatus.ON_SITE -> SafetyAmber.copy(alpha = 0.2f)
                        TechStatus.EMERGENCY_ASSIST -> HazardRed.copy(alpha = 0.2f)
                        else -> Color(0xFF334155)
                    }
                ) {
                    Text(
                        text = tech.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (tech.status) {
                            TechStatus.AVAILABLE -> OperationalEmerald
                            TechStatus.EN_ROUTE -> TechBluePrimaryLight
                            TechStatus.ON_SITE -> SafetyAmber
                            TechStatus.EMERGENCY_ASSIST -> HazardRed
                            else -> Color(0xFF94A3B8)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "REAL-TIME AVAILABILITY CONTROLS",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AvailabilityQuickButton(
                    label = "Available",
                    isActive = tech.status == TechStatus.AVAILABLE,
                    color = OperationalEmerald,
                    onClick = { onUpdateStatus?.invoke(tech.id, TechStatus.AVAILABLE) },
                    modifier = Modifier.weight(1f)
                )
                AvailabilityQuickButton(
                    label = "On-Site",
                    isActive = tech.status == TechStatus.ON_SITE,
                    color = SafetyAmber,
                    onClick = { onUpdateStatus?.invoke(tech.id, TechStatus.ON_SITE) },
                    modifier = Modifier.weight(1f)
                )
                AvailabilityQuickButton(
                    label = "Offline",
                    isActive = tech.status == TechStatus.OFFLINE || tech.status == TechStatus.OFF_DUTY,
                    color = Color(0xFF64748B),
                    onClick = { onUpdateStatus?.invoke(tech.id, TechStatus.OFFLINE) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryItem(label = "Vehicle", value = tech.vehicleId)
                TelemetryItem(label = "Battery", value = "${tech.batteryPct}%")
                TelemetryItem(label = "Link", value = tech.signalStrength)
                TelemetryItem(label = "Speed", value = "${tech.speedKmh.toInt()} km/h")
            }

            if (tech.currentTaskId != null && tech.currentTaskTitle != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ACTIVE ASSIGNMENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = TechBluePrimaryLight,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tech.currentTaskTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                        Button(
                            onClick = { onNavigateToAssignedTask(tech.currentTaskId) },
                            colors = ButtonDefaults.buttonColors(containerColor = TechBluePrimary),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Inspect", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Close", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun TaskInspectionCard(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onAccept: ((TaskEntity) -> Unit)?,
    onNavigate: ((TaskEntity) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_inspection_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0182234)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(
                when (task.priority) {
                    TaskPriority.P1_CRITICAL -> HazardRed
                    TaskPriority.P2_HIGH -> SafetyAmber
                    else -> TechBluePrimary
                }
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${task.siteName} • ${task.equipmentCategory}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (task.priority) {
                        TaskPriority.P1_CRITICAL -> HazardRed.copy(alpha = 0.2f)
                        TaskPriority.P2_HIGH -> SafetyAmber.copy(alpha = 0.2f)
                        else -> TechBluePrimary.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = task.priority.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (task.priority) {
                            TaskPriority.P1_CRITICAL -> HazardRed
                            TaskPriority.P2_HIGH -> SafetyAmber
                            else -> TechBluePrimaryLight
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!task.hazardAlert.isNullOrEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HazardRed.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = HazardRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.hazardAlert,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFCA5A5),
                            maxLines = 2
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryItem(label = "Status", value = task.status.label)
                TelemetryItem(label = "Deadline", value = task.slaDeadline)
                TelemetryItem(label = "Assigned", value = task.assignedTechName ?: "Unassigned")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close", color = Color(0xFF94A3B8))
                }
                if (task.status == TaskStatus.DISPATCHED && onAccept != null) {
                    Button(
                        onClick = { onAccept(task) },
                        colors = ButtonDefaults.buttonColors(containerColor = OperationalEmerald),
                        modifier = Modifier.weight(1.5f).testTag("accept_task_map_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Accept Task")
                    }
                } else if (onNavigate != null) {
                    Button(
                        onClick = { onNavigate(task) },
                        colors = ButtonDefaults.buttonColors(containerColor = TechBluePrimary),
                        modifier = Modifier.weight(1.5f).testTag("view_work_order_button")
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Work Order")
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE2E8F0), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
fun AvailabilitySummaryChip(count: Int, label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun AvailabilityQuickButton(
    label: String,
    isActive: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) color.copy(alpha = 0.22f) else Color(0xFF0F172A),
        border = BorderStroke(
            1.dp,
            if (isActive) color else Color(0xFF334155)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("quick_avail_${label.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isActive) color else Color(0xFF64748B))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) Color.White else Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }
    }
}
