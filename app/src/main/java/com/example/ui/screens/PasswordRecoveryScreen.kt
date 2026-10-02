package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DjezzyEmblem
import com.example.ui.components.DjezzyNetworkBackground
import com.example.ui.theme.HazardRed
import com.example.ui.theme.ObsidianDarkBg
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TechBluePrimary
import com.example.ui.theme.TechBluePrimaryLight
import com.example.viewmodel.PasswordStrength
import com.example.viewmodel.RecoveryStep

@Composable
fun PasswordRecoveryScreen(
    step: RecoveryStep,
    emailInitial: String,
    securityQuestion: String,
    testSampleCode: String,
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onSelectMethod: (method: String, email: String) -> Unit,
    onVerifyAnswer: (email: String, answer: String) -> Unit,
    onVerifyToken: (email: String, token: String) -> Unit,
    onSubmitNewPassword: (email: String, newPass: String, confirmPass: String) -> Unit,
    onEvaluateStrength: (String) -> PasswordStrength,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var emailInput by remember { mutableStateOf(emailInitial.ifEmpty { "tech.alex@fieldshield.com" }) }
    var answerInput by remember { mutableStateOf("tx-904") }
    var tokenInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val strength = remember(newPassword) { onEvaluateStrength(newPassword) }
    val scrollState = rememberScrollState()

    DjezzyNetworkBackground(
        modifier = modifier.padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DjezzyEmblem(size = 80.dp)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "DJEZZY CREDENTIAL RECOVERY",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "Field Operations Identity & Credential Recovery",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF0182234)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateCardBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Success Banner
                    AnimatedVisibility(visible = successMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OperationalEmerald.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OperationalEmerald, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = successMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                        }
                    }

                    // Error Banner
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HazardRed.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = HazardRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }

                    when (step) {
                        RecoveryStep.SELECT_METHOD -> {
                            Text(
                                text = "STEP 1: SELECT RECOVERY PATHWAY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TechBluePrimaryLight,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Corporate Email Address") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = TechBluePrimaryLight)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("recovery_email_input")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            RecoveryPathwayOption(
                                icon = Icons.Default.Email,
                                title = "Corporate Email Reset Token",
                                description = "Receive an encrypted 6-digit one-time authorization token with 10-min expiry.",
                                onClick = { onSelectMethod("EMAIL_TOKEN", emailInput) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            RecoveryPathwayOption(
                                icon = Icons.Default.QuestionAnswer,
                                title = "Security Challenge Question",
                                description = "Validate employee identity via predefined depot or equipment challenge question.",
                                onClick = { onSelectMethod("SECURITY_QUESTION", emailInput) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            RecoveryPathwayOption(
                                icon = Icons.Default.SupervisorAccount,
                                title = "Supervisor Radio Override",
                                description = "Emergency field recovery signed off by Lead Dispatcher on duty.",
                                onClick = { onSelectMethod("SUPERVISOR_BYPASS", emailInput) }
                            )
                        }

                        RecoveryStep.ENTER_EMAIL -> {
                            // Handled under Select Method
                        }

                        RecoveryStep.ANSWER_QUESTION -> {
                            Text(
                                text = "SECURITY CHALLENGE QUESTION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TechBluePrimaryLight
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "CHALLENGE QUESTION:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = securityQuestion.ifEmpty { "What was the asset tag of your first certified transformer?" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = answerInput,
                                onValueChange = { answerInput = it },
                                label = { Text("Your Answer") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("security_answer_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { answerInput = "tx-904" }) {
                                Text("Auto-Fill Demo Answer (tx-904)", color = TechBluePrimaryLight, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onVerifyAnswer(emailInput, answerInput) },
                                enabled = !isLoading && answerInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = TechBluePrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_answer_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text("VERIFY ANSWER", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        RecoveryStep.ENTER_TOKEN -> {
                            Text(
                                text = "ENTER RECOVERY AUTHORIZATION CODE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TechBluePrimaryLight
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (testSampleCode.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "DISPATCHED TOKEN (TEST ASSIST):",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B),
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = testSampleCode,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = OperationalEmerald
                                            )
                                        }
                                        TextButton(onClick = { tokenInput = testSampleCode }) {
                                            Text("Use Code", color = TechBluePrimaryLight)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                label = { Text("6-Digit Token or Supervisor Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("recovery_token_input")
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onVerifyToken(emailInput, tokenInput) },
                                enabled = !isLoading && tokenInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = TechBluePrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_token_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text("VALIDATE RECOVERY TOKEN", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        RecoveryStep.SET_NEW_PASSWORD -> {
                            Text(
                                text = "SET NEW COMPLIANT PASSWORD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TechBluePrimaryLight
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("New Password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth().testTag("new_password_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Password Strength Bar
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Password Strength", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                    Text(
                                        strength.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (strength.score) {
                                            4 -> OperationalEmerald
                                            3 -> SafetyAmber
                                            else -> HazardRed
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { strength.score / 4f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = when (strength.score) {
                                        4 -> OperationalEmerald
                                        3 -> SafetyAmber
                                        else -> HazardRed
                                    },
                                    trackColor = Color(0xFF1E293B)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Policy Requirements Checklist
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                RequirementPill("8+ Chars", strength.hasMinLength)
                                RequirementPill("Uppercase", strength.hasUppercase)
                                RequirementPill("Number", strength.hasDigit)
                                RequirementPill("Symbol", strength.hasSpecial)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm New Password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth().testTag("confirm_password_input")
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = { onSubmitNewPassword(emailInput, newPassword, confirmPassword) },
                                enabled = !isLoading && newPassword.length >= 8 && newPassword == confirmPassword,
                                colors = ButtonDefaults.buttonColors(containerColor = OperationalEmerald),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_password_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text("SAVE NEW PASSWORD", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        RecoveryStep.SUCCESS_CONFIRMED -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OperationalEmerald, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Password Reset Complete!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Your new password has been active. Any previous lockouts have been cleared.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = onBackToLogin,
                                    colors = ButtonDefaults.buttonColors(containerColor = TechBluePrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Sign In With New Password")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBackToLogin,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel & Return to Login", color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
private fun RecoveryPathwayOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateCardBorder)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TechBluePrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TechBluePrimaryLight, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun RequirementPill(label: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isMet) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isMet) OperationalEmerald else Color(0xFF64748B),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isMet) OperationalEmerald else Color(0xFF64748B),
            fontSize = 10.sp
        )
    }
}
