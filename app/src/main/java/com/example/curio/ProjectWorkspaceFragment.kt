package com.example.curio

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.curio.data.CurioDatabase
import com.example.curio.data.Project
import com.example.curio.data.Source
import com.example.curio.databinding.DialogNewProjectBinding
import com.example.curio.databinding.FragmentProjectWorkspaceBinding
import com.example.curio.databinding.ItemSourceBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.curio.utils.SwipeToDeleteCallback

/**
 * Fragment representing the core research environment for a specific project.
 * Displays a list of collected sources (videos, articles, etc.) and allows for
 * filtering, searching, and managing project settings.
 */
class ProjectWorkspaceFragment : Fragment(R.layout.fragment_project_workspace) {

    // ViewModel scoped to the Activity for data management
    private val viewModel: CurioViewModel by activityViewModels {
        val database = CurioDatabase.getDatabase(requireContext())
        CurioViewModelFactory(database.projectDao(), database.sourceDao(), database.noteDao())
    }

    // Holds a temporary URI if the user selects a new hero image in settings
    private var pendingHeroUri: Uri? = null

    /**
     * Photo picker registry to update the project's hero image.
     * Saves the selected image to local app storage.
     */
    private val pickMedia = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val file = java.io.File(requireContext().filesDir, "hero_${System.currentTimeMillis()}.jpg")
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                pendingHeroUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(), "${requireContext().packageName}.provider", file
                )
                Toast.makeText(requireContext(), "Image selected — tap Save to apply", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentProjectWorkspaceBinding.bind(view)
        val projectId = arguments?.getInt("projectId") ?: -1

        // Dynamically update the toolbar title based on the project name
        viewModel.getProjectById(projectId).observe(viewLifecycleOwner) { project ->
            binding.workspaceToolbar.title = project?.title ?: "Workspace"
        }

        binding.workspaceToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Toolbar menu handling for Search and Project Settings
        binding.workspaceToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    val searchView = menuItem.actionView as? SearchView
                    searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean = true
                        override fun onQueryTextChange(newText: String?): Boolean {
                            viewModel.setSourceSearchQuery(newText ?: "")
                            return true
                        }
                    })
                    true
                }
                R.id.action_project_settings -> {
                    showProjectSettingsDialog(projectId)
                    true
                }
                else -> false
            }
        }

        // Handle Chip-based filtering of sources by type (Video, Article, etc.)
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipVideos -> "VIDEO"
                R.id.chipArticles -> "ARTICLE"
                R.id.chipImages -> "IMAGE"
                R.id.chipPapers -> "PAPER"
                else -> "ALL"
            }
            viewModel.setSourceTypeFilter(filter)
        }

        // Adapter setup for the source list; navigates to Annotation screen on click
        val adapter = SourceAdapter { source ->
            val bundle = Bundle().apply {
                putInt("sourceId", source.id)
                putString("url", source.url)
            }
            findNavController().navigate(R.id.action_workspace_to_annotation, bundle)
        }

        binding.sourceRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.sourceRecyclerView.adapter = adapter

        // Swipe logic for swipe to delete functionality
        val swipeHandler = SwipeToDeleteCallback { position ->
            val sourceToDelete = adapter.items.getOrNull(position)
            sourceToDelete?.let {
                viewModel.deleteSource(it)
                Toast.makeText(requireContext(), "Source deleted", Toast.LENGTH_SHORT).show()
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.sourceRecyclerView)

        // Observe and display sources filtered by search query and type chip
        viewModel.getFilteredSources(projectId).observe(viewLifecycleOwner) { sources ->
            adapter.submitList(sources)
        }

        // FAB navigates to the Writing Lab to draft the final paper
        binding.pencilFab.setOnClickListener {
            val bundle = Bundle().apply { putInt("projectId", projectId) }
            findNavController().navigate(R.id.action_workspace_to_writing, bundle)
        }
    }

    /**
     * Displays a dialog allowing the user to rename the project,
     * change the hero image, or delete the project entirely.
     */
    private fun showProjectSettingsDialog(projectId: Int) {
        pendingHeroUri = null

        // Reuse the New Project dialog layout for editing
        val dialogBinding = DialogNewProjectBinding.inflate(layoutInflater)
        dialogBinding.dialogTitle.text = "Edit Project"
        dialogBinding.uploadHeroButton.text = "Change Hero Image"
        dialogBinding.createProjectButton.text = "Save Changes"

        // Pre-fill the input with the current project title
        val nameObserver = Observer<Project?> { project ->
            if (project != null && dialogBinding.projectNameInput.text.isNullOrEmpty()) {
                dialogBinding.projectNameInput.setText(project.title)
            }
        }
        val liveData = viewModel.getProjectById(projectId)
        liveData.observe(viewLifecycleOwner, nameObserver)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .setOnDismissListener {
                // Cleanup observer and state when dialog closes
                liveData.removeObserver(nameObserver)
                pendingHeroUri = null
            }
            .create()

        dialogBinding.uploadHeroButton.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }

        // Save updated project details to the database
        dialogBinding.createProjectButton.setOnClickListener {
            val newName = dialogBinding.projectNameInput.text?.toString()?.trim()
            if (!newName.isNullOrEmpty()) {
                viewModel.updateProjectDetails(
                    projectId = projectId,
                    newTitle = newName,
                    newImageUri = pendingHeroUri?.toString()
                )
                dialog.dismiss()
                Toast.makeText(requireContext(), "Project updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Show delete button (hidden by default in New Project flow) and handle deletion
        dialogBinding.deleteProjectButton.visibility = View.VISIBLE
        dialogBinding.deleteProjectButton.setOnClickListener {
            dialog.dismiss()
            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Delete Project?")
                .setMessage("This will permanently delete the project and all its sources and notes. This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteProject(projectId)
                    findNavController().navigateUp() // Return to HomeLibrary
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialogBinding.closeDialog.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /**
     * RecyclerView adapter for sources, utilizing a distinct icon based on source type.
     */
    inner class SourceAdapter(val onClick: (Source) -> Unit) :
        RecyclerView.Adapter<SourceAdapter.ViewHolder>() {

        var items = listOf<Source>()

        fun submitList(newList: List<Source>) {
            items = newList
            notifyDataSetChanged()
        }

        inner class ViewHolder(val b: ItemSourceBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val source = items[position]
            holder.b.sourceTitle.text = source.title
            holder.b.sourceTypeLabel.text = source.type

            // Select appropriate icon based on the source's content type
            val placeholderRes = when (source.type) {
                "VIDEO" -> R.drawable.ic_play_circle
                "IMAGE" -> R.drawable.ic_image
                "PAPER" -> R.drawable.ic_menu_book
                else    -> R.drawable.ic_description
            }

            // Load thumbnail or fallback to type-specific icon
            holder.b.sourceThumbnail.load(source.thumbnailUrl) {
                crossfade(true)
                placeholder(placeholderRes)
                error(placeholderRes)
            }

            holder.itemView.setOnClickListener { onClick(source) }
        }

        override fun getItemCount() = items.size
    }
}