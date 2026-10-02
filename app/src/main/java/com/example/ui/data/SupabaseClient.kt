package com.example.ui.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Singleton object that initializes and provides the Supabase client instance
 * using `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY`, with support
 * for runtime configuration if credentials are provided in-app.
 */
object SupabaseClient {

    private const val TAG = "SupabaseClient"
    private const val PREFS_NAME = "supabase_runtime_prefs"
    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"
    private const val KEY_TABLE_NAME = "supabase_table_name"

    @Volatile
    private var runtimeUrl: String = ""

    @Volatile
    private var runtimeAnonKey: String = ""

    @Volatile
    private var preferredTableName: String = ""

    @Volatile
    private var cachedClient: JanSupabaseClient? = null

    @Volatile
    private var cachedClientKey: String = ""

    /**
     * Loads any saved runtime Supabase credentials from SharedPreferences.
     */
    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        runtimeUrl = prefs.getString(KEY_URL, "")?.trim()?.trimEnd('/').orEmpty()
        runtimeAnonKey = prefs.getString(KEY_ANON_KEY, "")?.trim().orEmpty()
        preferredTableName = prefs.getString(KEY_TABLE_NAME, "")?.trim().orEmpty()
    }

    /**
     * Saves runtime Supabase URL, Anon Key, and preferred table name so the user can connect
     * immediately even if `.env` / Secrets were not set before compilation.
     */
    fun updateCredentials(
        context: Context,
        url: String,
        anonKey: String,
        tableName: String = ""
    ) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanKey = anonKey.trim()
        val cleanTable = tableName.trim()
        runtimeUrl = cleanUrl
        runtimeAnonKey = cleanKey
        preferredTableName = cleanTable
        cachedClient = null
        cachedClientKey = ""

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URL, cleanUrl)
            .putString(KEY_ANON_KEY, cleanKey)
            .putString(KEY_TABLE_NAME, cleanTable)
            .apply()
    }

    val supabaseUrl: String
        get() {
            if (isValidUrl(runtimeUrl)) return runtimeUrl
            val buildConfigUrl = runCatching {
                BuildConfig.SUPABASE_URL.trim().trimEnd('/')
            }.getOrDefault("")
            if (isValidUrl(buildConfigUrl)) return buildConfigUrl
            val envUrl = System.getenv("SUPABASE_URL")?.trim()?.trimEnd('/').orEmpty()
            return if (isValidUrl(envUrl)) envUrl else buildConfigUrl
        }

    val supabaseAnonKey: String
        get() {
            if (isValidAnonKey(runtimeAnonKey)) return runtimeAnonKey
            val buildConfigKey = runCatching {
                BuildConfig.SUPABASE_ANON_KEY.trim()
            }.getOrDefault("")
            if (isValidAnonKey(buildConfigKey)) return buildConfigKey
            val envKey = System.getenv("SUPABASE_ANON_KEY")?.trim().orEmpty()
            return if (isValidAnonKey(envKey)) envKey else buildConfigKey
        }

    val configuredTableName: String
        get() = preferredTableName.ifEmpty { "user-login" }

    /**
     * Candidate table names in priority order. Includes any user-specified table name first,
     * followed by `user-login`, `user_login`, `user - login`, `user`, and `users`.
     */
    val candidateTables: List<String>
        get() = buildList {
            if (preferredTableName.isNotBlank()) {
                add(preferredTableName)
            }
            add("user-login")
            add("user_login")
            add("user - login")
            add("user")
            add("users")
        }.distinct()

    private fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        if (url.contains("your-project-id") || url.contains("placeholder") || url.contains("PLACEHOLDER")) {
            return false
        }
        return url.startsWith("https://") || url.startsWith("http://")
    }

    private fun isValidAnonKey(key: String): Boolean {
        if (key.isBlank()) return false
        if (key == "YOUR_SUPABASE_ANON_KEY" || key.contains("placeholder") || key.contains("PLACEHOLDER")) {
            return false
        }
        return key.length > 15
    }

    /**
     * Checks whether valid (non-placeholder) `SUPABASE_URL` and `SUPABASE_ANON_KEY` values are configured.
     */
    fun isConfigured(): Boolean {
        return isValidUrl(supabaseUrl) && isValidAnonKey(supabaseAnonKey)
    }

    /**
     * Singleton Supabase client instance initialized using `BuildConfig.SUPABASE_URL`
     * and `BuildConfig.SUPABASE_ANON_KEY` (or runtime-configured credentials).
     */
    val client: JanSupabaseClient
        get() {
            val resolvedUrl = supabaseUrl.ifBlank { "https://placeholder-project.supabase.co" }
            val resolvedKey = supabaseAnonKey.ifBlank { "placeholder-anon-key" }
            val signature = "$resolvedUrl|$resolvedKey"

            val existing = cachedClient
            if (existing != null && cachedClientKey == signature) {
                return existing
            }

            synchronized(this) {
                val again = cachedClient
                if (again != null && cachedClientKey == signature) {
                    return again
                }
                val created = createSupabaseClient(
                    supabaseUrl = resolvedUrl,
                    supabaseKey = resolvedKey
                ) {
                    httpEngine = OkHttp.create()
                    install(Postgrest)
                    install(Auth)
                    install(Realtime)
                }
                cachedClient = created
                cachedClientKey = signature
                return created
            }
        }

    /**
     * Alias for the singleton [client] instance.
     */
    val instance: JanSupabaseClient
        get() = client

    /**
     * Returns the initialized [JanSupabaseClient] if configured, or `null` otherwise.
     */
    val clientOrNull: JanSupabaseClient?
        get() = if (isConfigured()) {
            runCatching { client }
                .onFailure { e -> Log.e(TAG, "Failed to initialize SupabaseClient: ${e.message}", e) }
                .getOrNull()
        } else {
            null
        }
}
