package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.DjezzyEmblem
import com.example.ui.data.SupabaseClient
import com.example.ui.theme.DjezzyRed
import com.example.ui.theme.DjezzyRedLight
import com.example.ui.theme.HazardRed
import com.example.ui.theme.ObsidianDarkBg
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.viewmodel.LoginFormValidationState
import com.example.ui.viewmodel.PasswordRecoveryState
import com.example.ui.viewmodel.RecoveryStep
import com.example.ui.viewmodel.SupabaseDbStatus

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (user: String, pass: String, rememberMe: Boolean) -> Unit,
    rememberMe: Boolean = true,
    savedUsername: String = "",
    onRememberMeChanged: (Boolean) -> Unit = {},
    onRegister: (
        firstName: String,
        surName: String,
        username: String,
        email: String,
        userId: String,
        entity: String,
        role: UserRole,
        password: String,
        confirmPassword: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onClearError: () -> Unit = {},
    formValidationState: LoginFormValidationState = LoginFormValidationState(),
    onUsernameChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {},
    onRegistrationEmailChanged: (String) -> Unit = {},
    passwordRecoveryState: PasswordRecoveryState = PasswordRecoveryState(),
    onRequestPasswordRecovery: (identifier: String) -> Unit = {},
    onVerifyRecoveryTokenAndResetPassword: (token: String, newPass: String, confirmPass: String) -> Unit = { _, _, _ -> },
    onResetRecoveryFlow: () -> Unit = {},
    availableCompanies: List<String> = com.example.ui.viewmodel.LoginViewModel.DEFAULT_COMPANIES,
    dbStatus: SupabaseDbStatus = SupabaseDbStatus(),
    diagnosticLogs: List<String> = emptyList(),
    isRunningDiagnostics: Boolean = false,
    onRunDiagnostics: () -> Unit = {},
    onSaveSupabaseConfig: (url: String, anonKey: String, tableName: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var isForgotPasswordMode by remember { mutableStateOf(false) }
    var showSupabaseConfigDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isRegisterMode || isForgotPasswordMode) {
        onClearError()
        if (isForgotPasswordMode) {
            onResetRecoveryFlow()
            isForgotPasswordMode = false
        } else {
            isRegisterMode = false
        }
    }

    // Login state
    var username by remember { mutableStateOf(savedUsername) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var localRememberMe by remember(rememberMe) { mutableStateOf(rememberMe) }

    LaunchedEffect(savedUsername) {
        if (savedUsername.isNotBlank() && username.isBlank()) {
            username = savedUsername
            onUsernameChanged(savedUsername)
        }
    }

    // Forgot Password state
    var recoveryIdentifier by remember { mutableStateOf("") }
    var recoveryTokenInput by remember { mutableStateOf("") }
    var recoveryNewPassword by remember { mutableStateOf("") }
    var recoveryConfirmPassword by remember { mutableStateOf("") }
    var isRecoveryPasswordVisible by remember { mutableStateOf(false) }

    // Self-Registration state (matches `public.user_login` columns)
    var regFirstName by remember { mutableStateOf("") }
    var regSurName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regUserId by remember { mutableStateOf("") }
    var regEntity by remember { mutableStateOf("Djezzy Telecom") }
    var regRole by remember { mutableStateOf(UserRole.LEAD_TECHNICIAN) }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var isRegPasswordVisible by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = DjezzyRed,
        unfocusedBorderColor = SlateCardBorder,
        errorBorderColor = HazardRed,
        focusedLabelColor = DjezzyRedLight,
        unfocusedLabelColor = Color(0xFF94A3B8),
        errorLabelColor = Color(0xFFFCA5A5),
        cursorColor = DjezzyRedLight,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color(0xFFE2E8F0),
        errorTextColor = Color.White
    )

    if (showSupabaseConfigDialog) {
        SupabaseConfigDialog(
            initialUrl = SupabaseClient.supabaseUrl.takeIf { SupabaseClient.isConfigured() }.orEmpty(),
            initialAnonKey = SupabaseClient.supabaseAnonKey.takeIf { SupabaseClient.isConfigured() }.orEmpty(),
            initialTable = dbStatus.activeTable.ifEmpty { "user_login" },
            diagnosticLogs = diagnosticLogs,
            isRunningDiagnostics = isRunningDiagnostics,
            onRunDiagnostics = onRunDiagnostics,
            onDismiss = { showSupabaseConfigDialog = false },
            onSave = { url, key, table ->
                onSaveSupabaseConfig(url, key, table)
            }
        )
    }

    if (isForgotPasswordMode) {
        ForgotPasswordScreen(
            recoveryState = passwordRecoveryState,
            initialEmailOrUsername = recoveryIdentifier.ifBlank { username },
            onRequestRecoveryToken = { emailOrUser ->
                recoveryIdentifier = emailOrUser
                onRequestPasswordRecovery(emailOrUser)
            },
            onVerifyTokenAndResetPassword = { token, newPass, confirmPass ->
                onVerifyRecoveryTokenAndResetPassword(token, newPass, confirmPass)
            },
            onBackToLogin = {
                if (passwordRecoveryState.targetUsername.isNotBlank()) {
                    username = passwordRecoveryState.targetUsername
                }
                onResetRecoveryFlow()
                onClearError()
                isForgotPasswordMode = false
            },
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // High-tech network globe background image uploaded by user
        Image(
            painter = painterResource(id = R.drawable.network),
            contentDescription = "Djezzy Telecom Network Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent gradient scrim identical to recovery page to show network globe
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55060512),
                            Color(0x770B081B),
                            Color(0x9912091E)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DjezzyEmblem(size = if (isRegisterMode) 64.dp else 80.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "DJEZZY FIELD OPS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isRegisterMode) {
                    "Create your company account in your Supabase user table to access Field Ops."
                } else {
                    "Please input your registered company username and password to proceed."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .testTag("login_helper_text")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Live Supabase Connection Status Badge (tappable to configure URL / Key / Table Name)
            SupabaseStatusPill(
                dbStatus = dbStatus,
                onClick = { showSupabaseConfigDialog = true }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF0182234)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(SlateCardBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    when {
                        isForgotPasswordMode -> {
                            // ================= FORGOT PASSWORD / RECOVERY VIEW =================
                            ForgotPasswordView(
                                recoveryState = passwordRecoveryState,
                                identifier = recoveryIdentifier,
                                onIdentifierChange = { recoveryIdentifier = it },
                                tokenInput = recoveryTokenInput,
                                onTokenInputChange = { recoveryTokenInput = it },
                                newPassword = recoveryNewPassword,
                                onNewPasswordChange = { recoveryNewPassword = it },
                                confirmPassword = recoveryConfirmPassword,
                                onConfirmPasswordChange = { recoveryConfirmPassword = it },
                                isPasswordVisible = isRecoveryPasswordVisible,
                                onTogglePasswordVisible = { isRecoveryPasswordVisible = !isRecoveryPasswordVisible },
                                textFieldColors = textFieldColors,
                                onRequestToken = { onRequestPasswordRecovery(recoveryIdentifier) },
                                onResetPassword = {
                                    onVerifyRecoveryTokenAndResetPassword(
                                        recoveryTokenInput,
                                        recoveryNewPassword,
                                        recoveryConfirmPassword
                                    )
                                },
                                onBackToLogin = {
                                    if (passwordRecoveryState.targetUsername.isNotBlank()) {
                                        username = passwordRecoveryState.targetUsername
                                    }
                                    onResetRecoveryFlow()
                                    onClearError()
                                    isForgotPasswordMode = false
                                }
                            )
                        }
                        !isRegisterMode -> {
                        // ================= LOGIN MODE =================
                        val usernameFieldError = formValidationState.usernameOrEmailError
                        val passwordFieldError = formValidationState.passwordError

                        // 1. Username / Email Text Input Field
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                onUsernameChanged(it)
                            },
                            label = { Text("Username or Email") },
                            placeholder = {
                                Text(
                                    text = "Enter company username or email",
                                    color = Color(0xFF64748B)
                                )
                            },
                            isError = usernameFieldError != null,
                            supportingText = {
                                if (usernameFieldError != null) {
                                    Text(
                                        text = usernameFieldError,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp,
                                        modifier = Modifier.testTag("login_username_error")
                                    )
                                } else {
                                    Text(
                                        text = "Use your registered company username (e.g., alex.rivera) or email (e.g., name@djezzy.dz)",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Username icon",
                                    tint = if (usernameFieldError != null) HazardRed else DjezzyRedLight
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Password Text Input Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                onPasswordChanged(it)
                            },
                            label = { Text("Password") },
                            placeholder = {
                                Text(
                                    text = "Enter your registered password",
                                    color = Color(0xFF64748B)
                                )
                            },
                            isError = passwordFieldError != null,
                            supportingText = {
                                if (passwordFieldError != null) {
                                    Text(
                                        text = passwordFieldError,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp,
                                        modifier = Modifier.testTag("login_password_error")
                                    )
                                } else {
                                    Text(
                                        text = "Enter your company password (hashed with SHA-256)",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Password icon",
                                    tint = if (passwordFieldError != null) HazardRed else DjezzyRedLight
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    onLogin(username, password, localRememberMe)
                                }
                            ),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        // Remember Me Checkbox & Forgot Password Link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Remember Me Checkbox with accessible touch target
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        localRememberMe = !localRememberMe
                                        onRememberMeChanged(localRememberMe)
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("remember_me_container")
                            ) {
                                Checkbox(
                                    checked = localRememberMe,
                                    onCheckedChange = { isChecked ->
                                        localRememberMe = isChecked
                                        onRememberMeChanged(isChecked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = DjezzyRed,
                                        uncheckedColor = Color(0xFF64748B),
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("remember_me_checkbox")
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Remember me",
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }

                            TextButton(
                                onClick = {
                                    onClearError()
                                    onResetRecoveryFlow()
                                    recoveryIdentifier = username
                                    recoveryTokenInput = ""
                                    recoveryNewPassword = ""
                                    recoveryConfirmPassword = ""
                                    isForgotPasswordMode = true
                                },
                                modifier = Modifier.testTag("forgot_password_link")
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    color = DjezzyRedLight,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Error Alert
                        AnimatedVisibility(visible = errorMessage != null) {
                            ErrorBanner(
                                errorMessage = errorMessage,
                                onConfigureClick = if (!dbStatus.isConnected) {
                                    { showSupabaseConfigDialog = true }
                                } else {
                                    null
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 3. Single Djezzy Red Login Button
                        Button(
                            onClick = { onLogin(username, password, localRememberMe) },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DjezzyRed,
                                contentColor = Color.White,
                                disabledContainerColor = DjezzyRed.copy(alpha = 0.4f),
                                disabledContentColor = Color.White.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "LOGIN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = SlateCardBorder.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Create Account Link -> Transitions to Registration View
                        TextButton(
                            onClick = {
                                onClearError()
                                isRegisterMode = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_account_link")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Create Account",
                                tint = DjezzyRedLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create Account",
                                color = DjezzyRedLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        }
                        else -> {
                        // ================= REGISTRATION VIEW =================
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CREATE ACCOUNT",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Inserts into public.${dbStatus.activeTable} (SHA-256 password_hash)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            IconButton(
                                onClick = { showSupabaseConfigDialog = true },
                                modifier = Modifier.testTag("open_supabase_config_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Configure Supabase Database",
                                    tint = DjezzyRedLight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Company / Entity Selection (Choose CompanyChip or enter custom company)
                        Text(
                            text = "Choose Company (entity) *",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableCompanies.forEach { companyOption ->
                                val isSelected = regEntity.equals(companyOption, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { regEntity = companyOption },
                                    label = {
                                        Text(
                                            text = companyOption,
                                            fontSize = 12.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = null,
                                            tint = if (isSelected) DjezzyRedLight else Color(0xFF94A3B8),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DjezzyRed.copy(alpha = 0.25f),
                                        selectedLabelColor = Color.White,
                                        labelColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regEntity,
                            onValueChange = { regEntity = it },
                            label = { Text("Company / Entity *") },
                            placeholder = { Text("Select above or type company name", color = Color(0xFF64748B)) },
                            supportingText = {
                                Text(
                                    text = "Saved to the 'entity' column in public.${dbStatus.activeTable}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = "Company",
                                    tint = DjezzyRedLight
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_company_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 1. Username (Required)
                        OutlinedTextField(
                            value = regUsername,
                            onValueChange = { regUsername = it },
                            label = { Text("Username *") },
                            placeholder = { Text("e.g. karim.mansouri", color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Username",
                                    tint = DjezzyRedLight
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_username_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Password (Required - Hashed with SHA-256)
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            label = { Text("Password *") },
                            placeholder = { Text("Hashed with SHA-256 for password_hash", color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Password",
                                    tint = DjezzyRedLight
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { isRegPasswordVisible = !isRegPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isRegPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_password_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Confirm Password (Optional or matches Password)
                        OutlinedTextField(
                            value = regConfirmPassword,
                            onValueChange = { regConfirmPassword = it },
                            label = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter password", color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Confirm Password",
                                    tint = DjezzyRedLight
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // First Name & Surname Row (Optional - auto-derived from username if blank)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = regFirstName,
                                onValueChange = { regFirstName = it },
                                label = { Text("First Name") },
                                placeholder = { Text("e.g. Karim", color = Color(0xFF64748B)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                colors = textFieldColors,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("register_first_name_input")
                            )

                            OutlinedTextField(
                                value = regSurName,
                                onValueChange = { regSurName = it },
                                label = { Text("Surname") },
                                placeholder = { Text("e.g. Mansouri", color = Color(0xFF64748B)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                colors = textFieldColors,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("register_sur_name_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Corporate Email (Optional)
                        val regEmailError = formValidationState.regEmailError
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = {
                                regEmail = it
                                onRegistrationEmailChanged(it)
                            },
                            label = { Text("Corporate Email") },
                            placeholder = { Text("e.g. karim.mansouri@djezzy.dz", color = Color(0xFF64748B)) },
                            isError = regEmailError != null,
                            supportingText = if (regEmailError != null) {
                                {
                                    Text(
                                        text = regEmailError,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                null
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = if (regEmailError != null) HazardRed else DjezzyRedLight
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_email_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Badge / User ID (Optional)
                        OutlinedTextField(
                            value = regUserId,
                            onValueChange = { regUserId = it },
                            label = { Text("User ID / Badge (Optional)") },
                            placeholder = { Text("Auto-generated if blank (e.g. USR-104)", color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = "User ID",
                                    tint = DjezzyRedLight
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_user_id_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Role Selector Chips
                        Text(
                            text = "Operational Role",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UserRole.values().forEach { roleOption ->
                                val selected = regRole == roleOption
                                FilterChip(
                                    selected = selected,
                                    onClick = { regRole = roleOption },
                                    label = {
                                        Text(
                                            text = roleOption.displayName,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DjezzyRed.copy(alpha = 0.25f),
                                        selectedLabelColor = Color.White,
                                        labelColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }

                        // Error Alert
                        AnimatedVisibility(visible = errorMessage != null) {
                            ErrorBanner(
                                errorMessage = errorMessage,
                                onConfigureClick = { showSupabaseConfigDialog = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Djezzy Red Create Account Button
                        Button(
                            onClick = {
                                onRegister(
                                    regFirstName,
                                    regSurName,
                                    regUsername,
                                    regEmail,
                                    regUserId,
                                    regEntity,
                                    regRole,
                                    regPassword,
                                    regConfirmPassword
                                )
                            },
                            enabled = !isLoading &&
                                regEntity.isNotBlank() &&
                                regUsername.isNotBlank() &&
                                regPassword.isNotBlank() &&
                                (regConfirmPassword.isBlank() || regPassword == regConfirmPassword),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DjezzyRed,
                                contentColor = Color.White,
                                disabledContainerColor = DjezzyRed.copy(alpha = 0.4f),
                                disabledContentColor = Color.White.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("register_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "CREATE ACCOUNT",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = {
                                onClearError()
                                isRegisterMode = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("back_to_login_button")
                        ) {
                            Text(
                                text = "Already have an account? Back to Login",
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForgotPasswordView(
    recoveryState: PasswordRecoveryState,
    identifier: String,
    onIdentifierChange: (String) -> Unit,
    tokenInput: String,
    onTokenInputChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    textFieldColors: androidx.compose.material3.TextFieldColors,
    onRequestToken: () -> Unit,
    onResetPassword: () -> Unit,
    onBackToLogin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "FORGOT PASSWORD",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 0.8.sp
        )
        Text(
            text = "Retrieves your email from public.${recoveryState.activeTable} and dispatches a secure recovery token.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (recoveryState.step) {
            RecoveryStep.REQUEST_TOKEN -> {
                OutlinedTextField(
                    value = identifier,
                    onValueChange = onIdentifierChange,
                    label = { Text("Username or Email") },
                    placeholder = { Text("e.g. alex.rivera or name@djezzy.dz", color = Color(0xFF64748B)) },
                    supportingText = {
                        Text(
                            text = "Looks up your registered email in public.user_login",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Username or Email",
                            tint = DjezzyRedLight
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onRequestToken() }),
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recovery_identifier_input")
                )

                AnimatedVisibility(visible = recoveryState.errorMessage != null) {
                    ErrorBanner(errorMessage = recoveryState.errorMessage)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRequestToken,
                    enabled = !recoveryState.isLoading && identifier.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DjezzyRed,
                        contentColor = Color.White,
                        disabledContainerColor = DjezzyRed.copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("send_recovery_token_button")
                ) {
                    if (recoveryState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "SEND RECOVERY TOKEN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            RecoveryStep.VERIFY_AND_RESET -> {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = OperationalEmerald.copy(alpha = 0.14f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(OperationalEmerald.copy(alpha = 0.5f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = recoveryState.statusMessage
                                ?: "Recovery token sent to ${recoveryState.targetEmail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD1FAE5),
                            fontWeight = FontWeight.SemiBold
                        )
                        recoveryState.dispatchedTokenPreview?.let { tokenCode ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Dispatched Token: $tokenCode",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OperationalEmerald
                                )
                                TextButton(
                                    onClick = { onTokenInputChange(tokenCode) },
                                    modifier = Modifier.testTag("autofill_recovery_token_button")
                                ) {
                                    Text(
                                        text = "Use Token",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = onTokenInputChange,
                    label = { Text("Recovery Token") },
                    placeholder = { Text("e.g. DZY-123456", color = Color(0xFF64748B)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "Recovery Token",
                            tint = DjezzyRedLight
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recovery_token_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("New Password (SHA-256)") },
                    placeholder = { Text("Enter new password", color = Color(0xFF64748B)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "New Password",
                            tint = DjezzyRedLight
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onTogglePasswordVisible) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recovery_new_password_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirm New Password") },
                    placeholder = { Text("Re-enter new password", color = Color(0xFF64748B)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Confirm New Password",
                            tint = DjezzyRedLight
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onResetPassword() }),
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recovery_confirm_password_input")
                )

                AnimatedVisibility(visible = recoveryState.errorMessage != null) {
                    ErrorBanner(errorMessage = recoveryState.errorMessage)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onResetPassword,
                    enabled = !recoveryState.isLoading && tokenInput.isNotBlank() && newPassword.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DjezzyRed,
                        contentColor = Color.White,
                        disabledContainerColor = DjezzyRed.copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("reset_password_submit_button")
                ) {
                    if (recoveryState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "VERIFY TOKEN & RESET PASSWORD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            RecoveryStep.COMPLETED -> {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = OperationalEmerald.copy(alpha = 0.16f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(OperationalEmerald)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = OperationalEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = recoveryState.statusMessage
                                ?: "Your password has been updated in public.user_login.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onBackToLogin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DjezzyRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("recovery_proceed_to_login_button")
                ) {
                    Text(
                        text = "PROCEED TO LOGIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recovery_back_to_login_button")
        ) {
            Text(
                text = "Back to Login",
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SupabaseStatusPill(
    dbStatus: SupabaseDbStatus,
    onClick: () -> Unit
) {
    val dotColor = when {
        dbStatus.isConnected -> OperationalEmerald
        dbStatus.isConfigured -> SafetyAmber
        else -> DjezzyRedLight
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFF1E293B).copy(alpha = 0.9f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(dotColor.copy(alpha = 0.55f))
        ),
        modifier = Modifier
            .clickable { onClick() }
            .testTag("supabase_status_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = dbStatus.statusText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE2E8F0),
                maxLines = 2
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (dbStatus.isConnected) Icons.Default.CheckCircle else Icons.Default.Settings,
                contentDescription = "Configure Supabase",
                tint = dotColor,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun SupabaseConfigDialog(
    initialUrl: String,
    initialAnonKey: String,
    initialTable: String,
    diagnosticLogs: List<String>,
    isRunningDiagnostics: Boolean,
    onRunDiagnostics: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (url: String, anonKey: String, tableName: String) -> Unit
) {
    var urlInput by remember { mutableStateOf(initialUrl) }
    var keyInput by remember { mutableStateOf(initialAnonKey) }
    var tableInput by remember { mutableStateOf(initialTable) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = DjezzyRed,
        unfocusedBorderColor = SlateCardBorder,
        focusedLabelColor = DjezzyRedLight,
        unfocusedLabelColor = Color(0xFF94A3B8),
        cursorColor = DjezzyRedLight,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color(0xFFE2E8F0)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181E2C),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFCBD5E1),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = DjezzyRedLight,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Supabase DB & Diagnostics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your Supabase Project URL, Anon Key, and Table Name, then click 'SAVE & TEST PING' to inspect the live connection:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("SUPABASE_URL") },
                    placeholder = { Text("https://xyzcompany.supabase.co", color = Color(0xFF64748B)) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_url_input")
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("SUPABASE_ANON_KEY") },
                    placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6...", color = Color(0xFF64748B)) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_key_input")
                )

                OutlinedTextField(
                    value = tableInput,
                    onValueChange = { tableInput = it },
                    label = { Text("Table Name (in public schema)") },
                    placeholder = { Text("user_login, user-login, or user", color = Color(0xFF64748B)) },
                    supportingText = {
                        Text(
                            text = "Probes: user_login, user-login, user - login, user, users",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supabase_table_input")
                )

                // Diagnostic Ping & Read Button
                Button(
                    onClick = {
                        if (urlInput.isNotBlank() && keyInput.isNotBlank()) {
                            onSave(urlInput, keyInput, tableInput)
                        } else {
                            onRunDiagnostics()
                        }
                    },
                    enabled = !isRunningDiagnostics,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_diagnostics_button")
                ) {
                    if (isRunningDiagnostics) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Running Ping & Table Read...", color = Color.White, fontSize = 12.sp)
                    } else {
                        Text("RUN DIAGNOSTIC PING & READ TEST", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Live Diagnostic Output Console
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0B1120),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(SlateCardBorder)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "DIAGNOSTIC LOGS (public.user_login):",
                            fontSize = 11.sp,
                            color = DjezzyRedLight,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (diagnosticLogs.isEmpty()) {
                            Text(
                                text = "Tap 'RUN DIAGNOSTIC PING & READ TEST' to test connection.",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8)
                            )
                        } else {
                            diagnosticLogs.forEach { logLine ->
                                val lineColor = when {
                                    logLine.contains("[ERROR]") -> Color(0xFFFCA5A5)
                                    logLine.contains("[WARN]") -> SafetyAmber
                                    logLine.contains("SUCCESS") -> OperationalEmerald
                                    else -> Color(0xFFCBD5E1)
                                }
                                Text(
                                    text = logLine,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = lineColor,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "If RLS blocks SELECT/INSERT, run in Supabase SQL Editor:",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ALTER TABLE public.\"${tableInput.ifBlank { "user-login" }}\" DISABLE ROW LEVEL SECURITY;",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = DjezzyRedLight
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(urlInput, keyInput, tableInput)
                    onDismiss()
                },
                enabled = urlInput.isNotBlank() && keyInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed),
                modifier = Modifier.testTag("save_supabase_config_button")
            ) {
                Text("SAVE & CLOSE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
private fun ErrorBanner(
    errorMessage: String?,
    onConfigureClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = HazardRed.copy(alpha = 0.15f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(HazardRed.copy(alpha = 0.5f))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error alert",
                    tint = HazardRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFCA5A5)
                )
            }
            if (onConfigureClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onConfigureClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = DjezzyRedLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Configure Supabase DB",
                        color = DjezzyRedLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
