package com.passvaultsec.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.passvaultsec.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNotePermanently(id: String)

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteNote(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isPinned = :isPinned, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePinStatus(id: String, isPinned: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isLocked = :isLocked, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateLockStatus(id: String, isLocked: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isArchived = :isArchived, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateArchiveStatus(id: String, isArchived: Boolean, timestamp: Long = System.currentTimeMillis())
}
