package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DjezzyEmblem
import com.example.ui.theme.DjezzyRed
import com.example.ui.theme.DjezzyRedLight
import com.example.ui.theme.HazardRed
import com.example.ui.theme.ObsidianDarkBg
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.SlateCardBorder
import com.example.ui.viewmodel.PasswordRecoveryState
import com.example.ui.viewmodel.RecoveryStep

/**
 * Dedicated Forgot Password screen styled with the Djezzy brand aesthetic (`DjezzyRed` `#E2001A`,
 * `ObsidianDarkBg`, and `DjezzyEmblem`). Allows users to input their corporate email address,
 * triggers secure token generation via the Supabase Auth & `public.user_login` recovery flow,
 * and lets them verify the token and set a new SHA-256 hashed password.
 */
@Composable
fun ForgotPasswordScreen(
    recoveryState: PasswordRecoveryState,
    initialEmailOrUsername: String = "",
    onRequestRecoveryToken: (emailOrUsername: String) -> Unit,
    onVerifyTokenAndResetPassword: (token: String, newPassword: String, confirmPassword: String) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToLogin()
    }

    var emailInput by remember { mutableStateOf(initialEmailOrUsername) }
    var emailValidationError by remember { mutableStateOf<String?>(null) }
    var tokenInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(initialEmailOrUsername) {
        if (emailInput.isBlank() && initialEmailOrUsername.isNotBlank()) {
            emailInput = initialEmailOrUsername
        }
    }

    LaunchedEffect(recoveryState.dispatchedTokenPreview) {
        if (tokenInput.isBlank() && !recoveryState.dispatchedTokenPreview.isNullOrBlank()) {
            tokenInput = recoveryState.dispatchedTokenPreview!!
        }
    }

    val emailRegex = remember { Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") }

    fun validateEmailOrIdentifier(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return "Email address cannot be empty."
        }
        if (trimmed.contains("@") && !emailRegex.matches(trimmed)) {
            return "Please enter a valid corporate email (e.g., name@djezzy.dz)."
        }
        if (trimmed.length < 3) {
            return "Please enter a valid registered email or company username."
        }
        return null
    }

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

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.network),
            contentDescription = "Network globe background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

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
            // Djezzy Brand Header
            DjezzyEmblem(size = 76.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "DJEZZY PASSWORD RECOVERY",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.4.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter your registered corporate email to generate a secure recovery token via Supabase Auth.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .testTag("forgot_password_subtitle")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step Progress Pill
            RecoveryStepBadge(step = recoveryState.step, tableName = recoveryState.activeTable)

            Spacer(modifier = Modifier.height(18.dp))

            // Main Recovery Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2181E2C)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(SlateCardBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    when (recoveryState.step) {
                        RecoveryStep.REQUEST_TOKEN -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = DjezzyRedLight,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "REQUEST RECOVERY TOKEN",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "We will verify your email in public.${recoveryState.activeTable} and dispatch a secure recovery token.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    emailValidationError = validateEmailOrIdentifier(it)
                                },
                                label = { Text("Corporate Email Address") },
                                placeholder = {
                                    Text(
                                        text = "e.g. alex.rivera@djezzy.dz",
                                        color = Color(0xFF64748B)
                                    )
                                },
                                isError = emailValidationError != null,
                                supportingText = {
                                    if (emailValidationError != null) {
                                        Text(
                                            text = emailValidationError!!,
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 12.sp,
                                            modifier = Modifier.testTag("forgot_password_email_error")
                                        )
                                    } else {
                                        Text(
                                            text = "Input your registered company email (or username) from public.${recoveryState.activeTable}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email icon",
                                        tint = if (emailValidationError != null) HazardRed else DjezzyRedLight
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        val err = validateEmailOrIdentifier(emailInput)
                                        emailValidationError = err
                                        if (err == null) {
                                            onRequestRecoveryToken(emailInput)
                                        }
                                    }
                                ),
                                colors = textFieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_password_email_input")
                            )

                            AnimatedVisibility(visible = recoveryState.errorMessage != null) {
                                RecoveryErrorBanner(errorMessage = recoveryState.errorMessage)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Djezzy Red Action Button
                            Button(
                                onClick = {
                                    val err = validateEmailOrIdentifier(emailInput)
                                    emailValidationError = err
                                    if (err == null) {
                                        onRequestRecoveryToken(emailInput)
                                    }
                                },
                                enabled = !recoveryState.isLoading,
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
                                    .testTag("forgot_password_send_token_button")
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
                                        fontSize = 15.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        RecoveryStep.VERIFY_AND_RESET -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = OperationalEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VERIFY TOKEN & NEW PASSWORD",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Token Dispatched Banner with Supabase Email Guidance
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = SolidColor(OperationalEmerald.copy(alpha = 0.5f))
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            tint = OperationalEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Email Dispatched to ${recoveryState.targetEmail}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Supabase Auth sends a 'Reset password' link to your inbox. Because standard Supabase emails contain a link rather than a text code, your verified session token has been automatically pre-filled below for instant verification.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 18.sp
                                    )
                                    recoveryState.dispatchedTokenPreview?.let { tokenCode ->
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0F172A),
                                            border = CardDefaults.outlinedCardBorder().copy(
                                                brush = SolidColor(OperationalEmerald.copy(alpha = 0.4f))
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "IN-APP RECOVERY TOKEN",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                    Text(
                                                        text = tokenCode,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = OperationalEmerald
                                                    )
                                                }
                                                TextButton(
                                                    onClick = { tokenInput = tokenCode },
                                                    modifier = Modifier.testTag("autofill_recovery_token_button")
                                                ) {
                                                    Text(
                                                        text = if (tokenInput == tokenCode) "✓ Pre-Filled" else "Auto-Fill Token",
                                                        color = if (tokenInput == tokenCode) OperationalEmerald else Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                label = { Text("Recovery Token or Reset Link") },
                                placeholder = { Text("Pre-filled token or paste link from email", color = Color(0xFF64748B)) },
                                supportingText = {
                                    Text(
                                        text = "Your token is pre-filled. You can also paste the 'Reset password' link from your email.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                },
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
                                    .testTag("forgot_password_token_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
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
                                    imeAction = ImeAction.Next
                                ),
                                colors = textFieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_password_new_password_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
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
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        onVerifyTokenAndResetPassword(
                                            tokenInput,
                                            newPassword,
                                            confirmPassword
                                        )
                                    }
                                ),
                                colors = textFieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_password_confirm_password_input")
                            )

                            AnimatedVisibility(visible = recoveryState.errorMessage != null) {
                                RecoveryErrorBanner(errorMessage = recoveryState.errorMessage)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val effectiveToken = tokenInput.ifBlank { recoveryState.dispatchedTokenPreview.orEmpty() }
                                    onVerifyTokenAndResetPassword(
                                        effectiveToken,
                                        newPassword,
                                        confirmPassword
                                    )
                                },
                                enabled = !recoveryState.isLoading && (tokenInput.isNotBlank() || !recoveryState.dispatchedTokenPreview.isNullOrBlank()) && newPassword.isNotBlank(),
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
                                    .testTag("forgot_password_reset_submit_button")
                            ) {
                                if (recoveryState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "VERIFY TOKEN & UPDATE PASSWORD",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }

                        RecoveryStep.COMPLETED -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = OperationalEmerald.copy(alpha = 0.16f),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = SolidColor(OperationalEmerald)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Password Reset Complete",
                                        tint = OperationalEmerald,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = recoveryState.statusMessage
                                            ?: "Password successfully updated in public.user_login!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

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
                                    .testTag("forgot_password_proceed_login_button")
                            ) {
                                Text(
                                    text = "PROCEED TO LOGIN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = SlateCardBorder.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onBackToLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_password_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Login",
                            tint = DjezzyRedLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Back to Login",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecoveryStepBadge(
    step: RecoveryStep,
    tableName: String
) {
    val label = when (step) {
        RecoveryStep.REQUEST_TOKEN -> "Step 1 of 2 • Enter Email (public.$tableName)"
        RecoveryStep.VERIFY_AND_RESET -> "Step 2 of 2 • Verify Token & Reset Password"
        RecoveryStep.COMPLETED -> "Recovery Complete • SHA-256 Password Updated"
    }
    val dotColor = when (step) {
        RecoveryStep.COMPLETED -> OperationalEmerald
        RecoveryStep.VERIFY_AND_RESET -> OperationalEmerald
        RecoveryStep.REQUEST_TOKEN -> DjezzyRedLight
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFF1E293B).copy(alpha = 0.9f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(dotColor.copy(alpha = 0.55f))
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
private fun RecoveryErrorBanner(errorMessage: String?) {
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
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    }
}
