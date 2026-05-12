package com.example.curio.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the 'sources' table.
 * Provides the interface for querying and modifying research materials linked to projects.
 */
@Dao
interface SourceDAO {

    /**
     * Retrieves all sources associated with a specific project ID.
     * Returns a Flow to ensure the Workspace UI updates automatically when sources are added or removed.
     */
    @Query("SELECT * FROM sources WHERE projectId = :projectId")
    fun getSourcesForProject(projectId: Int): Flow<List<Source>>

    /**
     * Retrieves a specific source by its unique ID.
     */
    @Query("SELECT * FROM sources WHERE id = :sourceId LIMIT 1")
    fun getSourceById(sourceId: Int): Flow<Source?>

    /**
     * Adds a new research source to the database.
     * If a source with the same ID already exists, it is replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: Source)

    /**
     * Removes a specific source from the project.
     */
    @Delete
    suspend fun deleteSource(source: Source)
}