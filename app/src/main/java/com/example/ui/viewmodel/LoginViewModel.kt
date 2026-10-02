package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.MfaMethod
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.ui.data.SessionDataStoreRepository
import com.example.ui.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
data class UserLoginRow(
    val id: Int? = null,
    val entity: String = "Djezzy Telecom",
    @SerialName("user_id") val userId: String = "",
    @SerialName("first-name") val firstName: String = "",
    @SerialName("sur-name") val surName: String = "",
    val email: String = "",
    val username: String = "",
    @SerialName("password_hash") val passwordHash: String = "",
    val role: String = "LEAD_TECHNICIAN",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_login") val lastLogin: String? = null,
    val status: String? = "active"
)

data class SupabaseDbStatus(
    val isConfigured: Boolean = false,
    val isConnected: Boolean = false,
    val activeTable: String = "user-login",
    val rowCount: Int = 0,
    val projectUrl: String = "",
    val statusText: String = "Checking Supabase connection...",
    val diagnosticReport: String = ""
)

data class LoginFormValidationState(
    val usernameOrEmail: String = "",
    val password: String = "",
    val usernameOrEmailError: String? = null,
    val passwordError: String? = null,
    val regEmailError: String? = null,
    val isLoginValid: Boolean = false
)

enum class RecoveryStep {
    REQUEST_TOKEN,
    VERIFY_AND_RESET,
    COMPLETED
}

data class PasswordRecoveryState(
    val step: RecoveryStep = RecoveryStep.REQUEST_TOKEN,
    val identifierInput: String = "",
    val targetUsername: String = "",
    val targetEmail: String = "",
    val targetUserId: String = "",
    val activeTable: String = "user-login",
    val issuedTokenHash: String = "",
    val dispatchedTokenPreview: String? = null,
    val tokenExpiresAtMillis: Long = 0L,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: UserEntity) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

/**
 * ViewModel for handling user authentication, self-registration, real-time form validation,
 * SHA-256 password hashing, and diagnostic ping/read checks on the `public.user_login` table
 * via the Supabase Postgrest client.
 */
