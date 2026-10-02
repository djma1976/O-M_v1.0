package com.example.data.remote

import android.util.Log
import com.example.data.model.AuditLogEntity
import com.example.data.model.MfaMethod
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.data.SupabaseClient
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class UserLoginDto(
    val id: Int? = null,
    val entity: String = "Djezzy Telecom",
    @SerialName("user_id") val userId: String,
    @SerialName("first-name") val firstName: String,
    @SerialName("sur-name") val surName: String,
    val email: String,
    val username: String,
    @SerialName("password_hash") val passwordHash: String,
    val role: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_login") val lastLogin: String? = null,
    val status: String? = "active"
)

data class SupabaseConnectionStatus(
    val isConfigured: Boolean,
    val isReachable: Boolean,
    val projectUrl: String,
    val statusLabel: String,
    val lastSyncDetail: String
)

data class SupabaseGoTrueSession(
    val accessToken: String,
    val userId: String,
    val email: String,
    val fullName: String?,
    val badgeNumber: String?,
    val role: String?
)

/**
 * Auth and database operations service backed by the [SupabaseClient] singleton object
 * interacting with the `user_login` / `user-login` table in Supabase PostgreSQL.
 */
class SupabaseAuthClient {

    // Supports `user_login`, `user-login`, `user - login`, `user`, and `users` table names
    private val candidateTables: List<String>
        get() = SupabaseClient.candidateTables

    @Volatile
    private var activeTableName: String = "user_login"

    @Volatile
    private var useSpacedHyphenColumns: Boolean = false

    val supabaseUrl: String
        get() = SupabaseClient.supabaseUrl

    val supabaseAnonKey: String
        get() = SupabaseClient.supabaseAnonKey

    val client: JanSupabaseClient?
        get() = SupabaseClient.clientOrNull

    fun isConfigured(): Boolean = SupabaseClient.isConfigured()

