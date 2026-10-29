package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NexusDao {
    @Query("SELECT * FROM agent_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<AgentMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AgentMessageEntity): Long

    @Query("DELETE FROM agent_messages")
    suspend fun clearMessages()

    @Query("SELECT * FROM automation_macros ORDER BY id DESC")
    fun getAllMacros(): Flow<List<AutomationMacroEntity>>

    @Query("SELECT * FROM automation_macros ORDER BY id DESC")
    suspend fun getAllMacrosOnce(): List<AutomationMacroEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: AutomationMacroEntity): Long

    @Update
    suspend fun updateMacro(macro: AutomationMacroEntity)

    @Query("DELETE FROM automation_macros WHERE id = :id")
    suspend fun deleteMacroById(id: Long)

    @Query("SELECT * FROM execution_logs ORDER BY timestamp DESC LIMIT 80")
    fun getRecentLogs(): Flow<List<ExecutionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExecutionLogEntity): Long

    @Query("DELETE FROM execution_logs")
    suspend fun clearLogs()
}
