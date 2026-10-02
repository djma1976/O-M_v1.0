package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TechStatus
import com.example.data.model.UserEntity
import com.example.data.ui.components.OpsMapCanvas
import com.example.ui.components.DjezzyEmblem
import com.example.ui.theme.DjezzyRed
import com.example.ui.theme.DjezzyRedLight
import com.example.ui.theme.HazardRed
import com.example.ui.theme.ObsidianDarkBg
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.TechBluePrimary
import com.example.ui.theme.TechBluePrimaryLight
import com.example.viewmodel.AiDiagnosticsViewModel
import com.example.viewmodel.FieldOpsViewModel
import kotlinx.coroutines.launch

enum class DashboardTab(
    val label: String,
    val shortLabel: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    TASKS("Tasks", "Tasks", "Work Orders & Dispatch Queue", Icons.Default.Assignment),
    MAP("Geo Map", "Map", "Live Leaflet Field Operations", Icons.Default.Map),
    ANALYTICS("Analytics", "Charts", "D3 Trends & Efficiency", Icons.Default.BarChart),
    FLEET("Fleet", "Fleet", "Technician Roster & Telemetry", Icons.Default.Group),
    AI_DIAGNOSTICS("AI Assist", "AI", "Smart Equipment Diagnostics", Icons.Default.AutoAwesome),
    SECURITY("Security", "Security", "Profile, MFA & Audit Trail", Icons.Default.Security)
}

