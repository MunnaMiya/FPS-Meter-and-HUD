package com.growtotype.fpsmeterandhud.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.growtotype.fpsmeterandhud.model.GamingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM gaming_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<GamingSession>>

    @Query("SELECT * FROM gaming_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): GamingSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: GamingSession): Long

    @Delete
    suspend fun deleteSession(session: GamingSession)

    @Query("DELETE FROM gaming_sessions")
    suspend fun clearAll()
}
