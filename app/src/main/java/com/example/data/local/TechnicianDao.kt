package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TechStatus
import com.example.data.model.TechnicianEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TechnicianDao {
    @Query("SELECT * FROM technicians ORDER BY name ASC")
    fun getAllTechniciansFlow(): Flow<List<TechnicianEntity>>

    @Query("SELECT * FROM technicians WHERE id = :id LIMIT 1")
    fun getTechnicianByIdFlow(id: String): Flow<TechnicianEntity?>

    @Query("SELECT * FROM technicians WHERE id = :id LIMIT 1")
    suspend fun getTechnicianById(id: String): TechnicianEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTechnicians(technicians: List<TechnicianEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTechnician(technician: TechnicianEntity)

    @Update
    suspend fun updateTechnician(technician: TechnicianEntity)

    @Query("UPDATE technicians SET status = :status, currentTaskId = :taskId, currentTaskTitle = :taskTitle, lastPingTimestamp = :timestamp WHERE id = :techId")
    suspend fun updateTechStatus(techId: String, status: TechStatus, taskId: String?, taskTitle: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE technicians SET latitude = :lat, longitude = :lng, headingDegrees = :heading, speedKmh = :speed, lastPingTimestamp = :timestamp WHERE id = :techId")
    suspend fun updateTechLocation(techId: String, lat: Double, lng: Double, heading: Float, speed: Float, timestamp: Long = System.currentTimeMillis())
}