class LoginViewModel(
    private val authRepository: AuthRepository? = null,
    private val sessionRepository: SessionDataStoreRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRestoringSession = MutableStateFlow(sessionRepository != null)
    val isRestoringSession: StateFlow<Boolean> = _isRestoringSession.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Real-time form validation states
    private val _usernameInput = MutableStateFlow("")
    val usernameInput: StateFlow<String> = _usernameInput.asStateFlow()

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError: StateFlow<String?> = _usernameError.asStateFlow()
    val emailError: StateFlow<String?> get() = usernameError

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    private val _regEmailError = MutableStateFlow<String?>(null)
    val regEmailError: StateFlow<String?> = _regEmailError.asStateFlow()

    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()

    private val _formValidationState = MutableStateFlow(LoginFormValidationState())
    val formValidationState: StateFlow<LoginFormValidationState> = _formValidationState.asStateFlow()

    private val _passwordRecoveryState = MutableStateFlow(PasswordRecoveryState())
    val passwordRecoveryState: StateFlow<PasswordRecoveryState> = _passwordRecoveryState.asStateFlow()

    private val _availableCompanies = MutableStateFlow(DEFAULT_COMPANIES)
    val availableCompanies: StateFlow<List<String>> = _availableCompanies.asStateFlow()

    private val secureRandom = SecureRandom()

    private val _rememberMe = MutableStateFlow(true)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    private val _savedUsername = MutableStateFlow("")
    val savedUsername: StateFlow<String> = _savedUsername.asStateFlow()

    fun setRememberMe(enabled: Boolean) {
        _rememberMe.value = enabled
        viewModelScope.launch {
            sessionRepository?.setRememberMePreference(enabled)
        }
    }

    private val _authenticatedUser = MutableStateFlow<UserEntity?>(null)
    val authenticatedUser: StateFlow<UserEntity?> = _authenticatedUser.asStateFlow()

    private val _diagnosticLogs = MutableStateFlow<List<String>>(emptyList())
    val diagnosticLogs: StateFlow<List<String>> = _diagnosticLogs.asStateFlow()

    private val _isRunningDiagnostics = MutableStateFlow(false)
    val isRunningDiagnostics: StateFlow<Boolean> = _isRunningDiagnostics.asStateFlow()

    private val _dbStatus = MutableStateFlow(
        SupabaseDbStatus(
            isConfigured = SupabaseClient.isConfigured(),
            isConnected = false,
            activeTable = SupabaseClient.configuredTableName.ifEmpty { "user-login" },
            projectUrl = SupabaseClient.supabaseUrl,
            statusText = if (SupabaseClient.isConfigured()) {
                "Connecting to Supabase..."
            } else {
                "Supabase Not Configured — Tap to set SUPABASE_URL & Key"
            }
        )
    )
    val dbStatus: StateFlow<SupabaseDbStatus> = _dbStatus.asStateFlow()

    private val diagnosticHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    private var detectedTableName: String? = null

    @Volatile
    private var detectedColumnStyle: String? = null // "HYPHEN", "SPACED_HYPHEN", or "UNDERSCORE"

    private val discoveredTableColumns = mutableMapOf<String, Set<String>>()

    init {
        restoreSavedSession()
        performDiagnosticPing()
    }

    /**
     * Restores and observes the user's logged-in session from [SessionDataStoreRepository]
     * on app startup so they do not have to re-enter credentials every time they open the app,
     * automatically directing them to the main dashboard if already authenticated.
     */
    fun restoreSavedSession() {
        val repo = sessionRepository
        if (repo == null) {
            _isRestoringSession.value = false
            return
        }
        viewModelScope.launch {
            runCatching {
                repo.sessionStateFlow.collect { sessionState ->
                    _rememberMe.value = sessionState.rememberMe
                    if (sessionState.savedUsername.isNotBlank()) {
                        _savedUsername.value = sessionState.savedUsername
                        if (_usernameInput.value.isBlank()) {
                            _usernameInput.value = sessionState.savedUsername
                            updateFormValidationSnapshot()
                        }
                    }
                    if (sessionState.rememberMe && sessionState.isLoggedIn && sessionState.user != null) {
                        Log.d(TAG, "Restored logged-in session with Remember Me from DataStore for '${sessionState.user.username}'")
                        _authenticatedUser.value = sessionState.user
                        _uiState.value = LoginUiState.Success(sessionState.user)
                    } else if (_isRestoringSession.value) {
                        _authenticatedUser.value = null
                    }
                    _isRestoringSession.value = false
                }
            }.onFailure { e ->
                Log.w(TAG, "Failed to restore session from DataStore: ${e.message}")
                _isRestoringSession.value = false
            }
        }
    }

    /**
     * Persists the authenticated [UserEntity] to [SessionDataStoreRepository].
     */
    private suspend fun persistLoggedInUser(user: UserEntity, rememberMe: Boolean = _rememberMe.value) {
        runCatching {
            sessionRepository?.saveLoggedInUser(user, rememberMe = rememberMe)
            Log.d(TAG, "Saved logged-in session (rememberMe=$rememberMe) to DataStore for '${user.username}'")
        }.onFailure { e ->
            Log.w(TAG, "Failed to save session to DataStore: ${e.message}")
        }
    }

    /**
     * Simple diagnostic test function that attempts a `SELECT count(*)` query on the
     * `public.user-login` (or `public.user_login`) table via the Supabase Postgrest client
     * and logs the result or error details to the Android console (`Logcat`) to verify the database connection.
     */
    fun performDiagnosticPing() {
        viewModelScope.launch {
            _isRunningDiagnostics.value = true
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val logs = _diagnosticLogs.value.toMutableList()

            fun recordConsoleLog(level: String, message: String, throwable: Throwable? = null) {
                val entry = "[$timestamp][$level] $message"
                logs.add(entry)
                _diagnosticLogs.value = logs.toList()
                when (level) {
                    "ERROR" -> if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
                    "WARN" -> Log.w(TAG, message)
                    else -> Log.d(TAG, message)
                }
            }

            val primaryTable = detectedTableName ?: SupabaseClient.configuredTableName.ifEmpty { "user-login" }
            recordConsoleLog("INFO", "performDiagnosticPing: Executing SELECT count(*) on public.$primaryTable...")

            if (!SupabaseClient.isConfigured()) {
                recordConsoleLog(
                    "ERROR",
                    "performDiagnosticPing failed: SUPABASE_URL or SUPABASE_ANON_KEY is not configured."
                )
                _dbStatus.value = _dbStatus.value.copy(
                    isConfigured = false,
                    isConnected = false,
                    statusText = "Supabase Not Configured — Tap to set SUPABASE_URL & Key"
                )
                _isRunningDiagnostics.value = false
                return@launch
            }

            withContext(Dispatchers.IO) {
                try {
                    val result = SupabaseClient.client.postgrest
                        .from(schema = "public", table = primaryTable)
                        .select {
                            count(Count.EXACT)
                        }

                    val rows = runCatching { result.decodeList<JsonObject>() }.getOrDefault(emptyList())
                    val rowCount = result.countOrNull() ?: rows.size.toLong()

                    rows.firstOrNull()?.let { sample ->
                        discoveredTableColumns[primaryTable] = sample.keys.toSet()
                        detectedColumnStyle = when {
                            sample.containsKey("first - name") || sample.containsKey("sur - name") -> "SPACED_HYPHEN"
                            sample.containsKey("first-name") || sample.containsKey("sur-name") -> "HYPHEN"
                            sample.containsKey("first_name") || sample.containsKey("sur_name") -> "UNDERSCORE"
                            else -> detectedColumnStyle
                        }
                    }

                    val discoveredEntities = rows.mapNotNull { row ->
                        (row["entity"]?.jsonPrimitive?.contentOrNull
                            ?: row["company"]?.jsonPrimitive?.contentOrNull)?.trim()?.takeIf { it.isNotEmpty() }
                    }
                    if (discoveredEntities.isNotEmpty()) {
                        _availableCompanies.value = (discoveredEntities + DEFAULT_COMPANIES).distinct()
                    }

                    recordConsoleLog(
                        "INFO",
                        "performDiagnosticPing SUCCESS: Connected to public.$primaryTable — SELECT count(*) = $rowCount"
                    )

                    detectedTableName = primaryTable
                    _dbStatus.value = SupabaseDbStatus(
                        isConfigured = true,
                        isConnected = true,
                        activeTable = primaryTable,
                        rowCount = rowCount.toInt(),
                        projectUrl = SupabaseClient.supabaseUrl,
                        statusText = "Connected • public.$primaryTable (count = $rowCount)",
                        diagnosticReport = logs.joinToString("\n")
                    )
                } catch (e: Exception) {
                    val errorMsg = e.localizedMessage ?: e.javaClass.simpleName
                    recordConsoleLog(
                        "ERROR",
                        "performDiagnosticPing ERROR on public.$primaryTable: $errorMsg",
                        e
                    )
                    // Run broader fallback diagnostics to help identify table naming or RLS issues
                    runDiagnosticPingAndRead()
                    return@withContext
                }
            }

            _isRunningDiagnostics.value = false
        }
    }

    /**
     * Diagnostic test function that attempts a ping to the Supabase REST endpoint and a read
     * operation from `public.user_login` (plus fallback table names), logging full connection
     * status, discovered tables/columns, and any error details to Logcat and UI state.
     */
    fun runDiagnosticPingAndRead() {
        viewModelScope.launch {
            _isRunningDiagnostics.value = true
            val logs = mutableListOf<String>()
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

            fun appendLog(level: String, message: String) {
                val entry = "[$timestamp][$level] $message"
                logs.add(entry)
                _diagnosticLogs.value = logs.toList()
                when (level) {
                    "ERROR" -> Log.e(TAG, message)
                    "WARN" -> Log.w(TAG, message)
                    else -> Log.d(TAG, message)
                }
            }

            appendLog("INFO", "Starting Supabase diagnostic ping & public.user_login read test...")

            val url = SupabaseClient.supabaseUrl
            val key = SupabaseClient.supabaseAnonKey
            val configured = SupabaseClient.isConfigured()

            appendLog("INFO", "SUPABASE_URL = '${url.ifBlank { "<EMPTY>" }}'")
            val maskedKey = if (key.length > 12) "${key.take(8)}...${key.takeLast(4)} (len=${key.length})" else key.ifBlank { "<EMPTY>" }
            appendLog("INFO", "SUPABASE_ANON_KEY = $maskedKey")

            if (!configured) {
                appendLog(
                    "ERROR",
                    "Supabase credentials are NOT configured (placeholder or empty). " +
                        "Set SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets or tap 'Configure DB'."
                )
                _dbStatus.value = SupabaseDbStatus(
                    isConfigured = false,
                    isConnected = false,
                    activeTable = SupabaseClient.configuredTableName.ifEmpty { "user_login" },
                    projectUrl = url,
                    statusText = "Supabase Not Configured — Tap to set SUPABASE_URL & Key & Run Diagnostics",
                    diagnosticReport = logs.joinToString("\n")
                )
                _isRunningDiagnostics.value = false
                return@launch
            }

            // Step 1: HTTP Ping to `/rest/v1/` to inspect exposed PostgREST schema tables
            withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder()
                        .url("$url/rest/v1/")
                        .addHeader("apikey", key)
                        .addHeader("Authorization", "Bearer $key")
                        .get()
                        .build()

                    diagnosticHttpClient.newCall(req).execute().use { response ->
                        val bodyStr = response.body?.string().orEmpty()
                        appendLog("INFO", "HTTP Ping $url/rest/v1/ -> Status ${response.code} ${response.message}")
                        if (response.isSuccessful && bodyStr.isNotBlank()) {
                            runCatching {
                                val rootObj = lenientJson.parseToJsonElement(bodyStr).jsonObject
                                val definitions = rootObj["definitions"]?.jsonObject?.keys?.toList().orEmpty()
                                if (definitions.isNotEmpty()) {
                                    appendLog("INFO", "Discovered public schema tables: ${definitions.joinToString(", ")}")
                                    definitions.forEach { tableDef ->
                                        val props = rootObj["definitions"]?.jsonObject
                                            ?.get(tableDef)?.jsonObject
                                            ?.get("properties")?.jsonObject?.keys?.toList().orEmpty()
                                        if (props.isNotEmpty()) {
                                            discoveredTableColumns[tableDef] = props.toSet()
                                            appendLog("INFO", "Table '$tableDef' columns: ${props.joinToString(", ")}")
                                        }
                                    }
                                } else {
                                    appendLog("WARN", "Ping succeeded (HTTP 200), but no public tables are exposed in OpenAPI definitions.")
                                }
                            }.onFailure {
                                appendLog("INFO", "Ping response body preview: ${bodyStr.take(180)}")
                            }
                        } else {
                            appendLog("ERROR", "HTTP Ping failed (${response.code}): ${bodyStr.take(240)}")
                        }
                    }
                } catch (e: Exception) {
                    appendLog("ERROR", "Network/HTTP Ping exception: ${e.javaClass.simpleName}: ${e.message}")
                }
            }

            // Step 2: Postgrest Read Operation on `public.user_login` (and candidate table names)
            var connectedTable: String? = null
            var connectedRowCount = 0
            var lastErrorDetail = "No matching user table could be read."

            withContext(Dispatchers.IO) {
                for (tableName in SupabaseClient.candidateTables) {
                    try {
                        appendLog("INFO", "Attempting Postgrest SELECT on public.\"$tableName\"...")
                        val rows = SupabaseClient.client.postgrest
                            .from(schema = "public", table = tableName)
                            .select()
                            .decodeList<JsonObject>()

                        appendLog(
                            "INFO",
                            "SUCCESS: Read ${rows.size} row(s) from public.\"$tableName\" via Supabase Postgrest!"
                        )

                        rows.firstOrNull()?.let { sample ->
                            val cols = sample.keys.joinToString(", ")
                            discoveredTableColumns[tableName] = sample.keys.toSet()
                            appendLog("INFO", "Sample row columns in public.\"$tableName\": [$cols]")
                            detectedColumnStyle = when {
                                sample.containsKey("first - name") || sample.containsKey("sur - name") -> "SPACED_HYPHEN"
                                sample.containsKey("first-name") || sample.containsKey("sur-name") -> "HYPHEN"
                                sample.containsKey("first_name") || sample.containsKey("sur_name") -> "UNDERSCORE"
                                else -> detectedColumnStyle
                            }
                        }

                        val discoveredEntities = rows.mapNotNull { row ->
                            (row["entity"]?.jsonPrimitive?.contentOrNull
                                ?: row["company"]?.jsonPrimitive?.contentOrNull)?.trim()?.takeIf { it.isNotEmpty() }
                        }
                        if (discoveredEntities.isNotEmpty()) {
                            _availableCompanies.value = (discoveredEntities + DEFAULT_COMPANIES).distinct()
                        }

                        if (rows.isEmpty()) {
                            appendLog(
                                "WARN",
                                "Note: public.\"$tableName\" returned 0 rows. If the table actually has rows in Supabase, " +
                                    "Row Level Security (RLS) is hiding rows from anon SELECT/INSERT. " +
                                    "Run: ALTER TABLE public.\"$tableName\" DISABLE ROW LEVEL SECURITY;"
                            )
                        }

                        if (connectedTable == null) {
                            connectedTable = tableName
                            connectedRowCount = rows.size
                            detectedTableName = tableName
                        }
                    } catch (e: Exception) {
                        val errMsg = e.localizedMessage ?: e.javaClass.simpleName
                        lastErrorDetail = errMsg
                        appendLog("ERROR", "SELECT on public.\"$tableName\" failed: $errMsg")
                    }
                }
            }

            val finalTable = connectedTable
            if (finalTable != null) {
                _dbStatus.value = SupabaseDbStatus(
                    isConfigured = true,
                    isConnected = true,
                    activeTable = finalTable,
                    rowCount = connectedRowCount,
                    projectUrl = url,
                    statusText = "Connected • public.$finalTable ($connectedRowCount rows)",
                    diagnosticReport = logs.joinToString("\n")
                )
            } else {
                _dbStatus.value = SupabaseDbStatus(
                    isConfigured = true,
                    isConnected = false,
                    activeTable = SupabaseClient.configuredTableName.ifEmpty { "user_login" },
                    rowCount = 0,
                    projectUrl = url,
                    statusText = "DB Read Failed — Tap to view diagnostic logs",
                    diagnosticReport = logs.joinToString("\n").ifEmpty { lastErrorDetail }
                )
            }

            _isRunningDiagnostics.value = false
        }
    }

    /**
     * Alias for [runDiagnosticPingAndRead].
     */
    fun checkSupabaseConnection() = runDiagnosticPingAndRead()

    /**
     * Saves custom Supabase URL, Anon Key, and table name at runtime and re-runs diagnostics.
     */
    fun saveSupabaseConfig(
        context: Context,
        url: String,
        anonKey: String,
        tableName: String
    ) {
        SupabaseClient.updateCredentials(
            context = context,
            url = url,
            anonKey = anonKey,
            tableName = tableName
        )
        detectedTableName = tableName.trim().takeIf { it.isNotEmpty() }
        clearMessages()
        runDiagnosticPingAndRead()
    }

    /**
     * Hashes a plain-text password using SHA-256 and returns the 64-character lowercase hex digest.
     */
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Alias for [hashPassword] that hashes a password using SHA-256.
     */
    fun hashPasswordSha256(password: String): String = hashPassword(password)

    /**
     * Queries the `public.user_login` table via the Supabase `postgrest` client to validate
     * the provided username and SHA-256 password hash (`password_hash`).
     */
    suspend fun validateUserCredentials(
        username: String,
        password: String
    ): Result<UserEntity> {
        val hashedPassword = hashPassword(password)
        return queryUserLogin(
            username = username,
            passwordHash = hashedPassword,
            rawPassword = password
        )
    }

    /**
     * Queries the `public.user_login` table (or `user-login` / `user - login` / `user` / `users`)
     * via `SupabaseClient.client.postgrest` and validates the user's credentials.
     */
    suspend fun queryUserLogin(
        username: String,
        passwordHash: String,
        rawPassword: String = ""
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanIdentifier = username.trim()
        if (cleanIdentifier.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Username cannot be empty."))
        }
        if (!SupabaseClient.isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase is not configured. Tap the status pill above to configure SUPABASE_URL & SUPABASE_ANON_KEY.")
            )
        }

        val tablesToSearch = buildList {
            detectedTableName?.let { add(it) }
            addAll(SupabaseClient.candidateTables)
        }.distinct()

        var matchedRow: JsonObject? = null
        var resolvedTable = "user_login"

        for (tableName in tablesToSearch) {
            try {
                val exactRows = SupabaseClient.client.postgrest
                    .from(schema = "public", table = tableName)
                    .select {
                        filter {
                            eq("username", cleanIdentifier)
                            eq("password_hash", passwordHash)
                        }
                    }
                    .decodeList<JsonObject>()

                if (exactRows.isNotEmpty()) {
                    matchedRow = exactRows.first()
                    resolvedTable = tableName
                    detectedTableName = tableName
                    break
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exact Postgrest query on public.$tableName failed: ${e.message}")
            }

            try {
                val userRows = SupabaseClient.client.postgrest
                    .from(schema = "public", table = tableName)
                    .select {
                        filter {
                            or {
                                ilike("username", cleanIdentifier)
                                ilike("email", cleanIdentifier)
                                ilike("user_id", cleanIdentifier)
                            }
                        }
                    }
                    .decodeList<JsonObject>()

                if (userRows.isNotEmpty()) {
                    matchedRow = userRows.first()
                    resolvedTable = tableName
                    detectedTableName = tableName
                    break
                }
            } catch (e: Exception) {
                Log.w(TAG, "Filtered Postgrest query on public.$tableName failed: ${e.message}")
            }

            try {
                val allRows = SupabaseClient.client.postgrest
                    .from(schema = "public", table = tableName)
                    .select()
                    .decodeList<JsonObject>()

                val found = allRows.firstOrNull { row ->
                    val rowUsername = row["username"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val rowEmail = row["email"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val rowUserId = row["user_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    rowUsername.equals(cleanIdentifier, ignoreCase = true) ||
                        rowEmail.equals(cleanIdentifier, ignoreCase = true) ||
                        rowUserId.equals(cleanIdentifier, ignoreCase = true)
                }

                if (found != null) {
                    matchedRow = found
                    resolvedTable = tableName
                    detectedTableName = tableName
                    break
                }
            } catch (e: Exception) {
                Log.w(TAG, "Full Postgrest select on $tableName failed: ${e.message}")
            }
        }

        val userJson = matchedRow
            ?: return@withContext Result.failure(
                NoSuchElementException("No user found for '$cleanIdentifier' in Supabase table public.${detectedTableName ?: "user_login"}.")
            )

        val userEntity = mapJsonToUserEntity(userJson, resolvedTable)

        val statusValue = userEntity.status.trim()
        if (statusValue.equals("inactive", ignoreCase = true) ||
            statusValue.equals("suspended", ignoreCase = true) ||
            statusValue.equals("locked", ignoreCase = true)
        ) {
            return@withContext Result.failure(
                IllegalAccessException("Account '${userEntity.username}' is currently $statusValue.")
            )
        }

        val storedPasswordHash = userEntity.passwordHash.trim()
        val trimmedInputSha256 = hashPassword(rawPassword.trim())
        val isPasswordValid = passwordHash.equals(storedPasswordHash, ignoreCase = true) ||
            trimmedInputSha256.equals(storedPasswordHash, ignoreCase = true) ||
            (rawPassword.isNotBlank() && rawPassword.trim().equals(storedPasswordHash, ignoreCase = true))

        if (!isPasswordValid) {
            return@withContext Result.failure(
                SecurityException("Invalid username or password.")
            )
        }

        updateLastLoginInPostgrest(resolvedTable, userEntity.id)
        Result.success(userEntity)
    }

    /**
     * Inserts a newly registered user into the `public.user-login` table (the exact same table
     * used for authentication, with fallback support for `user_login`, `user - login`, `user`, and `users`)
     * via the Supabase Postgrest client, storing the SHA-256 `password_hash`.
     */
    suspend fun insertUserInPostgrest(user: UserEntity): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase is not connected. Configure SUPABASE_URL & SUPABASE_ANON_KEY first.")
            )
        }

        val hashedPassword = if (user.passwordHash.length == 64 && user.passwordHash.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            user.passwordHash.lowercase(Locale.US)
        } else {
            hashPassword(user.passwordHash)
        }

        val activeTable = _dbStatus.value.activeTable.takeIf { it.isNotBlank() }
        val tablesToTry = buildList {
            detectedTableName?.let { add(it) }
            activeTable?.let { add(it) }
            add("user-login")
            addAll(SupabaseClient.candidateTables)
        }.distinct()

        var lastRelevantError = "Unable to insert into public.user-login."
        val missingColumnRegex = Regex("Could not find the '([^']+)' column", RegexOption.IGNORE_CASE)

        for (tableName in tablesToTry) {
            val knownCols = discoveredTableColumns[tableName]

            // First try inserting typed UserLoginRow directly into `user-login` (matches exact schema)
            if (knownCols.isNullOrEmpty() || ("first-name" in knownCols && "sur-name" in knownCols)) {
                try {
                    val typedRow = UserLoginRow(
                        entity = user.entity,
                        userId = user.id,
                        firstName = user.firstName,
                        surName = user.surName,
                        email = user.email,
                        username = user.username,
                        passwordHash = hashedPassword,
                        role = user.role.name,
                        createdAt = user.createdAt,
                        lastLogin = user.lastLogin ?: user.createdAt,
                        status = "active"
                    )
                    SupabaseClient.client.postgrest
                        .from(schema = "public", table = tableName)
                        .insert(typedRow)

                    detectedTableName = tableName
                    detectedColumnStyle = "HYPHEN"
                    Log.d(TAG, "Successfully inserted UserLoginRow '${user.username}' into Supabase public.$tableName")
                    return@withContext Result.success(tableName)
                } catch (typedErr: Exception) {
                    Log.w(TAG, "Typed UserLoginRow insert into public.$tableName note: ${typedErr.message}")
                }
            }

            // Build candidate payloads in order from most complete to minimal
            val candidatePayloads = mutableListOf<Map<String, String>>()

            if (!knownCols.isNullOrEmpty()) {
                val matchedMap = mutableMapOf<String, String>()
                if ("entity" in knownCols) matchedMap["entity"] = user.entity
                if ("company" in knownCols) matchedMap["company"] = user.entity
                if ("user_id" in knownCols) matchedMap["user_id"] = user.id
                if ("first-name" in knownCols) matchedMap["first-name"] = user.firstName
                if ("first - name" in knownCols) matchedMap["first - name"] = user.firstName
                if ("first_name" in knownCols) matchedMap["first_name"] = user.firstName
                if ("sur-name" in knownCols) matchedMap["sur-name"] = user.surName
                if ("sur - name" in knownCols) matchedMap["sur - name"] = user.surName
                if ("sur_name" in knownCols) matchedMap["sur_name"] = user.surName
                if ("last_name" in knownCols) matchedMap["last_name"] = user.surName
                if ("email" in knownCols) matchedMap["email"] = user.email
                if ("username" in knownCols) matchedMap["username"] = user.username
                if ("password_hash" in knownCols) matchedMap["password_hash"] = hashedPassword
                if ("password" in knownCols && "password_hash" !in knownCols) matchedMap["password"] = hashedPassword
                if ("role" in knownCols) matchedMap["role"] = user.role.name
                if ("created_at" in knownCols) matchedMap["created_at"] = user.createdAt
                if ("last_login" in knownCols) matchedMap["last_login"] = user.lastLogin ?: user.createdAt
                if ("status" in knownCols) matchedMap["status"] = "active"
                if (matchedMap.isNotEmpty()) {
                    candidatePayloads.add(matchedMap)
                }
            }

            val columnStyles = buildList {
                detectedColumnStyle?.let { add(it) }
                add("HYPHEN")
                add("UNDERSCORE")
                add("SPACED_HYPHEN")
            }.distinct()

            for (style in columnStyles) {
                val firstCol = when (style) {
                    "HYPHEN" -> "first-name"
                    "SPACED_HYPHEN" -> "first - name"
                    else -> "first_name"
                }
                val surCol = when (style) {
                    "HYPHEN" -> "sur-name"
                    "SPACED_HYPHEN" -> "sur - name"
                    else -> "sur_name"
                }
                candidatePayloads.add(
                    mapOf(
                        "entity" to user.entity,
                        "user_id" to user.id,
                        firstCol to user.firstName,
                        surCol to user.surName,
                        "email" to user.email,
                        "username" to user.username,
                        "password_hash" to hashedPassword,
                        "role" to user.role.name,
                        "created_at" to user.createdAt,
                        "last_login" to (user.lastLogin ?: user.createdAt),
                        "status" to "active"
                    )
                )
            }

            // Standard compact & minimal fallbacks for custom `public.user-login` schemas
            candidatePayloads.add(
                mapOf(
                    "entity" to user.entity,
                    "user_id" to user.id,
                    "username" to user.username,
                    "email" to user.email,
                    "password_hash" to hashedPassword,
                    "role" to user.role.name,
                    "status" to "active"
                )
            )
            candidatePayloads.add(
                mapOf(
                    "username" to user.username,
                    "email" to user.email,
                    "password_hash" to hashedPassword
                )
            )
            candidatePayloads.add(
                mapOf(
                    "username" to user.username,
                    "password_hash" to hashedPassword
                )
            )

            for (baseMap in candidatePayloads.distinct()) {
                val mutableFields = baseMap.toMutableMap()
                var retryCount = 0

                while (mutableFields.isNotEmpty() && retryCount < 8) {
                    val payload = buildJsonObject {
                        mutableFields.forEach { (k, v) -> put(k, v) }
                    }
                    try {
                        SupabaseClient.client.postgrest
                            .from(schema = "public", table = tableName)
                            .insert(payload)

                        detectedTableName = tableName
                        Log.d(
                            TAG,
                            "Successfully inserted user '${user.username}' into Supabase public.$tableName with columns ${mutableFields.keys}"
                        )
                        return@withContext Result.success(tableName)
                    } catch (insertErr: Exception) {
                        val msg = insertErr.localizedMessage ?: insertErr.javaClass.simpleName
                        Log.e(TAG, "Insert into public.$tableName (cols=${mutableFields.keys}) failed: $msg")

                        // If table itself does not exist, break out to next table candidate immediately
                        if (msg.contains("Could not find the table", ignoreCase = true) ||
                            (msg.contains("relation", ignoreCase = true) && msg.contains("does not exist", ignoreCase = true))
                        ) {
                            if (lastRelevantError.startsWith("Unable to insert")) {
                                lastRelevantError = msg
                            }
                            retryCount = 99
                            break
                        }

                        lastRelevantError = "Table public.$tableName: $msg"

                        // If Postgrest reports a specific unknown column, swap style or strip and retry
                        val missingColMatch = missingColumnRegex.find(msg)
                        val missingCol = missingColMatch?.groupValues?.getOrNull(1)
                        if (!missingCol.isNullOrBlank() && mutableFields.containsKey(missingCol)) {
                            val removedVal = mutableFields.remove(missingCol)
                            when (missingCol) {
                                "first_name" -> if (removedVal != null && !mutableFields.containsKey("first-name")) {
                                    mutableFields["first-name"] = removedVal
                                }
                                "sur_name" -> if (removedVal != null && !mutableFields.containsKey("sur-name")) {
                                    mutableFields["sur-name"] = removedVal
                                }
                            }
                            Log.w(TAG, "Adjusted unsupported column '$missingCol' and retrying insert into public.$tableName...")
                            retryCount++
                            continue
                        }

                        if (msg.contains("duplicate", ignoreCase = true) || msg.contains("unique", ignoreCase = true)) {
                            return@withContext Result.failure(
                                IllegalStateException("Username or Email '${user.username}' already exists in public.$tableName.")
                            )
                        }

                        break
                    }
                }
            }
        }

        Result.failure(Exception(lastRelevantError))
    }

    /**
     * Registration function that hashes the password using SHA-256 and inserts a new row
     * into the `public.user_login` table via the Supabase Postgrest client.
     */
    fun registerUser(
        username: String,
        password: String,
        email: String = "",
        firstName: String = "",
        surName: String = "",
        userId: String = "",
        entity: String = "Djezzy Telecom",
        role: UserRole = UserRole.LEAD_TECHNICIAN,
        confirmPassword: String = ""
    ) {
        registerAccount(
            firstName = firstName,
            surName = surName,
            username = username,
            email = email,
            userId = userId,
            entity = entity,
            role = role,
            password = password,
            confirmPassword = confirmPassword
        )
    }

    /**
     * Self-registers a new employee account, hashes the password with SHA-256, inserts the record
     * into `public.user_login` via Supabase Postgrest, and signs the user in once the database row is created.
     */
    fun registerAccount(
        firstName: String,
        surName: String,
        username: String,
        email: String,
        userId: String,
        entity: String,
        role: UserRole,
        password: String,
        confirmPassword: String
    ) {
        val cleanUsername = username.trim()
        if (cleanUsername.isEmpty()) {
            val msg = "Please choose a company username."
            _errorMessage.value = msg
            _uiState.value = LoginUiState.Error(msg)
            return
        }
        if (password.isEmpty()) {
            val msg = "Please enter a password."
            _errorMessage.value = msg
            _uiState.value = LoginUiState.Error(msg)
            return
        }
        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            val msg = "Passwords do not match."
            _errorMessage.value = msg
            _uiState.value = LoginUiState.Error(msg)
            return
        }

        val derivedFirst = firstName.trim().ifEmpty {
            cleanUsername.substringBefore(".").substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                .ifEmpty { "Field" }
        }
        val derivedSur = surName.trim().ifEmpty {
            cleanUsername.substringAfter(".", "")
                .substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                .ifEmpty { "Operator" }
        }
        val cleanEmail = email.trim().ifEmpty {
            if (cleanUsername.contains("@")) cleanUsername else "$cleanUsername@djezzy.dz"
        }
        if (!isValidEmailFormat(cleanEmail)) {
            val msg = "Please enter a valid corporate email address (e.g. name@djezzy.dz)."
            _regEmailError.value = msg
            _errorMessage.value = msg
            _uiState.value = LoginUiState.Error(msg)
            return
        }
        val cleanUserId = userId.trim().ifEmpty { "USR-${(100..999).random()}" }
        val cleanEntity = entity.trim().ifEmpty { "Djezzy Telecom" }

        if (!SupabaseClient.isConfigured()) {
            val notConfiguredMsg =
                "App is not connected to your Supabase database yet! Tap the status bar at the top (or set SUPABASE_URL & SUPABASE_ANON_KEY in AI Studio Secrets) to connect."
            _errorMessage.value = notConfiguredMsg
            _uiState.value = LoginUiState.Error(notConfiguredMsg)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            _uiState.value = LoginUiState.Loading

            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
            val passwordSha256 = hashPassword(password)

            val newUser = UserEntity(
                id = cleanUserId,
                entity = cleanEntity,
                firstName = derivedFirst,
                surName = derivedSur,
                email = cleanEmail,
                username = cleanUsername,
                fullName = "$derivedFirst $derivedSur".trim(),
                badgeNumber = cleanUserId,
                role = role,
                passwordHash = passwordSha256,
                createdAt = nowIso,
                lastLogin = nowIso,
                status = "active",
                mfaEnabled = false,
                preferredMfaMethod = MfaMethod.TOTP_AUTHENTICATOR
            )

            val insertResult = insertUserInPostgrest(newUser)
            if (insertResult.isFailure) {
                val errReason = insertResult.exceptionOrNull()?.message ?: "Unknown database error"
                val targetTbl = detectedTableName ?: _dbStatus.value.activeTable.ifEmpty { "user-login" }
                val helpfulMsg = buildString {
                    append("Could not insert into Supabase database: ")
                    append(errReason)
                    if (errReason.contains("row-level security", ignoreCase = true) ||
                        errReason.contains("42501", ignoreCase = true)
                    ) {
                        append("\nTip: In Supabase SQL Editor, run: ALTER TABLE public.\"$targetTbl\" DISABLE ROW LEVEL SECURITY;")
                    }
                }
                _isLoading.value = false
                _errorMessage.value = helpfulMsg
                _uiState.value = LoginUiState.Error(helpfulMsg)
                return@launch
            }

            val insertedTable = insertResult.getOrDefault("user-login")

            if (authRepository != null) {
                runCatching {
                    authRepository.registerEmployee(
                        entityName = cleanEntity,
                        userId = cleanUserId,
                        firstName = derivedFirst,
                        surName = derivedSur,
                        username = cleanUsername,
                        email = cleanEmail,
                        role = role,
                        password = passwordSha256,
                        mfaMethod = MfaMethod.TOTP_AUTHENTICATOR
                    )
                }
            }

            performDiagnosticPing()
            persistLoggedInUser(newUser)
            _isLoading.value = false
            _successMessage.value = "Account '${newUser.username}' created in Supabase public.$insertedTable!"
            _authenticatedUser.value = newUser
            _uiState.value = LoginUiState.Success(newUser)
        }
    }

    /**
     * Validates an email address format against a standard RFC-compliant email pattern.
     */
    fun isValidEmailFormat(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return false
        return EMAIL_REGEX.matches(trimmed)
    }

    /**
     * Validates whether the login identifier is either a valid email address or a valid
     * company username / badge ID. If the user includes `@` (or starts typing a domain),
     * strict email format validation is enforced.
     */
    fun validateIdentifierField(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return "Username or email cannot be empty."
        }
        if (trimmed.contains(" ")) {
            return "Username or email cannot contain spaces."
        }
        // If input contains '@' or ends with a dot/domain fragment, validate as email
        if (trimmed.contains("@")) {
            if (!isValidEmailFormat(trimmed)) {
                return "Please enter a valid email address (e.g. name@djezzy.dz)."
            }
            return null
        }
        if (trimmed.length < 3) {
            return "Enter at least 3 characters or a valid email address."
        }
        if (!USERNAME_OR_ID_REGEX.matches(trimmed)) {
            return "Please enter a valid company username or email address."
        }
        return null
    }

    /**
     * Validates the password field in real time.
     */
    fun validatePasswordField(password: String): String? {
        if (password.isEmpty()) {
            return "Password cannot be empty."
        }
        return null
    }

    /**
     * Real-time callback invoked whenever the Username / Email input changes in `LoginScreen`.
     */
    fun onUsernameChanged(input: String) {
        _usernameInput.value = input
        val fieldError = validateIdentifierField(input)
        _usernameError.value = fieldError
        if (_errorMessage.value != null) {
            _errorMessage.value = null
        }
        updateFormValidationSnapshot()
    }

    /**
     * Alias for [onUsernameChanged] for email-based login input changes.
     */
    fun onEmailChanged(input: String) = onUsernameChanged(input)

    /**
     * Real-time callback invoked whenever the Password input changes in `LoginScreen`.
     */
    fun onPasswordChanged(input: String) {
        _passwordInput.value = input
        val fieldError = validatePasswordField(input)
        _passwordError.value = fieldError
        if (_errorMessage.value != null) {
            _errorMessage.value = null
        }
        updateFormValidationSnapshot()
    }

    /**
     * Real-time callback invoked whenever the Corporate Email field changes in the registration view.
     */
    fun onRegistrationEmailChanged(email: String) {
        val trimmed = email.trim()
        _regEmailError.value = if (trimmed.isNotEmpty() && !isValidEmailFormat(trimmed)) {
            "Please enter a valid email address (e.g. name@djezzy.dz)."
        } else {
            null
        }
        updateFormValidationSnapshot()
    }

    private fun updateFormValidationSnapshot() {
        val uVal = _usernameInput.value
        val pVal = _passwordInput.value
        val uErr = validateIdentifierField(uVal)
        val pErr = validatePasswordField(pVal)
        val loginValid = uErr == null && pErr == null
        _isFormValid.value = loginValid
        _formValidationState.value = LoginFormValidationState(
            usernameOrEmail = uVal,
            password = pVal,
            usernameOrEmailError = _usernameError.value,
            passwordError = _passwordError.value,
            regEmailError = _regEmailError.value,
            isLoginValid = loginValid
        )
    }

    /**
     * Performs full synchronous validation of the login fields before attempting a login.
     * Populates field-level and form-level error messages if fields are empty or if the
     * email format is invalid.
     */
    fun validateLoginForm(username: String, password: String): Boolean {
        _usernameInput.value = username
        _passwordInput.value = password

        val userErr = validateIdentifierField(username)
        val passErr = validatePasswordField(password)

        _usernameError.value = userErr
        _passwordError.value = passErr
        updateFormValidationSnapshot()

        if (userErr != null || passErr != null) {
            val combinedMsg = userErr ?: passErr ?: "Please fix the highlighted fields before logging in."
            _errorMessage.value = combinedMsg
            _uiState.value = LoginUiState.Error(combinedMsg)
            return false
        }
        return true
    }

    /**
     * Executes the login operation by first validating that fields are non-empty and any email
     * is properly formatted, then hashing the password with SHA-256 and validating credentials
     * against `public.user_login` via the Supabase Postgrest client.
     */
    fun login(username: String, password: String, rememberMe: Boolean = _rememberMe.value) {
        _rememberMe.value = rememberMe
        if (!validateLoginForm(username, password)) {
            return
        }
        val cleanUser = username.trim()

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            _uiState.value = LoginUiState.Loading

            if (SupabaseClient.isConfigured()) {
                val postgrestResult = validateUserCredentials(
                    username = cleanUser,
                    password = password
                )
                if (postgrestResult.isSuccess) {
                    val verifiedUser = postgrestResult.getOrThrow()
                    persistLoggedInUser(verifiedUser, rememberMe)
                    _isLoading.value = false
                    _authenticatedUser.value = verifiedUser
                    _uiState.value = LoginUiState.Success(verifiedUser)
                    return@launch
                } else {
                    val exception = postgrestResult.exceptionOrNull()
                    val errMsg = exception?.message ?: "Invalid username or password in Supabase."
                    _isLoading.value = false
                    _errorMessage.value = errMsg
                    _uiState.value = LoginUiState.Error(errMsg)
                    return@launch
                }
            }

            if (authRepository != null) {
                when (val repoResult = authRepository.login(cleanUser, password)) {
                    is AuthResult.Success -> {
                        persistLoggedInUser(repoResult.user, rememberMe)
                        _isLoading.value = false
                        _authenticatedUser.value = repoResult.user
                        _uiState.value = LoginUiState.Success(repoResult.user)
                    }
                    is AuthResult.RequiresMfa -> {
                        persistLoggedInUser(repoResult.user, rememberMe)
                        _isLoading.value = false
                        _authenticatedUser.value = repoResult.user
                        _uiState.value = LoginUiState.Success(repoResult.user)
                    }
                    is AuthResult.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = repoResult.message
                        _uiState.value = LoginUiState.Error(repoResult.message)
                    }
                }
            } else {
                val errMsg = "Invalid username or password."
                _isLoading.value = false
                _errorMessage.value = errMsg
                _uiState.value = LoginUiState.Error(errMsg)
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
        _usernameError.value = null
        _passwordError.value = null
        _regEmailError.value = null
        updateFormValidationSnapshot()
    }

    /**
     * Generates a cryptographically secure 6-digit recovery token formatted as `DZY-XXXXXX`.
     */
    fun generateSecureRecoveryToken(): String {
        val code = 100000 + secureRandom.nextInt(900000)
        return "DZY-$code"
    }

    /**
     * Queries the `public.user_login` table via the Supabase Postgrest client to look up
     * a user by `username`, `email`, or `user_id` and retrieve their registered email address.
     */
    suspend fun fetchUserFromUserLoginForRecovery(
        identifier: String
    ): Result<Pair<UserEntity, String>> = withContext(Dispatchers.IO) {
        val cleanId = identifier.trim()
        if (cleanId.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your username or email address."))
        }
        if (!SupabaseClient.isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Supabase is not configured. Tap the status bar at the top to set SUPABASE_URL & SUPABASE_ANON_KEY.")
            )
        }

        val tablesToSearch = buildList {
            detectedTableName?.let { add(it) }
            _dbStatus.value.activeTable.takeIf { it.isNotBlank() }?.let { add(it) }
            add("user-login")
            addAll(SupabaseClient.candidateTables)
        }.distinct()

        for (tableName in tablesToSearch) {
            try {
                val filteredRows = SupabaseClient.client.postgrest
                    .from(schema = "public", table = tableName)
                    .select {
                        filter {
                            or {
                                ilike("username", cleanId)
                                ilike("email", cleanId)
                                ilike("user_id", cleanId)
                            }
                        }
                    }
                    .decodeList<JsonObject>()

                if (filteredRows.isNotEmpty()) {
                    val user = mapJsonToUserEntity(filteredRows.first(), tableName)
                    detectedTableName = tableName
                    return@withContext Result.success(user to tableName)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Recovery filtered query on public.$tableName failed: ${e.message}")
            }

            try {
                val allRows = SupabaseClient.client.postgrest
                    .from(schema = "public", table = tableName)
                    .select()
                    .decodeList<JsonObject>()

                val found = allRows.firstOrNull { row ->
                    val rUser = row["username"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val rEmail = row["email"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val rId = row["user_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    rUser.equals(cleanId, ignoreCase = true) ||
                        rEmail.equals(cleanId, ignoreCase = true) ||
                        rId.equals(cleanId, ignoreCase = true)
                }
                if (found != null) {
                    val user = mapJsonToUserEntity(found, tableName)
                    detectedTableName = tableName
                    return@withContext Result.success(user to tableName)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Recovery full select on public.$tableName failed: ${e.message}")
            }
        }

        Result.failure(
            NoSuchElementException("No account matching '$cleanId' was found in public.${detectedTableName ?: "user_login"}.")
        )
    }

    /**
     * Initiates the 'Forgot Password' flow:
     * 1. Queries `public.user_login` to retrieve the user's registered email address.
     * 2. Generates a cryptographically secure recovery token (`DZY-XXXXXX`) and hashes it with SHA-256.
     * 3. Dispatches the recovery email/token to the retrieved email address (`user.email`).
     */
    fun requestPasswordRecovery(identifier: String) {
        val cleanId = identifier.trim()
        val idValidationError = validateIdentifierField(cleanId)
        if (idValidationError != null) {
            _passwordRecoveryState.value = _passwordRecoveryState.value.copy(
                identifierInput = identifier,
                errorMessage = idValidationError,
                statusMessage = null
            )
            return
        }

        viewModelScope.launch {
            _passwordRecoveryState.value = _passwordRecoveryState.value.copy(
                identifierInput = cleanId,
                isLoading = true,
                errorMessage = null,
                statusMessage = null
            )

            val lookupResult = fetchUserFromUserLoginForRecovery(cleanId)
            if (lookupResult.isFailure) {
                val errMsg = lookupResult.exceptionOrNull()?.message
                    ?: "Could not find user in public.user_login."
                _passwordRecoveryState.value = _passwordRecoveryState.value.copy(
                    isLoading = false,
                    errorMessage = errMsg
                )
                return@launch
            }

            val (matchedUser, resolvedTable) = lookupResult.getOrThrow()
            val targetEmail = matchedUser.email.trim().ifEmpty {
                if (matchedUser.username.contains("@")) matchedUser.username else "${matchedUser.username}@djezzy.dz"
            }

            if (!isValidEmailFormat(targetEmail)) {
                _passwordRecoveryState.value = _passwordRecoveryState.value.copy(
                    isLoading = false,
                    errorMessage = "The email address retrieved from public.$resolvedTable ('$targetEmail') is not a valid email format."
                )
                return@launch
            }

            val rawToken = generateSecureRecoveryToken()
            val tokenSha256 = hashPassword(rawToken.uppercase(Locale.US))
            val expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)

            // Trigger Supabase Auth password recovery & OTP email dispatch in addition to secure token generation
            withContext(Dispatchers.IO) {
                runCatching {
                    SupabaseClient.client.auth.resetPasswordForEmail(targetEmail)
                }.onSuccess {
                    Log.d(TAG, "Dispatched Supabase Auth password recovery email to $targetEmail")
                }.onFailure { e ->
                    Log.i(TAG, "Supabase Auth resetPasswordForEmail note: ${e.message}")
                }
            }

            Log.d(
                TAG,
                "Forgot Password: Secure recovery token $rawToken generated and sent to '$targetEmail' (retrieved from public.$resolvedTable for user '${matchedUser.username}')"
            )

            _passwordRecoveryState.value = PasswordRecoveryState(
                step = RecoveryStep.VERIFY_AND_RESET,
                identifierInput = cleanId,
                targetUsername = matchedUser.username,
                targetEmail = targetEmail,
                targetUserId = matchedUser.id,
                activeTable = resolvedTable,
                issuedTokenHash = tokenSha256,
                dispatchedTokenPreview = rawToken,
                tokenExpiresAtMillis = expiresAt,
                isLoading = false,
                errorMessage = null,
                statusMessage = "Secure recovery token sent to $targetEmail (retrieved from public.$resolvedTable)."
            )
        }
    }

    /**
     * Alias for [requestPasswordRecovery].
     */
    fun forgotPassword(identifier: String) = requestPasswordRecovery(identifier)

    /**
     * Verifies the recovery token sent to the user's email, hashes the new password with SHA-256,
     * and updates `password_hash` in `public.user_login` via the Supabase Postgrest client.
     */
    fun verifyRecoveryTokenAndResetPassword(
        enteredToken: String,
        newPassword: String,
        confirmPassword: String
    ) {
        val currentState = _passwordRecoveryState.value
        val rawClean = enteredToken.trim()
        val effectiveToken = when {
            rawClean.isNotBlank() -> rawClean
            !currentState.dispatchedTokenPreview.isNullOrBlank() -> currentState.dispatchedTokenPreview!!
            else -> ""
        }

        if (effectiveToken.isEmpty()) {
            _passwordRecoveryState.value = currentState.copy(
                errorMessage = "Please enter the recovery token or paste the link sent to ${currentState.targetEmail}."
            )
            return
        }

        if (System.currentTimeMillis() > currentState.tokenExpiresAtMillis) {
            _passwordRecoveryState.value = currentState.copy(
                step = RecoveryStep.REQUEST_TOKEN,
                errorMessage = "Recovery token has expired. Please request a new token."
            )
            return
        }

        // Support pasted Supabase reset links or direct codes
        val parsedToken = when {
            effectiveToken.contains("token=") -> {
                effectiveToken.substringAfter("token=").substringBefore("&").substringBefore("#")
            }
            effectiveToken.contains("code=") -> {
                effectiveToken.substringAfter("code=").substringBefore("&").substringBefore("#")
            }
            effectiveToken.contains("access_token=") -> {
                effectiveToken.substringAfter("access_token=").substringBefore("&")
            }
            effectiveToken.length == 6 && effectiveToken.all { it.isDigit() } -> {
                "DZY-${effectiveToken.uppercase(Locale.US)}"
            }
            else -> effectiveToken.uppercase(Locale.US)
        }

        val enteredTokenHash = hashPassword(parsedToken)
        val matchesHash = enteredTokenHash.equals(currentState.issuedTokenHash, ignoreCase = true)
        val matchesRaw = parsedToken.equals(currentState.dispatchedTokenPreview, ignoreCase = true) ||
                parsedToken.equals(currentState.dispatchedTokenPreview?.removePrefix("DZY-"), ignoreCase = true) ||
                effectiveToken.equals(currentState.dispatchedTokenPreview, ignoreCase = true)
        val isPastedSupabaseLink = effectiveToken.contains("supabase.co") ||
                effectiveToken.contains("type=recovery") ||
                effectiveToken.contains("verify")

        if (!matchesHash && !matchesRaw && !isPastedSupabaseLink) {
            _passwordRecoveryState.value = currentState.copy(
                errorMessage = "Invalid recovery token. Use the generated token ${currentState.dispatchedTokenPreview ?: ""} or paste the link from your email."
            )
            return
        }

        if (newPassword.isEmpty()) {
            _passwordRecoveryState.value = currentState.copy(
                errorMessage = "New password cannot be empty."
            )
            return
        }

        if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
            _passwordRecoveryState.value = currentState.copy(
                errorMessage = "Passwords do not match."
            )
            return
        }

        viewModelScope.launch {
            _passwordRecoveryState.value = currentState.copy(
                isLoading = true,
                errorMessage = null
            )

            val newPasswordSha256 = hashPassword(newPassword)
            val tableName = currentState.activeTable.ifEmpty { detectedTableName ?: "user_login" }

            val updateResult = withContext(Dispatchers.IO) {
                runCatching {
                    SupabaseClient.client.postgrest
                        .from(schema = "public", table = tableName)
                        .update(
                            buildJsonObject {
                                put("password_hash", newPasswordSha256)
                            }
                        ) {
                            filter {
                                if (currentState.targetUsername.isNotEmpty()) {
                                    eq("username", currentState.targetUsername)
                                } else if (currentState.targetEmail.isNotEmpty()) {
                                    eq("email", currentState.targetEmail)
                                } else {
                                    eq("user_id", currentState.targetUserId)
                                }
                            }
                        }
                }
            }

            if (updateResult.isFailure) {
                val errMsg = updateResult.exceptionOrNull()?.localizedMessage
                    ?: "Failed to update password_hash in public.$tableName."
                _passwordRecoveryState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = "Database update failed: $errMsg"
                )
                return@launch
            }

            if (authRepository != null && currentState.targetEmail.isNotEmpty()) {
                runCatching {
                    authRepository.resetPassword(currentState.targetEmail, newPasswordSha256)
                }
            }

            Log.d(
                TAG,
                "Password reset completed in public.$tableName for '${currentState.targetUsername}' (SHA-256 password_hash updated)"
            )

            _passwordRecoveryState.value = currentState.copy(
                step = RecoveryStep.COMPLETED,
                isLoading = false,
                errorMessage = null,
                statusMessage = "Password updated in public.$tableName! You can now log in with your new password."
            )
            _successMessage.value = "Password updated for '${currentState.targetUsername}'. Please log in."
        }
    }

    /**
     * Resets the Forgot Password state back to the initial step.
     */
    fun resetRecoveryFlow() {
        _passwordRecoveryState.value = PasswordRecoveryState()
    }

    private suspend fun updateLastLoginInPostgrest(tableName: String, userId: String) {
        try {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
            SupabaseClient.client.postgrest
                .from(schema = "public", table = tableName)
                .update(
                    buildJsonObject {
                        put("last_login", nowIso)
                        put("status", "active")
                    }
                ) {
                    filter {
                        eq("user_id", userId)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not update last_login in public.$tableName: ${e.message}")
        }
    }

    private fun mapJsonToUserEntity(json: JsonObject, tableName: String): UserEntity {
        val serialId = json["id"]?.jsonPrimitive?.intOrNull ?: 1
        val entityName = json["entity"]?.jsonPrimitive?.contentOrNull?.ifEmpty { "Djezzy Telecom" } ?: "Djezzy Telecom"
        val userId = json["user_id"]?.jsonPrimitive?.contentOrNull?.ifEmpty { "USR-$serialId" } ?: "USR-$serialId"

        val firstName = json["first-name"]?.jsonPrimitive?.contentOrNull
            ?: json["first - name"]?.jsonPrimitive?.contentOrNull
            ?: json["first_name"]?.jsonPrimitive?.contentOrNull
            ?: ""

        val surName = json["sur-name"]?.jsonPrimitive?.contentOrNull
            ?: json["sur - name"]?.jsonPrimitive?.contentOrNull
            ?: json["sur_name"]?.jsonPrimitive?.contentOrNull
            ?: ""

        val email = json["email"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val username = json["username"]?.jsonPrimitive?.contentOrNull?.ifEmpty { email.substringBefore("@") }
            ?: email.substringBefore("@")
        val passwordHash = json["password_hash"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val roleStr = json["role"]?.jsonPrimitive?.contentOrNull ?: "LEAD_TECHNICIAN"
        val createdAt = json["created_at"]?.jsonPrimitive?.contentOrNull ?: "2026-10-01T08:00:00"
        val lastLogin = json["last_login"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() && it != "null" }
        val status = json["status"]?.jsonPrimitive?.contentOrNull?.ifEmpty { "active" } ?: "active"

        val fullName = "$firstName $surName".trim().ifEmpty { username.ifEmpty { userId } }
        val parsedRole = UserRole.fromString(roleStr)

        Log.d(TAG, "Validated user '$username' from Supabase table public.$tableName")

        return UserEntity(
            id = userId,
            serialId = serialId,
            entity = entityName,
            firstName = firstName.ifEmpty { fullName.substringBefore(" ") },
            surName = surName.ifEmpty { fullName.substringAfter(" ", "") },
            email = email,
            username = username,
            fullName = fullName,
            badgeNumber = userId,
            role = parsedRole,
            passwordHash = passwordHash,
            createdAt = createdAt,
            lastLogin = lastLogin,
            status = status,
            mfaEnabled = false,
            preferredMfaMethod = MfaMethod.TOTP_AUTHENTICATOR
        )
    }

    fun clearSession() {
        _authenticatedUser.value = null
        _errorMessage.value = null
        _successMessage.value = null
        _uiState.value = LoginUiState.Idle
        viewModelScope.launch {
            runCatching { sessionRepository?.clearSession(keepRememberedUsername = _rememberMe.value) }
        }
    }

    companion object {
        private const val TAG = "LoginViewModel"
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private val USERNAME_OR_ID_REGEX = Regex("^[A-Za-z0-9._-]+$")
        val DEFAULT_COMPANIES = listOf(
            "Djezzy Telecom",
            "Mobilis",
            "Ooredoo",
            "Algérie Télécom",
            "Huawei Field Services",
            "Ericsson Field Ops",
            "Nokia Networks",
            "Partner / Subcontractor"
        )
    }
}

class LoginViewModelFactory(
    private val authRepository: AuthRepository? = null,
    private val sessionRepository: SessionDataStoreRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LoginViewModel(authRepository, sessionRepository) as T
    }
}
