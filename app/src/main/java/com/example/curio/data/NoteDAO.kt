package com.example.curio.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the 'notes' table.
 * Defines the SQL queries and database operations for research annotations.
 */
@Dao
interface NoteDAO {

    /**
     * Retrieves all notes for a specific source, ordered by the most recent first.
     * Returns a Flow to provide real-time updates to the UI whenever data changes.
     */
    @Query("SELECT * FROM notes WHERE sourceId = :sourceId ORDER BY timestamp DESC")
    fun getNotesForSource(sourceId: Int): Flow<List<Note>>

    /**
     * Inserts a new note or replaces an existing one if the ID conflicts.
     * This is a suspend function to ensure it runs off the main thread.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    /**
     * Permanently removes a note from the database.
     */
    @Delete
    suspend fun deleteNote(note: Note)
}