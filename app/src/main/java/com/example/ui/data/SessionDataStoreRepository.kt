package com.example.ui.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.MfaMethod
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "djezzy_user_session_prefs"
)

/**
 * Snapshot of the persisted user login state stored in Jetpack Preferences DataStore.
 */
data class UserSessionState(
    val isLoggedIn: Boolean = false,
    val rememberMe: Boolean = true,
    val savedUsername: String = "",
    val user: UserEntity? = null,
    val sessionSavedAtMillis: Long = 0L
)

/**
 * DataStore repository that manages local user login state across app launches,
 * ensuring the app remembers the active session and automatically directs the user
 * to the main dashboard upon launch if already authenticated with "Remember Me" enabled.
 */
class SessionDataStoreRepository(context: Context) {

    private val dataStore: DataStore<Preferences> = context.applicationContext.sessionDataStore

    /**
     * Reactive stream emitting the complete [UserSessionState] from DataStore.
     */
    val sessionStateFlow: Flow<UserSessionState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val user = mapPreferencesToUser(prefs)
            val rememberMe = prefs[Keys.REMEMBER_ME] ?: true
            val savedUsername = prefs[Keys.SAVED_USERNAME] ?: prefs[Keys.USERNAME].orEmpty()
            UserSessionState(
                isLoggedIn = user != null,
                rememberMe = rememberMe,
                savedUsername = savedUsername,
                user = user,
                sessionSavedAtMillis = prefs[Keys.SESSION_SAVED_AT_MILLIS] ?: 0L
            )
        }

    /**
     * Reactive stream emitting `true` if a valid logged-in session is stored in DataStore.
     */
    val isLoggedInFlow: Flow<Boolean> = sessionStateFlow.map { it.isLoggedIn }

    /**
     * Reactive stream emitting the persisted [UserEntity] if the user is logged in, or `null` otherwise.
     */
    val savedUserFlow: Flow<UserEntity?> = sessionStateFlow.map { it.user }

    /**
     * Reactive stream emitting the user's "Remember Me" preference.
     */
    val rememberMeFlow: Flow<Boolean> = sessionStateFlow.map { it.rememberMe }

    /**
     * Reactive stream emitting any previously saved username or identifier.
     */
    val savedUsernameFlow: Flow<String> = sessionStateFlow.map { it.savedUsername }

    private fun mapPreferencesToUser(prefs: Preferences): UserEntity? {
        val isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false
        val rememberMe = prefs[Keys.REMEMBER_ME] ?: true
        val username = prefs[Keys.USERNAME].orEmpty()
        // If Remember Me is disabled or user is not logged in, do not restore persistent session on launch
        if (!isLoggedIn || !rememberMe || username.isBlank()) {
            return null
        }
        val userId = prefs[Keys.USER_ID].orEmpty().ifEmpty { "USR-001" }
        val firstName = prefs[Keys.FIRST_NAME].orEmpty()
        val surName = prefs[Keys.SUR_NAME].orEmpty()
        val fullName = prefs[Keys.FULL_NAME].orEmpty()
            .ifEmpty { "$firstName $surName".trim().ifEmpty { username } }
        val roleStr = prefs[Keys.ROLE].orEmpty().ifEmpty { UserRole.LEAD_TECHNICIAN.name }

        return UserEntity(
            id = userId,
            serialId = prefs[Keys.SERIAL_ID] ?: 1,
            entity = prefs[Keys.ENTITY].orEmpty().ifEmpty { "Djezzy Telecom" },
            firstName = firstName,
            surName = surName,
            email = prefs[Keys.EMAIL].orEmpty(),
            username = username,
            fullName = fullName,
            badgeNumber = prefs[Keys.BADGE_NUMBER].orEmpty().ifEmpty { userId },
            role = UserRole.fromString(roleStr),
            passwordHash = prefs[Keys.PASSWORD_HASH].orEmpty(),
            createdAt = prefs[Keys.CREATED_AT].orEmpty(),
            lastLogin = prefs[Keys.LAST_LOGIN],
            status = prefs[Keys.STATUS].orEmpty().ifEmpty { "active" },
            mfaEnabled = false,
            preferredMfaMethod = MfaMethod.TOTP_AUTHENTICATOR
        )
    }

    /**
     * Reads the currently saved [UserEntity] once from DataStore.
     */
    suspend fun getSavedUserOnce(): UserEntity? = savedUserFlow.firstOrNull()

    /**
     * Saves the authenticated user's session state to DataStore.
     * When [rememberMe] is true, the full login session and username are stored for automatic restoration.
     * When [rememberMe] is false, session credentials are NOT saved to disk so the user must re-authenticate on next launch.
     */
    suspend fun saveLoggedInUser(user: UserEntity, rememberMe: Boolean = true) {
        dataStore.edit { prefs ->
            prefs[Keys.REMEMBER_ME] = rememberMe
            if (rememberMe) {
                prefs[Keys.IS_LOGGED_IN] = true
                prefs[Keys.SAVED_USERNAME] = user.username
                prefs[Keys.USER_ID] = user.id
                prefs[Keys.SERIAL_ID] = user.serialId
                prefs[Keys.ENTITY] = user.entity
                prefs[Keys.FIRST_NAME] = user.firstName
                prefs[Keys.SUR_NAME] = user.surName
                prefs[Keys.EMAIL] = user.email
                prefs[Keys.USERNAME] = user.username
                prefs[Keys.FULL_NAME] = user.fullName
                prefs[Keys.BADGE_NUMBER] = user.badgeNumber
                prefs[Keys.ROLE] = user.role.name
                prefs[Keys.PASSWORD_HASH] = user.passwordHash
                prefs[Keys.CREATED_AT] = user.createdAt
                user.lastLogin?.let { prefs[Keys.LAST_LOGIN] = it }
                prefs[Keys.STATUS] = user.status
                prefs[Keys.SESSION_SAVED_AT_MILLIS] = System.currentTimeMillis()
            } else {
                prefs[Keys.IS_LOGGED_IN] = false
                prefs.remove(Keys.USER_ID)
                prefs.remove(Keys.USERNAME)
                prefs.remove(Keys.PASSWORD_HASH)
                prefs[Keys.SESSION_SAVED_AT_MILLIS] = 0L
            }
        }
    }

    /**
     * Updates the user's "Remember Me" toggle preference independently.
     */
    suspend fun setRememberMePreference(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.REMEMBER_ME] = enabled
        }
    }

    /**
     * Clears the persisted logged-in session state when the user signs out.
     * Optionally retains the remembered username so the user doesn't have to re-type it.
     */
    suspend fun clearSession(keepRememberedUsername: Boolean = true) {
        dataStore.edit { prefs ->
            val rememberedUser = prefs[Keys.SAVED_USERNAME].orEmpty().ifEmpty { prefs[Keys.USERNAME].orEmpty() }
            val rememberMe = prefs[Keys.REMEMBER_ME] ?: true
            prefs.clear()
            if (keepRememberedUsername && rememberMe && rememberedUser.isNotBlank()) {
                prefs[Keys.REMEMBER_ME] = true
                prefs[Keys.SAVED_USERNAME] = rememberedUser
            } else {
                prefs[Keys.REMEMBER_ME] = rememberMe
            }
        }
    }

    private object Keys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val SAVED_USERNAME = stringPreferencesKey("saved_username")
        val USER_ID = stringPreferencesKey("user_id")
        val SERIAL_ID = intPreferencesKey("serial_id")
        val ENTITY = stringPreferencesKey("entity")
        val FIRST_NAME = stringPreferencesKey("first_name")
        val SUR_NAME = stringPreferencesKey("sur_name")
        val EMAIL = stringPreferencesKey("email")
        val USERNAME = stringPreferencesKey("username")
        val FULL_NAME = stringPreferencesKey("full_name")
        val BADGE_NUMBER = stringPreferencesKey("badge_number")
        val ROLE = stringPreferencesKey("role")
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val CREATED_AT = stringPreferencesKey("created_at")
        val LAST_LOGIN = stringPreferencesKey("last_login")
        val STATUS = stringPreferencesKey("status")
        val SESSION_SAVED_AT_MILLIS = longPreferencesKey("session_saved_at_millis")
    }
}