@Composable
fun MainDashboardScreen(
    currentUser: UserEntity,
    opsViewModel: FieldOpsViewModel,
    aiViewModel: AiDiagnosticsViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.TASKS) }
    var statusDropdownOpen by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val tasks by opsViewModel.tasks.collectAsStateWithLifecycle()
    val technicians by opsViewModel.technicians.collectAsStateWithLifecycle()
    val auditLogs by opsViewModel.auditLogs.collectAsStateWithLifecycle()
    val selectedTaskId by opsViewModel.selectedTaskId.collectAsStateWithLifecycle()
    val selectedTechId by opsViewModel.selectedTechId.collectAsStateWithLifecycle()
    val activeMapInspector by opsViewModel.activeMapInspector.collectAsStateWithLifecycle()
    val inspectionTask by opsViewModel.inspectionTask.collectAsStateWithLifecycle()
    val snackbarMsg by opsViewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isFleetSimActive by opsViewModel.isFleetSimulationActive.collectAsStateWithLifecycle()
    val lastStatusEvent by opsViewModel.lastStatusEvent.collectAsStateWithLifecycle()

    // Find current user's technician status entry
    val currentTechRecord = remember(technicians, currentUser) {
        technicians.firstOrNull { it.id == currentUser.id }
    }
    val currentTechStatus = currentTechRecord?.status ?: TechStatus.AVAILABLE

    val snackbarHostState = remember { SnackbarHostState() }

    // Close drawer on system back press when open
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    LaunchedEffect(snackbarMsg) {
        if (!snackbarMsg.isNullOrEmpty()) {
            snackbarHostState.showSnackbar(snackbarMsg!!)
            opsViewModel.clearSnackbar()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Disable edge-swipe opening while panning the interactive Leaflet map unless drawer is already open
        gesturesEnabled = drawerState.isOpen || selectedTab != DashboardTab.MAP,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F172A),
                drawerContentColor = Color.White,
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 320.dp)
                    .fillMaxHeight()
                    .testTag("main_navigation_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Drawer Brand Header with Djezzy Emblem & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            DjezzyEmblem(size = 42.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "DJEZZY OPS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Field Operations Center",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DjezzyRedLight
                                )
                            }
                        }
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.close() } },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Navigation Drawer",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Current Authenticated User & Status Card inside Drawer
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = currentUser.fullName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentUser.badgeNumber} • ${currentUser.entity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Active Status:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                                StatusPillBadge(status = currentTechStatus)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "OPERATIONAL MODULES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    // Touch-accessible Drawer Navigation Items (>= 48dp touch targets)
                    DashboardTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        val badgeText = when (tab) {
                            DashboardTab.TASKS -> tasks.size.toString()
                            DashboardTab.MAP -> "${technicians.size + tasks.size}"
                            DashboardTab.ANALYTICS -> "D3"
                            DashboardTab.FLEET -> technicians.size.toString()
                            else -> null
                        }

                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            },
                            label = {
                                Column {
                                    Text(
                                        text = tab.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = tab.subtitle,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color(0xFFE2E8F0) else Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            badge = badgeText?.let { countStr ->
                                {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFF1E293B)
                                    ) {
                                        Text(
                                            text = countStr,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = isSelected,
                            onClick = {
                                selectedTab = tab
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = DjezzyRed,
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .testTag("drawer_nav_tab_${tab.name.lowercase()}")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Sign Out Button in Drawer
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            onSignOut()
                        },
                        border = BorderStroke(1.dp, HazardRed.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("drawer_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = HazardRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign Out of Djezzy Ops",
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = Color(0xDD0F172A),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hamburger Menu Button + Djezzy Brand & User Info
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("top_hamburger_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Open Navigation Drawer",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                DjezzyEmblem(size = 34.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DJEZZY OPS",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 1.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${currentUser.fullName} • ${currentUser.badgeNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Real-time Status Toggle Selector
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = when (currentTechStatus) {
                                        TechStatus.AVAILABLE -> OperationalEmerald.copy(alpha = 0.2f)
                                        TechStatus.EN_ROUTE -> TechBluePrimary.copy(alpha = 0.2f)
                                        TechStatus.ON_SITE -> SafetyAmber.copy(alpha = 0.2f)
                                        TechStatus.EMERGENCY_ASSIST -> HazardRed.copy(alpha = 0.2f)
                                        else -> Color(0xFF1E293B)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when (currentTechStatus) {
                                            TechStatus.AVAILABLE -> OperationalEmerald
                                            TechStatus.EN_ROUTE -> TechBluePrimaryLight
                                            TechStatus.ON_SITE -> SafetyAmber
                                            TechStatus.EMERGENCY_ASSIST -> HazardRed
                                            else -> Color(0xFF475569)
                                        }
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { statusDropdownOpen = true }
                                        .testTag("tech_status_selector")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (currentTechStatus) {
                                                        TechStatus.AVAILABLE -> OperationalEmerald
                                                        TechStatus.EN_ROUTE -> TechBluePrimaryLight
                                                        TechStatus.ON_SITE -> SafetyAmber
                                                        TechStatus.EMERGENCY_ASSIST -> HazardRed
                                                        else -> Color(0xFF94A3B8)
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = currentTechStatus.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = statusDropdownOpen,
                                    onDismissRequest = { statusDropdownOpen = false },
                                    modifier = Modifier.background(Color(0xFF1E293B))
                                ) {
                                    TechStatus.values().forEach { status ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                when (status) {
                                                                    TechStatus.AVAILABLE -> OperationalEmerald
                                                                    TechStatus.EN_ROUTE -> TechBluePrimaryLight
                                                                    TechStatus.ON_SITE -> SafetyAmber
                                                                    TechStatus.EMERGENCY_ASSIST -> HazardRed
                                                                    else -> Color(0xFF64748B)
                                                                }
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(status.label, color = Color.White)
                                                }
                                            },
                                            onClick = {
                                                opsViewModel.updateTechStatus(currentUser.id, status, currentUser.fullName)
                                                statusDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                ResponsiveBottomAppBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onOpenDrawer = {
                        coroutineScope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    DashboardTab.TASKS -> {
                        QueueScreen(
                            currentUser = currentUser,
                            tasks = tasks,
                            technicians = technicians,
                            selectedTask = inspectionTask,
                            onSelectTask = { opsViewModel.selectTask(it) },
                            onAcceptTask = { taskId, techId, techName ->
                                opsViewModel.acceptTask(taskId, techId, techName)
                            },
                            onDeclineTask = { taskId, techId, techName, reason ->
                                opsViewModel.declineTask(taskId, techId, techName, reason)
                            },
                            onUpdateStatus = { taskId, newStatus, techId, techName ->
                                opsViewModel.updateTaskStatus(taskId, newStatus, techId, techName)
                            },
                            onToggleChecklist = { taskId, index ->
                                opsViewModel.toggleChecklistItem(taskId, index)
                            },
                            onCompleteTask = { taskId, notes, sig, techId, techName ->
                                opsViewModel.completeTask(taskId, notes, sig, techId, techName)
                            },
                            onDispatchTask = { taskId, techId, techName, dispName ->
                                opsViewModel.dispatchTask(taskId, techId, techName, dispName)
                            },
                            onCreateTask = { title, desc, tag, cat, site, addr, lat, lng, prio, hazard, tools, sla, techId, techName, actor ->
                                opsViewModel.createWorkOrder(title, desc, tag, cat, site, addr, lat, lng, prio, hazard, tools, sla, techId, techName, actor)
                            },
                            onNavigateToAnalytics = {
                                selectedTab = DashboardTab.ANALYTICS
                            }
                        )
                    }
                    DashboardTab.ANALYTICS -> {
                        D3AnalyticsScreen(
                            technicians = technicians,
                            tasks = tasks,
                            onSelectTechnicianOnMap = { tech ->
                                opsViewModel.selectTech(tech)
                                selectedTab = DashboardTab.MAP
                            }
                        )
                    }
                    DashboardTab.MAP -> {
                        OpsMapCanvas(
                            technicians = technicians,
                            tasks = tasks,
                            selectedTechId = selectedTechId,
                            selectedTaskId = selectedTaskId,
                            userMobileCoordinates = currentTechRecord?.let { it.latitude to it.longitude },
                            onSelectTech = { opsViewModel.selectTech(it) },
                            onSelectTask = { opsViewModel.selectTask(it) },
                            onMarkerTap = { markerType, markerId ->
                                opsViewModel.onMarkerTap(markerType, markerId)
                            },
                            inspectorState = activeMapInspector,
                            onAcceptTask = { task ->
                                opsViewModel.acceptTask(task.id, currentUser.id, currentUser.fullName)
                            },
                            onNavigateToTask = { task ->
                                opsViewModel.selectTask(task)
                                selectedTab = DashboardTab.TASKS
                            },
                            onUpdateTechStatus = { techId, newStatus ->
                                opsViewModel.updateTechStatus(techId, newStatus, currentUser.fullName)
                            },
                            onUpdateTechCoordinates = { techId, lat, lng ->
                                opsViewModel.updateTechnicianCoordinates(techId, lat, lng)
                            },
                            onUpdateTaskCoordinates = { taskId, lat, lng ->
                                opsViewModel.updateWorkOrderCoordinates(taskId, lat, lng)
                            },
                            isSimulationActive = isFleetSimActive,
                            onToggleSimulation = { opsViewModel.toggleFleetSimulation() },
                            onTriggerInstantShift = { opsViewModel.triggerSimulatedShift() },
                            lastStatusEvent = lastStatusEvent
                        )
                    }
                    DashboardTab.FLEET -> {
                        TechniciansScreen(
                            currentUser = currentUser,
                            technicians = technicians,
                            onSelectTechOnMap = { tech ->
                                opsViewModel.selectTech(tech)
                                selectedTab = DashboardTab.MAP
                            }
                        )
                    }
                    DashboardTab.AI_DIAGNOSTICS -> {
                        val selectedEq by aiViewModel.selectedEquipment.collectAsStateWithLifecycle()
                        val symptoms by aiViewModel.symptomInput.collectAsStateWithLifecycle()
                        val conditions by aiViewModel.siteConditions.collectAsStateWithLifecycle()
                        val hazards by aiViewModel.hazardAlert.collectAsStateWithLifecycle()
                        val isAnalyzing by aiViewModel.isAnalyzing.collectAsStateWithLifecycle()
                        val analysisResult by aiViewModel.analysisResult.collectAsStateWithLifecycle()

                        AiDiagnosticsScreen(
                            selectedEquipment = selectedEq,
                            symptomsInput = symptoms,
                            siteConditions = conditions,
                            hazardAlert = hazards,
                            isAnalyzing = isAnalyzing,
                            analysisResult = analysisResult,
                            onUpdateEquipment = { aiViewModel.updateEquipment(it) },
                            onUpdateSymptoms = { aiViewModel.updateSymptoms(it) },
                            onUpdateConditions = { aiViewModel.updateConditions(it) },
                            onUpdateHazards = { aiViewModel.updateHazards(it) },
                            onLoadPreset = { eq, sym, cond, haz ->
                                aiViewModel.loadPreset(eq, sym, cond, haz)
                            },
                            onRunDiagnostics = { aiViewModel.runDiagnostics() }
                        )
                    }
                    DashboardTab.SECURITY -> {
                        SecurityProfileScreen(
                            currentUser = currentUser,
                            auditLogs = auditLogs,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}

/**
 * Responsive Material 3 [BottomAppBar] with a dedicated Hamburger Drawer trigger and
 * touch-accessible (>= 48.dp) navigation items that adapt cleanly to compact mobile screens.
 */
@Composable
private fun ResponsiveBottomAppBar(
    selectedTab: DashboardTab,
    onSelectTab: (DashboardTab) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        containerColor = Color(0xEE0F172A),
        contentColor = Color(0xFF94A3B8),
        tonalElevation = 8.dp,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        windowInsets = WindowInsets.navigationBars,
        modifier = modifier
            .fillMaxWidth()
            .testTag("main_navigation_bar")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompactMobile = maxWidth < 400.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger Menu Trigger inside BottomAppBar for easy one-handed thumb access on mobile
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onOpenDrawer)
                        .padding(horizontal = if (isCompactMobile) 6.dp else 10.dp, vertical = 4.dp)
                        .testTag("bottom_hamburger_menu_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 36.dp, height = 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Menu Drawer",
                            tint = DjezzyRedLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Menu",
                        fontSize = if (isCompactMobile) 9.sp else 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // All 5 Navigation Destinations with 48dp+ touch accessibility & Djezzy Red active pill
                DashboardTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val itemLabel = if (isCompactMobile) tab.shortLabel else tab.label

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                role = Role.Tab,
                                onClick = { onSelectTab(tab) }
                            )
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .testTag("nav_tab_${tab.name.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(width = if (isCompactMobile) 42.dp else 48.dp, height = 28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) DjezzyRed else Color.Transparent
                                )
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = itemLabel,
                            fontSize = if (isCompactMobile) 9.sp else 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) DjezzyRedLight else Color(0xFF94A3B8),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPillBadge(status: TechStatus) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when (status) {
            TechStatus.AVAILABLE -> OperationalEmerald.copy(alpha = 0.2f)
            TechStatus.EN_ROUTE -> TechBluePrimary.copy(alpha = 0.2f)
            TechStatus.ON_SITE -> SafetyAmber.copy(alpha = 0.2f)
            TechStatus.EMERGENCY_ASSIST -> HazardRed.copy(alpha = 0.2f)
            else -> Color(0xFF0F172A)
        }
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = when (status) {
                TechStatus.AVAILABLE -> OperationalEmerald
                TechStatus.EN_ROUTE -> TechBluePrimaryLight
                TechStatus.ON_SITE -> SafetyAmber
                TechStatus.EMERGENCY_ASSIST -> HazardRed
                else -> Color(0xFF94A3B8)
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

