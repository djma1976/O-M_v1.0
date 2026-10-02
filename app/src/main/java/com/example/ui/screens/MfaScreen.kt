package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MfaMethod
import com.example.data.model.UserEntity
import com.example.ui.components.DjezzyEmblem
import com.example.ui.components.DjezzyNetworkBackground
import com.example.ui.components.MfaCodeInput
import com.example.ui.theme.HazardRed
import com.example.ui.theme.ObsidianDarkBg
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TechBluePrimary
import com.example.ui.theme.TechBluePrimaryLight

@Composable
fun MfaScreen(
    user: UserEntity,
    activeMethod: MfaMethod,
    availableMethods: List<MfaMethod>,
    testHintCode: String,
    totpCountdownSeconds: Int,
    isLoading: Boolean,
    errorMessage: String?,
    onVerifyCode: (String) -> Unit,
    onSwitchMethod: (MfaMethod) -> Unit,
    onResendSms: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pinCode by remember { mutableStateOf("") }
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
            // Official Djezzy Logo Emblem
            DjezzyEmblem(size = 80.dp)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "DJEZZY 2FA VERIFICATION",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "Securing session for ${user.fullName} (${user.badgeNumber})",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Method Selector Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableMethods.take(3).forEach { method ->
                    FilterChip(
                        selected = activeMethod == method,
                        onClick = {
                            pinCode = ""
                            onSwitchMethod(method)
                        },
                        label = {
                            Text(
                                text = when (method) {
                                    MfaMethod.TOTP_AUTHENTICATOR -> "Authenticator"
                                    MfaMethod.SMS_RADIO_TOKEN -> "SMS/Radio"
                                    MfaMethod.BIOMETRIC_KEY -> "Biometrics"
                                    MfaMethod.BACKUP_CODE -> "Backup Code"
                                },
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                when (method) {
                                    MfaMethod.TOTP_AUTHENTICATOR -> Icons.Default.Timer
                                    MfaMethod.SMS_RADIO_TOKEN -> Icons.Default.PhoneAndroid
                                    MfaMethod.BIOMETRIC_KEY -> Icons.Default.Fingerprint
                                    MfaMethod.BACKUP_CODE -> Icons.Default.Key
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TechBluePrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Verification Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF0182234)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateCardBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = activeMethod.displayName.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TechBluePrimaryLight,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeMethod.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    when (activeMethod) {
                        MfaMethod.TOTP_AUTHENTICATOR -> {
                            // Dynamic 30-second rolling code indicator
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateCardBorder)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "AUTHENTICATOR CODE (TEST HELPER)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = testHintCode,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = OperationalEmerald,
                                            letterSpacing = 3.sp
                                        )
                                    }
                                    // Countdown Timer Pill
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = TechBluePrimary.copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Timer, contentDescription = null, tint = TechBluePrimaryLight, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${totpCountdownSeconds}s",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TechBluePrimaryLight
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // 6-digit PIN Input
                            MfaCodeInput(
                                code = pinCode,
                                onCodeChange = { pinCode = it },
                                onComplete = { onVerifyCode(it) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // One-tap Auto-Fill Helper for instant testing
                            TextButton(onClick = {
                                pinCode = testHintCode
                                onVerifyCode(testHintCode)
                            }) {
                                Text("Auto-Fill Current TOTP ($testHintCode)", color = TechBluePrimaryLight, fontSize = 12.sp)
                            }
                        }

                        MfaMethod.SMS_RADIO_TOKEN -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "DISPATCH RADIO TOKEN SENT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = onResendSms, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Resend Code", tint = TechBluePrimaryLight)
                                        }
                                    }
                                    Text(
                                        text = "Code dispatched to ${user.phone}: $testHintCode",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            MfaCodeInput(
                                code = pinCode,
                                onCodeChange = { pinCode = it },
                                onComplete = { onVerifyCode(it) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(onClick = {
                                pinCode = testHintCode
                                onVerifyCode(testHintCode)
                            }) {
                                Text("Auto-Fill Radio Token ($testHintCode)", color = TechBluePrimaryLight, fontSize = 12.sp)
                            }
                        }

                        MfaMethod.BIOMETRIC_KEY -> {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A))
                                    .border(2.dp, OperationalEmerald, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = "Fingerprint Sensor",
                                    tint = OperationalEmerald,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Field Device Biometric Sensor Ready",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onVerifyCode("BIOMETRIC_SUCCESS") },
                                colors = ButtonDefaults.buttonColors(containerColor = OperationalEmerald),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("biometric_verify_button")
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify Fingerprint / FIDO2 Key", fontWeight = FontWeight.Bold)
                            }
                        }

                        MfaMethod.BACKUP_CODE -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "AVAILABLE EMERGENCY BACKUP CODE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "8492-1049",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = SafetyAmber
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            MfaCodeInput(
                                code = pinCode,
                                onCodeChange = { pinCode = it },
                                onComplete = { onVerifyCode(it) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(onClick = {
                                pinCode = "84921049"
                                onVerifyCode("84921049")
                            }) {
                                Text("Auto-Fill Emergency Code (84921049)", color = SafetyAmber, fontSize = 12.sp)
                            }
                        }
                    }

                    // Error Message
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HazardRed.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = HazardRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Verify Action Button
                    Button(
                        onClick = { onVerifyCode(pinCode) },
                        enabled = !isLoading && (pinCode.length >= 6 || activeMethod == MfaMethod.BIOMETRIC_KEY),
                        colors = ButtonDefaults.buttonColors(containerColor = TechBluePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("verify_mfa_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("COMPLETE SECURE LOGIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to Login Button
            OutlinedButton(
                onClick = onBackToLogin,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Return to Login Portal", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        }
    }
}
