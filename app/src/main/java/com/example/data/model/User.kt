package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole(val displayName: String) {
    DISPATCHER("Operations Dispatcher"),
    LEAD_TECHNICIAN("Lead Field Engineer"),
    HVAC_SPECIALIST("HVAC Specialist"),
    ELECTRICAL_TECH("High-Voltage Specialist"),
    MECHANICAL_TECH("Industrial Mechanic");

    companion object {
        fun fromString(raw: String): UserRole {
            val normalized = raw.trim().uppercase().replace("-", "_").replace(" ", "_")
            return values().firstOrNull {
                it.name.equals(normalized, ignoreCase = true) ||
                    it.displayName.equals(raw.trim(), ignoreCase = true)
            } ?: when {
                normalized.contains("DISPATCH") || normalized.contains("ADMIN") -> DISPATCHER
                normalized.contains("HVAC") -> HVAC_SPECIALIST
                normalized.contains("ELECTR") -> ELECTRICAL_TECH
                normalized.contains("MECH") -> MECHANICAL_TECH
                else -> LEAD_TECHNICIAN
            }
        }
    }
}

enum class MfaMethod(val displayName: String, val description: String) {
    TOTP_AUTHENTICATOR("Authenticator App (TOTP)", "Time-based dynamic 6-digit rolling code"),
    SMS_RADIO_TOKEN("Dispatch Radio / SMS Token", "Encrypted one-time passcode sent to field device"),
    BIOMETRIC_KEY("Biometric / Security Key", "Hardware-backed fingerprint or FIDO2 token"),
    BACKUP_CODE("Emergency Recovery Code", "Single-use emergency fallback codes")
}

/**
 * Mirrors the Supabase `public.user-login` table schema:
 * - id (serial primary key)
 * - entity (varchar 100)
 * - user_id (varchar 50, unique)
 * - first-name (varchar 100)
 * - sur-name (varchar 100)
 * - email (varchar 255, unique)
 * - username (varchar 100, unique)
 * - password_hash (varchar 255)
 * - role (varchar 50)
 * - created_at (timestamp without time zone)
 * - last_login (timestamp without time zone)
 * - status (varchar 50 default 'active')
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // Maps to `user_id` (unique varchar 50)
    val serialId: Int = 1, // Maps to `id` (serial)
    val entity: String = "Djezzy Telecom", // Maps to `entity` (varchar 100)
    val firstName: String = "", // Maps to `first-name` (varchar 100)
    val surName: String = "", // Maps to `sur-name` (varchar 100)
    val email: String, // Maps to `email` (varchar 255, unique)
    val username: String = "", // Maps to `username` (varchar 100, unique)
    val fullName: String, // Combined first-name + sur-name for UI display
    val badgeNumber: String, // Maps to `user_id`
    val role: UserRole, // Maps to `role` (varchar 50)
    val phone: String = "+213 770 00 00 00",
    val passwordHash: String, // Maps to `password_hash` (varchar 255)
    val createdAt: String = "2026-10-01 08:00:00", // Maps to `created_at`
    val lastLogin: String? = null, // Maps to `last_login`
    val status: String = "active", // Maps to `status` (default 'active')
    val mfaEnabled: Boolean = true,
    val preferredMfaMethod: MfaMethod = MfaMethod.TOTP_AUTHENTICATOR,
    val totpSecret: String = "JBSWY3DPEHPK3PXP",
    val backupCodes: String = "8492-1049,4820-9912,7301-6548,2954-8831",
    val securityQuestion: String = "What was your first field depot location?",
    val securityAnswerHash: String = "depot-7",
    val failedLoginAttempts: Int = 0,
    val isLocked: Boolean = false,
    val lockoutUntilTimestamp: Long = 0L
)
