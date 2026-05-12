package com.example.curio

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.curio.data.CurioDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * The main entry point for the application.
 * This activity also handles incoming shared text from other apps (like browsers).
 */
class MainActivity : AppCompatActivity() {

    // ViewModel scoped to the Activity lifecycle
    private val viewModel: CurioViewModel by viewModels {
        val database = CurioDatabase.getDatabase(applicationContext)
        CurioViewModelFactory(database.projectDao(), database.sourceDao(), database.noteDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if the activity was started via a "Share" intent
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            handleSharedText(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle the intent if the activity is already running in the background
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            handleSharedText(intent)
        }
    }

    /**
     * Processes text shared from external apps and allows the user to save it to a project.
     */
    private fun handleSharedText(intent: Intent) {
        // Extract the shared text; exit if empty
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return

        // Basic logic to extract a URL from the shared text if one exists
        val sharedUrl = sharedText.trim().split("\\s+".toRegex()).lastOrNull {
            it.startsWith("http://") || it.startsWith("https://")
        } ?: sharedText.trim()

        var dialogShown = false

        // Fetch active projects to show a selection dialog
        viewModel.activeProjects.observe(this) { projects ->
            // Ensure the dialog is only created once even if LiveData triggers multiple times
            if (dialogShown) return@observe
            dialogShown = true

            // If no projects exist, warn the user and close the share handler
            if (projects.isEmpty()) {
                Toast.makeText(this, "Create a project in Curio first!", Toast.LENGTH_LONG).show()
                finish()
                return@observe
            }

            // Map project titles for the dialog list
            val titles = projects.map { it.title }.toTypedArray()

            // Show a dialog for the user to pick which project to save the link to
            MaterialAlertDialogBuilder(this)
                .setTitle("Save to Project")
                .setItems(titles) { _, which ->
                    val selectedProject = projects[which]
                    Toast.makeText(this, "Saving to \"${selectedProject.title}\"…", Toast.LENGTH_SHORT).show()

                    // Insert the shared source into the database under the selected project
                    viewModel.insertSourceWithMetadata(selectedProject.id, sharedUrl)

                    // Briefly delay closing the activity so the user can see the "Saving" toast
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 2000)
                }
                .setNegativeButton("Cancel") { _, _ -> finish() }
                .setOnCancelListener { finish() }
                .show()
        }
    }
}