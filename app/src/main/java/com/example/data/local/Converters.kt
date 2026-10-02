package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.MfaMethod
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TechStatus
import com.example.data.model.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.LEAD_TECHNICIAN)

    @TypeConverter
    fun fromMfaMethod(value: MfaMethod): String = value.name

    @TypeConverter
    fun toMfaMethod(value: String): MfaMethod = runCatching { MfaMethod.valueOf(value) }.getOrDefault(MfaMethod.TOTP_AUTHENTICATOR)

    @TypeConverter
    fun fromTaskPriority(value: TaskPriority): String = value.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority = runCatching { TaskPriority.valueOf(value) }.getOrDefault(TaskPriority.P3_MEDIUM)

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = runCatching { TaskStatus.valueOf(value) }.getOrDefault(TaskStatus.PENDING_DISPATCH)

    @TypeConverter
    fun fromTechStatus(value: TechStatus): String = value.name

    @TypeConverter
    fun toTechStatus(value: String): TechStatus = runCatching { TechStatus.valueOf(value) }.getOrDefault(TechStatus.AVAILABLE)
}
