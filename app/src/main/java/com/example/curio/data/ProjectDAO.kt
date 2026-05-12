package com.example.curio.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the 'projects' table.
 * Contains methods to filter projects by their completion status and perform CRUD operations.
 */
@Dao
interface ProjectDAO {

    /**
     * Retrieves all projects that are currently in progress (Active).
     * Uses Flow to observe real-time database changes.
     */
    @Query("SELECT * FROM projects WHERE isCompleted = 0")
    fun getActiveProjects(): Flow<List<Project>>

    /**
     * Retrieves all projects that have been finalized and published (Archived).
     */
    @Query("SELECT * FROM projects WHERE isCompleted = 1")
    fun getArchivedProjects(): Flow<List<Project>>

    /**
     * Searches through active projects where the title matches the provided query string.
     */
    @Query("SELECT * FROM projects WHERE isCompleted = 0 AND title LIKE :query")
    fun searchActiveProjects(query: String): Flow<List<Project>>

    /**
     * Searches through archived projects where the title matches the provided query string.
     */
    @Query("SELECT * FROM projects WHERE isCompleted = 1 AND title LIKE :query")
    fun searchArchivedProjects(query: String): Flow<List<Project>>

    /**
     * Fetches a specific project by its ID directly without using a Flow.
     * Useful for one-time background tasks or state initialization.
     */
    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectByIdDirect(id: Int): Project?

    /**
     * Creates a new project or replaces an existing one.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    /**
     * Updates project details, such as changing a title or saving a draft.
     */
    @Update
    suspend fun updateProject(project: Project)

    /**
     * Deletes a project and, due to foreign key constraints in other tables,
     * triggers a cascade delete of its associated sources and notes.
     */
    @Delete
    suspend fun deleteProject(project: Project)
}