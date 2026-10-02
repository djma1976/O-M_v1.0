package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.MfaMethod
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TechStatus
import com.example.data.model.TechnicianEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        TaskEntity::class,
        TechnicianEntity::class,
        AuditLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun technicianDao(): TechnicianDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fieldshield_ops.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val userDao = database.userDao()
            val taskDao = database.taskDao()
            val technicianDao = database.technicianDao()
            val auditDao = database.auditDao()

            // 1. Initial Users matching `public.user-login` schema (password_hash stored as SHA-256 hex digest)
            val defaultPasswordSha256 = com.example.data.remote.SupabaseAuthClient.sha256Hex("securePass123!")
            val initialUsers = listOf(
                UserEntity(
                    id = "USR-001",
                    serialId = 1,
                    entity = "Djezzy Telecom",
                    firstName = "Alex",
                    surName = "Rivera",
                    email = "tech.alex@fieldshield.com",
                    username = "alex.rivera",
                    fullName = "Alex Rivera",
                    badgeNumber = "USR-001",
                    role = UserRole.LEAD_TECHNICIAN,
                    phone = "+1 (555) 234-8901",
                    passwordHash = defaultPasswordSha256,
                    createdAt = "2026-09-15 08:30:00",
                    lastLogin = "2026-10-01 09:15:00",
                    status = "active",
                    mfaEnabled = true,
                    preferredMfaMethod = MfaMethod.TOTP_AUTHENTICATOR,
                    totpSecret = "JBSWY3DPEHPK3PXP",
                    backupCodes = "8492-1049,4820-9912,7301-6548,2954-8831",
                    securityQuestion = "What was the asset tag of your first certified transformer?",
                    securityAnswerHash = "tx-904"
                ),
                UserEntity(
                    id = "USR-002",
                    serialId = 2,
                    entity = "Djezzy Telecom",
                    firstName = "Sarah",
                    surName = "Lin",
                    email = "dispatch.sarah@fieldshield.com",
                    username = "sarah.lin",
                    fullName = "Sarah Lin",
                    badgeNumber = "USR-002",
                    role = UserRole.DISPATCHER,
                    phone = "+1 (555) 890-1234",
                    passwordHash = defaultPasswordSha256,
                    createdAt = "2026-09-10 07:45:00",
                    lastLogin = "2026-10-01 08:00:00",
                    status = "active",
                    mfaEnabled = true,
                    preferredMfaMethod = MfaMethod.SMS_RADIO_TOKEN,
                    totpSecret = "KRSXG5CTMVRXEZLU",
                    backupCodes = "1122-3344,5566-7788,9900-1122,3344-5566",
                    securityQuestion = "What city was your regional dispatch center?",
                    securityAnswerHash = "chicago"
                ),
                UserEntity(
                    id = "USR-003",
                    serialId = 3,
                    entity = "Djezzy Telecom",
                    firstName = "Marcus",
                    surName = "Chen",
                    email = "marcus.hvac@fieldshield.com",
                    username = "marcus.chen",
                    fullName = "Marcus Chen",
                    badgeNumber = "USR-003",
                    role = UserRole.HVAC_SPECIALIST,
                    phone = "+1 (555) 345-6789",
                    passwordHash = defaultPasswordSha256,
                    createdAt = "2026-09-18 10:15:00",
                    lastLogin = "2026-10-01 07:50:00",
                    status = "active",
                    mfaEnabled = true,
                    preferredMfaMethod = MfaMethod.BIOMETRIC_KEY,
                    totpSecret = "MFRGGZDFMY2TINBV",
                    backupCodes = "4019-2811,9021-3482,6712-4401,1590-3321",
                    securityQuestion = "What year did you obtain EPA Universal 608?",
                    securityAnswerHash = "2021"
                )
            )
            userDao.insertUsers(initialUsers)

            // 2. Initial Technicians for Fleet Tracking & Map
            val initialTechs = listOf(
                TechnicianEntity(
                    id = "USR-001",
                    name = "Alex Rivera",
                    badgeNumber = "USR-001",
                    roleTitle = "Lead High-Voltage Tech",
                    phone = "+1 (555) 234-8901",
                    status = TechStatus.EN_ROUTE,
                    currentTaskId = "TASK-101",
                    currentTaskTitle = "Main Substation Feeder Tripping",
                    latitude = 41.8830,
                    longitude = -87.6380,
                    headingDegrees = 45f,
                    speedKmh = 48f,
                    batteryPct = 87,
                    signalStrength = "5G Encrypted",
                    vehicleId = "SERVICE-VAN-12",
                    certifications = "NFPA-70E Category 4, High Voltage Cable Splicing",
                    avatarInitials = "AR"
                ),
                TechnicianEntity(
                    id = "USR-003",
                    name = "Marcus Chen",
                    badgeNumber = "USR-003",
                    roleTitle = "HVAC & Industrial Chiller Specialist",
                    phone = "+1 (555) 345-6789",
                    status = TechStatus.ON_SITE,
                    currentTaskId = "TASK-102",
                    currentTaskTitle = "Centrifugal Chiller Bearing Overheating",
                    latitude = 41.8650,
                    longitude = -87.6200,
                    headingDegrees = 0f,
                    speedKmh = 0f,
                    batteryPct = 64,
                    signalStrength = "SatCom Backhaul",
                    vehicleId = "TRUCK-408",
                    certifications = "EPA Universal 608, Ammonia Refrigeration R-717",
                    avatarInitials = "MC"
                ),
                TechnicianEntity(
                    id = "TECH-004",
                    name = "Elena Vance",
                    badgeNumber = "TECH-6104",
                    roleTitle = "Industrial Automation & PLC Tech",
                    phone = "+1 (555) 987-6543",
                    status = TechStatus.AVAILABLE,
                    currentTaskId = null,
                    currentTaskTitle = null,
                    latitude = 41.8950,
                    longitude = -87.6520,
                    headingDegrees = 180f,
                    speedKmh = 0f,
                    batteryPct = 95,
                    signalStrength = "5G Encrypted",
                    vehicleId = "VAN-07",
                    certifications = "Siemens S7, Allen-Bradley Studio 5000, SCADA Cyber",
                    avatarInitials = "EV"
                ),
                TechnicianEntity(
                    id = "TECH-005",
                    name = "David Kowalski",
                    badgeNumber = "TECH-3321",
                    roleTitle = "Hydraulic & Pump Mechanic",
                    phone = "+1 (555) 765-4321",
                    status = TechStatus.AVAILABLE,
                    currentTaskId = null,
                    currentTaskTitle = null,
                    latitude = 41.8510,
                    longitude = -87.6650,
                    headingDegrees = 90f,
                    speedKmh = 0f,
                    batteryPct = 78,
                    signalStrength = "LTE-M",
                    vehicleId = "TRUCK-302",
                    certifications = "Hydraulic Institute Tier 3, Laser Alignment",
                    avatarInitials = "DK"
                )
            )
            technicianDao.insertTechnicians(initialTechs)

            // 3. Realistic Initial Tasks
            val initialTasks = listOf(
                TaskEntity(
                    id = "TASK-101",
                    title = "Main Substation Feeder Tripping on Earth Fault",
                    description = "Feeder 4B at Northside Distribution Station tripped intermittently under 85% load. Transformer thermal sensors show 72°C elevation. Requires isolation, megger test, and busbar thermal imaging.",
                    assetTag = "TX-SUB-4B-902",
                    equipmentCategory = "High-Voltage Substation",
                    siteName = "Northside Substation Alpha",
                    address = "1240 N Elston Ave, Industrial Sector",
                    latitude = 41.9030,
                    longitude = -87.6610,
                    priority = TaskPriority.P1_CRITICAL,
                    status = TaskStatus.EN_ROUTE,
                    assignedTechId = "USR-001",
                    assignedTechName = "Alex Rivera",
                    slaDeadline = "Today 11:30 AM (SLA: 2h)",
                    hazardAlert = "Arc Flash Boundary 12 ft - 40 cal/cm² PPE Category 4 Required. Isolate upstream 13.8kV breaker before entry.",
                    requiredTools = "10kV Megohmmeter, FLIR E8-XT Thermal Imager, Calibrated Torque Wrench, 40 cal Suit",
                    checklistJson = "Wear Category 4 Arc Flash Suit::true;Verify Zero Energy State with Proving Unit::true;Apply Lockout Tagout Padlocks::false;Perform 5kV Insulation Resistance Test::false;Inspect Buchholz Relay Gas Accumulation::false",
                    resolutionNotes = "",
                    signatureName = "",
                    rejectionReason = null,
                    createdAt = System.currentTimeMillis() - 45 * 60 * 1000,
                    acceptedAt = System.currentTimeMillis() - 30 * 60 * 1000
                ),
                TaskEntity(
                    id = "TASK-102",
                    title = "Centrifugal Chiller 450-Ton Bearing Overheat Alert",
                    description = "Vibration sensor alarm on drive end bearing #2 (RMS velocity 0.48 in/s). Oil differential pressure dropping to 14 psi. Risk of catastrophic impeller seizure in data center cooling loop.",
                    assetTag = "HVAC-CHL-03",
                    equipmentCategory = "Industrial HVAC & Chiller",
                    siteName = "Metro Data Hub Facility",
                    address = "350 E Cermak Rd, Data Center Bldg B",
                    latitude = 41.8530,
                    longitude = -87.6190,
                    priority = TaskPriority.P2_HIGH,
                    status = TaskStatus.ON_SITE_WORKING,
                    assignedTechId = "USR-003",
                    assignedTechName = "Marcus Chen",
                    slaDeadline = "Today 2:00 PM (SLA: 4h)",
                    hazardAlert = "Pressurized R-134a refrigerant lines (125 psig). Ear protection mandatory in mechanical gallery.",
                    requiredTools = "Tri-Axial Vibration Analyzer, Ultrasonic Leak Detector, Oil Sampling Kit, Refrigerant Recovery Unit",
                    checklistJson = "Lockout Main 460V Disconnect::true;Check Oil Sump Level and Sight Glass::true;Collect Oil Sample for Spectrochemical Analysis::false;Measure Bearing Vibration Baseline::false;Verify Oil Scavenge Line Temperature::false",
                    resolutionNotes = "",
                    signatureName = "",
                    rejectionReason = null,
                    createdAt = System.currentTimeMillis() - 90 * 60 * 1000,
                    acceptedAt = System.currentTimeMillis() - 75 * 60 * 1000
                ),
                TaskEntity(
                    id = "TASK-103",
                    title = "Emergency Water Booster Pump Cavitation & Seal Leak",
                    description = "Municipal water booster station pump #1 mechanical seal spraying 15 GPM. Discharge pressure fluctuating erratically between 30 and 85 PSI. Potential impeller erosion.",
                    assetTag = "PUMP-BST-01",
                    equipmentCategory = "Hydraulic Pumps",
                    siteName = "South Canal Pumping Depot",
                    address = "2100 S Canal St, Pump House #4",
                    latitude = 41.8540,
                    longitude = -87.6390,
                    priority = TaskPriority.P2_HIGH,
                    status = TaskStatus.DISPATCHED,
                    assignedTechId = "USR-001",
                    assignedTechName = "Alex Rivera",
                    slaDeadline = "Today 4:30 PM (SLA: 6h)",
                    hazardAlert = "Confined Space Warning: Pump pit requires atmospheric gas check (O2, H2S, LEL) prior to descent.",
                    requiredTools = "Dial Indicator, 4-Gas Monitor, Mechanical Seal Puller, Laser Shaft Alignment Tool",
                    checklistJson = "Atmospheric 4-Gas Test Verified::false;Lockout Tagout Suction & Discharge Gate Valves::false;Depressurize Casing via Drain Cock::false;Inspect Mechanical Seal Carbon Face::false;Perform Laser Shaft Realignment::false",
                    resolutionNotes = "",
                    signatureName = "",
                    rejectionReason = null,
                    createdAt = System.currentTimeMillis() - 15 * 60 * 1000
                ),
                TaskEntity(
                    id = "TASK-104",
                    title = "SCADA Remote Terminal Unit (RTU) Communications Loss",
                    description = "Telemetry lost from Riverfront Flood Control Gate actuator RTU. Solar battery backup voltage reporting 11.2V indicating failed charging controller or degraded cell.",
                    assetTag = "RTU-SCADA-09",
                    equipmentCategory = "Automation & Telemetry",
                    siteName = "Riverfront Sluice Gate 3",
                    address = "1800 N Branch St, Pier 12",
                    latitude = 41.9140,
                    longitude = -87.6590,
                    priority = TaskPriority.P3_MEDIUM,
                    status = TaskStatus.PENDING_DISPATCH,
                    assignedTechId = null,
                    assignedTechName = null,
                    slaDeadline = "Tomorrow 9:00 AM",
                    hazardAlert = "Working near open water channel. Personal Flotation Device (PFD) required.",
                    requiredTools = "Ethernet Cable Tester, RS-485 Protocol Sniffer, Solar MPPT Analyzer",
                    checklistJson = "Inspect Solar Panel Output Voltage::false;Test Lead-Acid Battery Bank Load::false;Verify Cellular Modem Signal RSSI::false;Reset Moxa Serial-to-Ethernet Gateway::false",
                    resolutionNotes = "",
                    signatureName = "",
                    rejectionReason = null,
                    createdAt = System.currentTimeMillis() - 10 * 60 * 1000
                )
            )
            taskDao.insertTasks(initialTasks)

            // 4. Initial Audit Logs
            val initialLogs = listOf(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis() - 30 * 60 * 1000,
                    eventCategory = "DISPATCH_LIFECYCLE",
                    actor = "Alex Rivera (USR-001)",
                    summary = "Task Accepted: Substation Feeder Tripping",
                    details = "Technician Alex Rivera accepted critical work order TASK-101 and initiated En Route status.",
                    isSecurityAlert = false
                ),
                AuditLogEntity(
                    timestamp = System.currentTimeMillis() - 40 * 60 * 1000,
                    eventCategory = "MFA_VERIFY",
                    actor = "Alex Rivera (USR-001)",
                    summary = "MFA Verification Succeeded via TOTP Authenticator",
                    details = "Rolling TOTP token validated successfully from device IP 198.51.100.24 (Field Android HW-ID).",
                    isSecurityAlert = false
                ),
                AuditLogEntity(
                    timestamp = System.currentTimeMillis() - 120 * 60 * 1000,
                    eventCategory = "AUTH_SECURITY",
                    actor = "Sarah Lin (USR-002)",
                    summary = "Dispatcher Session Authenticated",
                    details = "Dual-factor dispatch clearance granted with SMS radio confirmation token.",
                    isSecurityAlert = false
                )
            )
            initialLogs.forEach { auditDao.insertLog(it) }
        }
    }
}
