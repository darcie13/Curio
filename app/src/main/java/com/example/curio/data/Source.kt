package com.example.curio.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Represents a specific piece of research material (such as a YouTube video, web article, or image).
 * * The ForeignKey constraint links each Source to a parent Project.
 * If the parent Project is deleted, all associated Sources are automatically removed from the
 * database via CASCADE deletion.
 */
@Entity(
    tableName = "sources",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Source(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Unique identifier for the source
    val projectId: Int, // The ID of the project this source belongs to
    val title: String, // The descriptive title of the source
    val url: String, // The web link or file path to the original content
    val thumbnailUrl: String?, // Optional preview image URL for the workspace UI
    val type: String // Category of the source (e.g., "VIDEO", "ARTICLE", "IMAGE")
)