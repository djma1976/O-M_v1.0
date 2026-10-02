package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("""
        SELECT * FROM users 
        WHERE LOWER(email) = LOWER(:identifier) 
           OR LOWER(username) = LOWER(:identifier) 
           OR LOWER(id) = LOWER(:identifier) 
           OR LOWER(badgeNumber) = LOWER(:identifier) 
        LIMIT 1
    """)
    suspend fun getUserByIdentifier(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET lastLogin = :lastLogin, status = :status, failedLoginAttempts = 0, isLocked = 0 WHERE id = :userId")
    suspend fun recordSuccessfulLogin(userId: String, lastLogin: String, status: String = "active")

    @Query("UPDATE users SET failedLoginAttempts = failedLoginAttempts + 1 WHERE id = :userId")
    suspend fun incrementFailedAttempts(userId: String)

    @Query("UPDATE users SET failedLoginAttempts = 0 WHERE id = :userId")
    suspend fun resetFailedAttempts(userId: String)

    @Query("UPDATE users SET isLocked = :isLocked, status = :status, lockoutUntilTimestamp = :until WHERE id = :userId")
    suspend fun setAccountLockout(userId: String, isLocked: Boolean, status: String, until: Long)
}
