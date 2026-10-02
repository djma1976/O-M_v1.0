package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.MfaMethod
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseConnectionStatus
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.data.repository.RecoveryResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthScreenState {
    object Login : AuthScreenState()
    data class MfaPrompt(
        val user: UserEntity,
        val activeMethod: MfaMethod,
        val availableMethods: List<MfaMethod>,
        val testHintCode: String = "",
        val error: String? = null,
        val authSource: String = "Supabase public.user-login"
    ) : AuthScreenState()
    data class PasswordRecovery(
        val step: RecoveryStep,
        val email: String = "",
        val securityQuestion: String = "",
        val testSampleCode: String = "",
        val error: String? = null,
        val successMessage: String? = null
    ) : AuthScreenState()
    data class Authenticated(
        val user: UserEntity,
        val authSource: String = "Supabase public.user-login"
    ) : AuthScreenState()
}

enum class RecoveryStep {
    SELECT_METHOD,
    ENTER_EMAIL,
    ANSWER_QUESTION,
    ENTER_TOKEN,
    SET_NEW_PASSWORD,
    SUCCESS_CONFIRMED
}

data class PasswordStrength(
    val score: Int, // 0 to 4
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecial: Boolean,
    val label: String
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _screenState = MutableStateFlow<AuthScreenState>(AuthScreenState.Login)
    val screenState: StateFlow<AuthScreenState> = _screenState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _supabaseStatus = MutableStateFlow(
        SupabaseConnectionStatus(
            isConfigured = authRepository.supabaseClient.isConfigured(),
            isReachable = false,
            projectUrl = authRepository.supabaseClient.supabaseUrl.ifEmpty { "Configure SUPABASE_URL in Secrets" },
            statusLabel = if (authRepository.supabaseClient.isConfigured()) "Checking public.user-login..." else "Supabase Unconfigured (Local Fallback)",
            lastSyncDetail = "Connecting to Supabase table public.user-login..."
        )
    )
    val supabaseStatus: StateFlow<SupabaseConnectionStatus> = _supabaseStatus.asStateFlow()

    private val _supabaseInfoMessage = MutableStateFlow<String?>(null)
    val supabaseInfoMessage: StateFlow<String?> = _supabaseInfoMessage.asStateFlow()

    private val _totpRemainingSeconds = MutableStateFlow(30)
    val totpRemainingSeconds: StateFlow<Int> = _totpRemainingSeconds.asStateFlow()

    private val _liveTotpCode = MutableStateFlow("123456")
    val liveTotpCode: StateFlow<String> = _liveTotpCode.asStateFlow()

    init {
        refreshSupabaseConnection()

        // Start 30s rolling timer for TOTP display
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val secondsInWindow = 30 - ((now / 1000) % 30).toInt()
                _totpRemainingSeconds.value = secondsInWindow

                val state = _screenState.value
                if (state is AuthScreenState.MfaPrompt) {
                    val code = authRepository.getDeterministicTotpCode(state.user.totpSecret)
                    _liveTotpCode.value = code
                }
                delay(1000)
            }
        }
    }

    fun refreshSupabaseConnection() {
        viewModelScope.launch {
            _supabaseStatus.value = authRepository.getSupabaseStatus()
        }
    }

    fun syncDemoProfilesToSupabase() {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            val synced = authRepository.syncLocalUsersToSupabase()
            _supabaseStatus.value = authRepository.getSupabaseStatus()
            _isLoading.value = false
            _supabaseInfoMessage.value = if (synced) {
                "Demo profiles synced to Supabase 'public.user-login' table!"
            } else {
                "Configure SUPABASE_URL & SUPABASE_ANON_KEY in Secrets (and run user-login SQL) to sync."
            }
        }
    }

    fun registerEmployeeInSupabase(
        entityName: String,
        userId: String,
        firstName: String,
        surName: String,
        username: String,
        email: String,
        role: UserRole,
        password: String,
        mfaMethod: MfaMethod
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            val res = authRepository.registerEmployee(
                entityName = entityName,
                userId = userId,
                firstName = firstName,
                surName = surName,
                username = username,
                email = email,
                role = role,
                password = password,
                mfaMethod = mfaMethod
            )
            _isLoading.value = false
            res.onSuccess { user ->
                _supabaseInfoMessage.value = "Row inserted in public.user-login: ${user.username} (${user.id})!"
            }.onFailure { err ->
                _loginError.value = err.message
            }
        }
    }

    fun login(identifier: String, passwordAttempt: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            val result = authRepository.login(identifier, passwordAttempt)
            _isLoading.value = false

            when (result) {
                is AuthResult.Success -> {
                    _screenState.value = AuthScreenState.Authenticated(
                        user = result.user,
                        authSource = result.authSource
                    )
                }
                is AuthResult.RequiresMfa -> {
                    val hint = when (result.user.preferredMfaMethod) {
                        MfaMethod.TOTP_AUTHENTICATOR -> authRepository.getDeterministicTotpCode(result.user.totpSecret)
                        MfaMethod.SMS_RADIO_TOKEN -> authRepository.getPendingSmsCode(result.user.id) ?: "654321"
                        MfaMethod.BACKUP_CODE -> "8492-1049"
                        MfaMethod.BIOMETRIC_KEY -> "Instant FIDO2 Key Verification"
                    }
                    _screenState.value = AuthScreenState.MfaPrompt(
                        user = result.user,
                        activeMethod = result.user.preferredMfaMethod,
                        availableMethods = result.supportedMethods,
                        testHintCode = hint,
                        authSource = result.authSource
                    )
                }
                is AuthResult.Error -> {
                    _loginError.value = result.message
                }
            }
        }
    }

    fun switchMfaMethod(newMethod: MfaMethod) {
        val current = _screenState.value as? AuthScreenState.MfaPrompt ?: return
        val hint = when (newMethod) {
            MfaMethod.TOTP_AUTHENTICATOR -> authRepository.getDeterministicTotpCode(current.user.totpSecret)
            MfaMethod.SMS_RADIO_TOKEN -> authRepository.getPendingSmsCode(current.user.id) ?: authRepository.generateNewSmsCode(current.user.id)
            MfaMethod.BACKUP_CODE -> current.user.backupCodes.split(",").firstOrNull() ?: "8492-1049"
            MfaMethod.BIOMETRIC_KEY -> "Touch biometric sensor to authenticate"
        }
        _screenState.value = current.copy(
            activeMethod = newMethod,
            testHintCode = hint,
            error = null
        )
    }

    fun resendSmsCode() {
        val current = _screenState.value as? AuthScreenState.MfaPrompt ?: return
        val newCode = authRepository.generateNewSmsCode(current.user.id)
        _screenState.value = current.copy(
            testHintCode = newCode,
            error = null
        )
    }

    fun verifyMfa(code: String) {
        val current = _screenState.value as? AuthScreenState.MfaPrompt ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.verifyMfa(current.user, current.activeMethod, code)
            _isLoading.value = false

            result.onSuccess { verifiedUser ->
                _screenState.value = AuthScreenState.Authenticated(
                    user = verifiedUser,
                    authSource = current.authSource
                )
            }.onFailure { ex ->
                _screenState.value = current.copy(error = ex.message)
            }
        }
    }

    fun startPasswordRecovery() {
        _screenState.value = AuthScreenState.PasswordRecovery(step = RecoveryStep.SELECT_METHOD)
    }

    fun selectRecoveryMethod(method: String, email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = authRepository.requestPasswordRecovery(email, method)
            _isLoading.value = false

            when (res) {
                is RecoveryResult.CodeSent -> {
                    _screenState.value = AuthScreenState.PasswordRecovery(
                        step = RecoveryStep.ENTER_TOKEN,
                        email = res.email,
                        testSampleCode = res.sampleCodeForTesting,
                        successMessage = "Verification token dispatched to ${res.maskedContact}."
                    )
                }
                is RecoveryResult.QuestionPrompt -> {
                    _screenState.value = AuthScreenState.PasswordRecovery(
                        step = RecoveryStep.ANSWER_QUESTION,
                        email = res.email,
                        securityQuestion = res.question
                    )
                }
                is RecoveryResult.Verified -> {
                    _screenState.value = AuthScreenState.PasswordRecovery(
                        step = RecoveryStep.SET_NEW_PASSWORD,
                        email = res.user.email
                    )
                }
                is RecoveryResult.Error -> {
                    _screenState.value = AuthScreenState.PasswordRecovery(
                        step = RecoveryStep.SELECT_METHOD,
                        error = res.message
                    )
                }
            }
        }
    }

    fun verifySecurityAnswer(email: String, answer: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val ok = authRepository.verifySecurityAnswer(email, answer)
            _isLoading.value = false
            if (ok) {
                _screenState.value = AuthScreenState.PasswordRecovery(
                    step = RecoveryStep.SET_NEW_PASSWORD,
                    email = email,
                    successMessage = "Identity validated! Please define your new security credential."
                )
            } else {
                _screenState.value = AuthScreenState.PasswordRecovery(
                    step = RecoveryStep.ANSWER_QUESTION,
                    email = email,
                    error = "Security answer does not match corporate records."
                )
            }
        }
    }

    fun verifyRecoveryToken(email: String, token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val ok = authRepository.verifyRecoveryToken(email, token)
            _isLoading.value = false
            if (ok) {
                _screenState.value = AuthScreenState.PasswordRecovery(
                    step = RecoveryStep.SET_NEW_PASSWORD,
                    email = email,
                    successMessage = "Recovery authorization approved. Please enter a compliant new password."
                )
            } else {
                _screenState.value = AuthScreenState.PasswordRecovery(
                    step = RecoveryStep.ENTER_TOKEN,
                    email = email,
                    error = "Invalid or expired recovery code. Check dispatch records."
                )
            }
        }
    }

    fun submitNewPassword(email: String, newPass: String, confirmPass: String) {
        if (newPass != confirmPass) {
            val cur = _screenState.value as? AuthScreenState.PasswordRecovery
            _screenState.value = cur?.copy(error = "Passwords do not match.") ?: cur ?: AuthScreenState.Login
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val res = authRepository.resetPassword(email, newPass)
            _isLoading.value = false
            res.onSuccess {
                _screenState.value = AuthScreenState.PasswordRecovery(
                    step = RecoveryStep.SUCCESS_CONFIRMED,
                    email = email,
                    successMessage = "password_hash updated in public.user-login! Status set to 'active'."
                )
            }.onFailure { ex ->
                val cur = _screenState.value as? AuthScreenState.PasswordRecovery
                _screenState.value = cur?.copy(error = ex.message) ?: cur ?: AuthScreenState.Login
            }
        }
    }

    fun evaluatePasswordStrength(password: String): PasswordStrength {
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        var score = 0
        if (hasMinLength) score++
        if (hasUppercase) score++
        if (hasDigit) score++
        if (hasSpecial) score++

        val label = when (score) {
            4 -> "Very Strong (Enterprise Grade)"
            3 -> "Strong"
            2 -> "Moderate"
            1 -> "Weak"
            else -> "Insecure"
        }
        return PasswordStrength(score, hasMinLength, hasUppercase, hasDigit, hasSpecial, label)
    }

    fun navigateToLogin() {
        _screenState.value = AuthScreenState.Login
        _loginError.value = null
    }

    fun signOut() {
        _screenState.value = AuthScreenState.Login
        _loginError.value = null
    }

    fun autoFillProfile(identifier: String, pass: String) {
        login(identifier, pass)
    }
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(repository) as T
    }
}