    fun currentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        return sdf.format(Date())
    }

    /**
     * Probes the Supabase Postgrest plugin to detect whether the table is named
     * `user-login`, `user_login`, or `user - login`.
     */
    private suspend fun resolveActiveTable(): Pair<Boolean, String> {
        val sdk = client ?: return false to "SupabaseClient not initialized"
        var lastError = ""
        for (tableName in candidateTables) {
            try {
                val rows = sdk.from(tableName).select().decodeList<JsonObject>()
                activeTableName = tableName
                val firstRow = rows.firstOrNull()
                if (firstRow != null) {
                    if (firstRow.containsKey("first - name") || firstRow.containsKey("sur - name")) {
                        useSpacedHyphenColumns = true
                    } else if (firstRow.containsKey("first-name") || firstRow.containsKey("sur-name")) {
                        useSpacedHyphenColumns = false
                    }
                }
                return true to "Connected to public.$tableName (${rows.size} rows)"
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: e.javaClass.simpleName
            }
        }
        return false to lastError
    }

    /**
     * Checks connectivity to Supabase using [SupabaseClient] and returns current status.
     */
    suspend fun checkConnectionStatus(): SupabaseConnectionStatus = withContext(Dispatchers.IO) {
        if (!isConfigured() || client == null) {
            return@withContext SupabaseConnectionStatus(
                isConfigured = false,
                isReachable = false,
                projectUrl = "Unconfigured (Set SUPABASE_URL & SUPABASE_ANON_KEY in Secrets)",
                statusLabel = "SupabaseClient Unconfigured (Local Fallback)",
                lastSyncDetail = "Add SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets to initialize SupabaseClient."
            )
        }

        val (ok, detail) = resolveActiveTable()
        if (ok) {
            SupabaseConnectionStatus(
                isConfigured = true,
                isReachable = true,
                projectUrl = supabaseUrl,
                statusLabel = "SupabaseClient: public.$activeTableName",
                lastSyncDetail = detail
            )
        } else {
            SupabaseConnectionStatus(
                isConfigured = true,
                isReachable = false,
                projectUrl = supabaseUrl,
                statusLabel = "SupabaseClient Warning ($activeTableName)",
                lastSyncDetail = "Ensure table 'user-login' / 'user_login' and RLS policy exist: $detail"
            )
        }
    }

    /**
     * Queries the `user-login` / `user_login` table via `SupabaseClient.client.from(activeTableName).select()`
     * matching `email`, `username`, or `user_id`.
     */
    suspend fun fetchUserFromDatabase(identifier: String): UserEntity? = withContext(Dispatchers.IO) {
        val sdk = client ?: return@withContext null
        val clean = identifier.trim()
        if (clean.isEmpty()) return@withContext null

        resolveActiveTable()

        try {
            val rows = sdk.from(activeTableName)
                .select {
                    filter {
                        or {
                            ilike("email", clean)
                            ilike("username", clean)
                            ilike("user_id", clean)
                        }
                    }
                }
                .decodeList<JsonObject>()

            if (rows.isNotEmpty()) {
                return@withContext parseUserJsonObject(rows.first())
            }
        } catch (e: Exception) {
            Log.w("SupabaseAuthClient", "Filtered select failed, falling back to full select: ${e.message}")
        }

        try {
            val allRows = sdk.from(activeTableName).select().decodeList<JsonObject>()
            val matched = allRows.firstOrNull { obj ->
                val email = obj["email"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val username = obj["username"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val userId = obj["user_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                email.equals(clean, ignoreCase = true) ||
                    username.equals(clean, ignoreCase = true) ||
                    userId.equals(clean, ignoreCase = true)
            }
            if (matched != null) {
                return@withContext parseUserJsonObject(matched)
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuthClient", "Error fetching from $activeTableName via SupabaseClient: ${e.message}", e)
        }

        null
    }

    /**
     * Authenticates user credentials via `SupabaseClient.client.auth.signInWith(Email)`.
     */
    suspend fun signInWithGoTrue(email: String, password: String): Result<SupabaseGoTrueSession> = withContext(Dispatchers.IO) {
        val sdk = client ?: return@withContext Result.failure(IllegalStateException("SupabaseClient is not configured"))
        try {
            sdk.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val session = sdk.auth.currentSessionOrNull()
            val user = session?.user
            val metadata = user?.userMetadata
            return@withContext Result.success(
                SupabaseGoTrueSession(
                    accessToken = session?.accessToken.orEmpty(),
                    userId = user?.id ?: "SUPA-USER",
                    email = user?.email ?: email.trim(),
                    fullName = metadata?.get("full_name")?.jsonPrimitive?.contentOrNull,
                    badgeNumber = metadata?.get("user_id")?.jsonPrimitive?.contentOrNull,
                    role = metadata?.get("role")?.jsonPrimitive?.contentOrNull
                )
            )
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    /**
     * Inserts or upserts a user row into `user-login` / `user_login` using [SupabaseClient].
     */
    suspend fun registerUserInSupabase(user: UserEntity, rawPassword: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val sdk = client ?: return@withContext Result.failure(
            IllegalStateException("Configure SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets first.")
        )

        resolveActiveTable()

        try {
            runCatching {
                sdk.auth.signUpWith(Email) {
                    this.email = user.email
                    this.password = rawPassword
                    this.data = buildJsonObject {
                        put("entity", user.entity)
                        put("user_id", user.id)
                        put("first-name", user.firstName)
                        put("sur-name", user.surName)
                        put("username", user.username)
                        put("role", user.role.name)
                    }
                }
            }

            if (upsertRowWithSupabaseKt(user)) {
                return@withContext Result.success(user)
            } else {
                return@withContext Result.failure(
                    Exception("Failed to upsert row into '$activeTableName'. Verify table columns & RLS policy.")
                )
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuthClient", "Error registering user with SupabaseClient: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Syncs initial employee profiles to `user-login` / `user_login` using [SupabaseClient].
     */
    suspend fun syncInitialUsersToSupabase(users: List<UserEntity>): Boolean = withContext(Dispatchers.IO) {
        if (client == null) return@withContext false
        resolveActiveTable()
        var anySucceeded = false
        for (u in users) {
            if (upsertRowWithSupabaseKt(u)) {
                anySucceeded = true
            }
        }
        anySucceeded
    }

    private suspend fun upsertRowWithSupabaseKt(user: UserEntity): Boolean {
        val sdk = client ?: return false
        val modes = if (useSpacedHyphenColumns) listOf(true, false) else listOf(false, true)
        for (spaced in modes) {
            val jsonPayload = buildUserLoginJsonObject(user, spacedHyphens = spaced)
            try {
                sdk.from(activeTableName).insert(jsonPayload)
                useSpacedHyphenColumns = spaced
                return true
            } catch (insertEx: Exception) {
                Log.w("SupabaseAuthClient", "SupabaseClient insert (spaced=$spaced) failed: ${insertEx.message}")
                try {
                    sdk.from(activeTableName).upsert(jsonPayload) {
                        onConflict = "user_id"
                    }
                    useSpacedHyphenColumns = spaced
                    return true
                } catch (upsertEx: Exception) {
                    Log.w("SupabaseAuthClient", "SupabaseClient upsert (spaced=$spaced) failed: ${upsertEx.message}")
                }
            }
        }
        return false
    }

    /**
     * Updates `last_login` and `status` in `user-login` / `user_login` via [SupabaseClient].
     */
    suspend fun updateLastLoginInSupabase(userId: String, status: String = "active") = withContext(Dispatchers.IO) {
        val sdk = client ?: return@withContext
        try {
            sdk.from(activeTableName).update(
                buildJsonObject {
                    put("last_login", currentIsoTimestamp())
                    put("status", status)
                }
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseAuthClient", "Failed to update last_login via SupabaseClient: ${e.message}")
        }
    }

    /**
     * Updates `status` ('active' or 'locked') in `user-login` / `user_login` via [SupabaseClient].
     */
    suspend fun updateAccountStatusInSupabase(userId: String, status: String) = withContext(Dispatchers.IO) {
        val sdk = client ?: return@withContext
        try {
            sdk.from(activeTableName).update(
                buildJsonObject {
                    put("status", status)
                }
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseAuthClient", "Failed to update status via SupabaseClient: ${e.message}")
        }
    }

    /**
     * Updates `password_hash` and sets `status = 'active'` in `user-login` / `user_login` via [SupabaseClient].
     */
    suspend fun updatePasswordInSupabase(email: String, newPasswordHash: String): Boolean = withContext(Dispatchers.IO) {
        val sdk = client ?: return@withContext false
        try {
            sdk.from(activeTableName).update(
                buildJsonObject {
                    put("password_hash", ensureSha256(newPasswordHash))
                    put("status", "active")
                }
            ) {
                filter {
                    eq("email", email.trim())
                }
            }
            return@withContext true
        } catch (e: Exception) {
            Log.w("SupabaseAuthClient", "Failed to update password_hash via SupabaseClient: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Forwards security audit events to `audit_logs` via [SupabaseClient].
     */
    suspend fun insertAuditLogInSupabase(log: AuditLogEntity) = withContext(Dispatchers.IO) {
        val sdk = client ?: return@withContext
        try {
            sdk.from("audit_logs").insert(
                buildJsonObject {
                    put("timestamp", log.timestamp)
                    put("event_category", log.eventCategory)
                    put("actor", log.actor)
                    put("summary", log.summary)
                    put("details", log.details)
                    put("is_security_alert", log.isSecurityAlert)
                }
            )
        } catch (_: Exception) {
            // Ignore if audit_logs table is not present
        }
    }

    private fun parseUserJsonObject(json: JsonObject): UserEntity {
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

        val combinedFullName = "$firstName $surName".trim().ifEmpty {
            username.ifEmpty { userId }
        }

        val parsedRole = UserRole.fromString(roleStr)
        val isAccountLocked = status.equals("locked", ignoreCase = true) ||
            status.equals("suspended", ignoreCase = true) ||
            status.equals("blocked", ignoreCase = true)

        val preferredMfa = when (parsedRole) {
            UserRole.DISPATCHER -> MfaMethod.SMS_RADIO_TOKEN
            UserRole.HVAC_SPECIALIST -> MfaMethod.BIOMETRIC_KEY
            else -> MfaMethod.TOTP_AUTHENTICATOR
        }

        return UserEntity(
            id = userId,
            serialId = serialId,
            entity = entityName,
            firstName = firstName.ifEmpty { combinedFullName.substringBefore(" ") },
            surName = surName.ifEmpty { combinedFullName.substringAfter(" ", "") },
            email = email,
            username = username,
            fullName = combinedFullName,
            badgeNumber = userId,
            role = parsedRole,
            phone = "+213 770 00 00 00",
            passwordHash = passwordHash,
            createdAt = createdAt,
            lastLogin = lastLogin,
            status = status,
            mfaEnabled = true,
            preferredMfaMethod = preferredMfa,
            isLocked = isAccountLocked,
            lockoutUntilTimestamp = if (isAccountLocked) System.currentTimeMillis() + 15 * 60 * 1000 else 0L
        )
    }

    private fun buildUserLoginJsonObject(user: UserEntity, spacedHyphens: Boolean): JsonObject {
        val fName = user.firstName.ifEmpty { user.fullName.substringBefore(" ") }
        val sName = user.surName.ifEmpty { user.fullName.substringAfter(" ", "Ops") }
        val uName = user.username.ifEmpty { user.email.substringBefore("@") }
        val nowIso = currentIsoTimestamp()

        return buildJsonObject {
            put("entity", user.entity.ifEmpty { "Djezzy Telecom" })
            put("user_id", user.id)
            if (spacedHyphens) {
                put("first - name", fName)
                put("sur - name", sName)
            } else {
                put("first-name", fName)
                put("sur-name", sName)
            }
            put("email", user.email)
            put("username", uName)
            put("password_hash", ensureSha256(user.passwordHash))
            put("role", user.role.name)
            put("created_at", user.createdAt.replace(" ", "T").ifEmpty { nowIso })
            if (!user.lastLogin.isNullOrBlank()) {
                put("last_login", user.lastLogin.replace(" ", "T"))
            }
            put("status", user.status.ifEmpty { "active" })
        }
    }

    companion object {
        private val HEX_64_REGEX = Regex("^[0-9a-fA-F]{64}$")

        /**
         * Computes the lowercase 64-character SHA-256 hex digest of [input].
         */
        fun sha256Hex(input: String): String {
            val bytes = java.security.MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        /**
         * Returns [passwordOrHash] as a 64-character SHA-256 hex string (hashing it if it is plain text,
         * or preserving it lowercase if it is already a 64-character SHA-256 hex hash).
         */
        fun ensureSha256(passwordOrHash: String): String {
            val trimmed = passwordOrHash.trim()
            return if (HEX_64_REGEX.matches(trimmed)) {
                trimmed.lowercase(Locale.US)
            } else {
                sha256Hex(passwordOrHash)
            }
        }

        val SUPABASE_SQL_SCHEMA = """
            -- Supabase Table Definition: public."user-login" (or public.user_login)
            create table public."user-login" (
              id serial not null,
              entity character varying(100) not null,
              user_id character varying(50) not null,
              "first-name" character varying(100) not null,
              "sur-name" character varying(100) not null,
              email character varying(255) not null,
              username character varying(100) not null,
              password_hash character varying(255) not null,
              role character varying(50) not null,
              created_at timestamp without time zone null default now(),
              last_login timestamp without time zone null,
              status character varying(50) null default 'active'::character varying,
              constraint "user-login_pkey" primary key (id),
              constraint "user-login_email_key" unique (email),
              constraint "user-login_user_id_key" unique (user_id),
              constraint "user-login_username_key" unique (username)
            ) TABLESPACE pg_default;

            -- Enable Row Level Security (RLS) Policy for App Access:
            alter table public."user-login" enable row level security;
            create policy "Allow public.user-login access" on public."user-login"
              for all using (true) with check (true);
        """.trimIndent()
    }
}
