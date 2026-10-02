package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TaskEntity
import com.example.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskByIdFlow(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE assignedTechId = :techId ORDER BY createdAt DESC")
    fun getTasksForTechnicianFlow(techId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, acceptedAt = :acceptedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus, acceptedAt: Long? = null)

    @Query("UPDATE tasks SET status = :status, resolutionNotes = :notes, signatureName = :sig, completedAt = :completedAt WHERE id = :taskId")
    suspend fun completeTask(taskId: String, status: TaskStatus, notes: String, sig: String, completedAt: Long)

    @Query("UPDATE tasks SET status = :status, rejectionReason = :reason WHERE id = :taskId")
    suspend fun declineTask(taskId: String, status: TaskStatus, reason: String)

    @Query("UPDATE tasks SET checklistJson = :checklistJson WHERE id = :taskId")
    suspend fun updateChecklist(taskId: String, checklistJson: String)

    @Query("UPDATE tasks SET assignedTechId = :techId, assignedTechName = :techName, status = :status WHERE id = :taskId")
    suspend fun dispatchTask(taskId: String, techId: String, techName: String, status: TaskStatus)

    @Query("UPDATE tasks SET latitude = :lat, longitude = :lng WHERE id = :taskId")
    suspend fun updateTaskLocation(taskId: String, lat: Double, lng: Double)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)
}
