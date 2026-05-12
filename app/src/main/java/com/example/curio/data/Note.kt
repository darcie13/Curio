package com.example.curio.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Represents a single note or annotation attached to a research source.
 * * The ForeignKey constraint ensures that if a Source is deleted, all its
 * associated notes are also deleted automatically (onDelete = CASCADE).
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Source::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Unique ID for each note
    val sourceId: Int, // Foreign key linking the note to a specific source
    val title: String = "New Note",
    val content: String, // The actual text body of the note
    val timestamp: Long = System.currentTimeMillis() // Tracks when the note was created
)