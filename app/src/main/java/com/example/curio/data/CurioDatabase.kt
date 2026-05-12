package com.example.curio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main database holder for the Curio app.
 * Defines the entities (tables) and provides access to the DAOs.
 * Version 3 includes the schema for Projects, Sources, and Notes.
 */
@Database(entities = [Project::class, Source::class, Note::class], version = 3, exportSchema = false)
abstract class CurioDatabase : RoomDatabase() {

    // Abstract functions to retrieve the Data Access Objects (DAOs) for each table
    abstract fun projectDao(): ProjectDAO
    abstract fun sourceDao(): SourceDAO
    abstract fun noteDao(): NoteDAO

    companion object {
        /**
         * Singleton prevents multiple instances of database opening at the same time,
         * which is expensive and could lead to data corruption.
         */
        @Volatile
        private var INSTANCE: CurioDatabase? = null

        /**
         * Gets the singleton instance of the CurioDatabase.
         * Uses thread-safe synchronized locking to ensure only one instance is created.
         */
        fun getDatabase(context: Context): CurioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CurioDatabase::class.java,
                    "curio_database"
                )
                    // Allows the database to be cleared and recreated if the schema version changes
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}