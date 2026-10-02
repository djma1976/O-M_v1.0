package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.FieldOpsRepository
import com.example.ui.components.DjezzyNetworkBackground
import com.example.ui.data.SessionDataStoreRepository
import com.example.ui.data.SupabaseClient
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.theme.DjezzyRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianDarkBg
import com.example.ui.viewmodel.LoginViewModel
import com.example.ui.viewmodel.LoginViewModelFactory
import com.example.viewmodel.AiDiagnosticsViewModel
import com.example.viewmodel.FieldOpsViewModel
import com.example.viewmodel.FieldOpsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseClient.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    DjezzyNetworkBackground {
                        FieldShieldApp()
                    }
                }
            }
        }
    }
}

@Composable
fun FieldShieldApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appScope = rememberCoroutineScope()

    // Initialize Room Database, Session DataStore Repository, and Repositories
    val database = remember { AppDatabase.getDatabase(context, appScope) }
    val authRepo = remember { AuthRepository(database.userDao(), database.auditDao()) }
    val sessionRepo = remember { SessionDataStoreRepository(context) }
    val fieldOpsRepo = remember { FieldOpsRepository(database.taskDao(), database.technicianDao(), database.auditDao()) }

    // Initialize ViewModels
    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(authRepo, sessionRepo))
    val opsViewModel: FieldOpsViewModel = viewModel(factory = FieldOpsViewModelFactory(fieldOpsRepo))
    val aiViewModel: AiDiagnosticsViewModel = viewModel()

    val isRestoringSession by loginViewModel.isRestoringSession.collectAsStateWithLifecycle()
    val savedSessionUser by sessionRepo.savedUserFlow.collectAsStateWithLifecycle(initialValue = null)
    val isLoginLoading by loginViewModel.isLoading.collectAsStateWithLifecycle()
    val loginError by loginViewModel.errorMessage.collectAsStateWithLifecycle()
    val authenticatedUser by loginViewModel.authenticatedUser.collectAsStateWithLifecycle()
    val formValidationState by loginViewModel.formValidationState.collectAsStateWithLifecycle()
    val passwordRecoveryState by loginViewModel.passwordRecoveryState.collectAsStateWithLifecycle()
    val availableCompanies by loginViewModel.availableCompanies.collectAsStateWithLifecycle()
    val dbStatus by loginViewModel.dbStatus.collectAsStateWithLifecycle()
    val diagnosticLogs by loginViewModel.diagnosticLogs.collectAsStateWithLifecycle()
    val isRunningDiagnostics by loginViewModel.isRunningDiagnostics.collectAsStateWithLifecycle()
    val rememberMe by loginViewModel.rememberMe.collectAsStateWithLifecycle()
    val savedUsername by loginViewModel.savedUsername.collectAsStateWithLifecycle()

    val currentUser = authenticatedUser ?: savedSessionUser
    when {
        currentUser != null -> {
            BackHandler {
                // Keep inside dashboard or prompt sign out
            }
            MainDashboardScreen(
                currentUser = currentUser,
                opsViewModel = opsViewModel,
                aiViewModel = aiViewModel,
                onSignOut = { loginViewModel.clearSession() }
            )
        }
        isRestoringSession -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("session_restoring_screen"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = DjezzyRed,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Restoring Djezzy Field Session...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        else -> {
            LoginScreen(
                isLoading = isLoginLoading,
                errorMessage = loginError,
                rememberMe = rememberMe,
                savedUsername = savedUsername,
                onRememberMeChanged = { loginViewModel.setRememberMe(it) },
                onLogin = { username, password, rem -> loginViewModel.login(username, password, rem) },
                onRegister = { firstName, surName, username, email, userId, entity, role, password, confirmPassword ->
                    loginViewModel.registerAccount(
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
                },
                onClearError = { loginViewModel.clearMessages() },
                formValidationState = formValidationState,
                onUsernameChanged = { loginViewModel.onUsernameChanged(it) },
                onPasswordChanged = { loginViewModel.onPasswordChanged(it) },
                onRegistrationEmailChanged = { loginViewModel.onRegistrationEmailChanged(it) },
                passwordRecoveryState = passwordRecoveryState,
                onRequestPasswordRecovery = { identifier -> loginViewModel.requestPasswordRecovery(identifier) },
                onVerifyRecoveryTokenAndResetPassword = { token, newPass, confirmPass ->
                    loginViewModel.verifyRecoveryTokenAndResetPassword(token, newPass, confirmPass)
                },
                onResetRecoveryFlow = { loginViewModel.resetRecoveryFlow() },
                availableCompanies = availableCompanies,
                dbStatus = dbStatus,
                diagnosticLogs = diagnosticLogs,
                isRunningDiagnostics = isRunningDiagnostics,
                onRunDiagnostics = { loginViewModel.performDiagnosticPing() },
                onSaveSupabaseConfig = { url, anonKey, tableName ->
                    loginViewModel.saveSupabaseConfig(
                        context = context,
                        url = url,
                        anonKey = anonKey,
                        tableName = tableName
                    )
                }
            )
        }
    }
}
