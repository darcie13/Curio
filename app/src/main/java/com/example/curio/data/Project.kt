package com.example.curio.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a research project within the Curio app.
 * This entity stores metadata about the project, its current status (Active vs. Archived),
 * and the final compiled research content.
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Unique identifier for the project
    val title: String, // The name of the research project
    val heroImageUri: String?, // Optional URI for the project's cover/hero image
    val isCompleted: Boolean = false, // Flag to determine if project is in the Workspace or Archive
    val finalPaperContent: String? = null, // The synthesized text written in the Writing Lab
    val completedAt: Long? = null // Timestamp marking when the project was moved to the Archive
)