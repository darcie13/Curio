package com.example.curio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.curio.data.CurioDatabase
import com.example.curio.data.Project
import com.example.curio.databinding.FragmentHomeLibraryBinding
import com.example.curio.databinding.DialogNewProjectBinding
import com.example.curio.databinding.ItemProjectBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * The main landing screen of the app. Displays a list of active research projects
 * and allows users to create new ones or search existing ones.
 */
class HomeLibraryFragment : Fragment() {

    // View binding for the fragment's layout
    private var _binding: FragmentHomeLibraryBinding? = null
    private val binding get() = _binding!!

    // Temporarily stores the URI of an image selected for a new project hero section
    private var selectedImageUri: Uri? = null

    /**
     * Registers a contract to select an image from the device's photo gallery.
     * When an image is picked, it is copied to the app's internal storage for persistence.
     */
    private val pickMedia = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                // Create a unique file name in internal storage
                val file = java.io.File(requireContext().filesDir, "hero_${System.currentTimeMillis()}.jpg")
                // Stream data from the gallery URI to the local file
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                // Generate a FileProvider URI so the app can access the newly saved file
                selectedImageUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(), "${requireContext().packageName}.provider", file
                )
                Toast.makeText(requireContext(), "Image Selected!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Shared ViewModel initialized with the database DAOs
    private val viewModel: CurioViewModel by activityViewModels {
        val database = CurioDatabase.getDatabase(requireContext())
        CurioViewModelFactory(database.projectDao(), database.sourceDao(), database.noteDao())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the adapter with a click listener to navigate to the specific Workspace
        val adapter = ProjectAdapter { project ->
            val bundle = Bundle().apply { putInt("projectId", project.id) }
            findNavController().navigate(R.id.action_home_to_workspace, bundle)
        }

        // Setup RecyclerView with a standard vertical list
        binding.projectRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.projectRecyclerView.adapter = adapter

        // Observe the search-filtered list of active projects
        viewModel.filteredActiveProjects.observe(viewLifecycleOwner) { projects ->
            adapter.submitList(projects)
            // Show empty state UI if no projects match or exist
            binding.emptyStateLayout.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
        }

        // Configure toolbar actions for searching and navigating to the archive
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    val searchView = menuItem.actionView as? SearchView
                    searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean = true
                        override fun onQueryTextChange(newText: String?): Boolean {
                            viewModel.setProjectSearchQuery(newText ?: "")
                            return true
                        }
                    })
                    true
                }
                R.id.action_archive -> {
                    findNavController().navigate(R.id.action_home_to_archive)
                    true
                }
                else -> false
            }
        }

        // Floating Action Button to trigger the new project creation flow
        binding.newProjectFab.setOnClickListener { showNewProjectDialog() }
    }

    /**
     * Builds and displays a Material Design dialog to input project details.
     */
    private fun showNewProjectDialog() {
        val dialogBinding = DialogNewProjectBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .create()

        // Button inside dialog to trigger the system photo picker
        dialogBinding.uploadHeroButton.setOnClickListener {
            pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(
                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
            ))
        }

        // Validate and save the new project to the database
        dialogBinding.createProjectButton.setOnClickListener {
            val name = dialogBinding.projectNameInput.text.toString().trim()
            if (name.isNotEmpty()) {
                viewModel.insertProject(name, selectedImageUri?.toString())
                selectedImageUri = null // Reset for next use
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter a project name", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.closeDialog.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /**
     * Adapter for the main project list.
     */
    inner class ProjectAdapter(private val onClick: (Project) -> Unit) :
        RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {

        private var items = listOf<Project>()

        fun submitList(newList: List<Project>) {
            items = newList
            notifyDataSetChanged()
        }

        inner class ViewHolder(val itemBinding: ItemProjectBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val project = items[position]
            holder.itemBinding.projectTitle.text = project.title
            // Load the hero image; if null or error, shows default background
            holder.itemBinding.heroImageView.load(project.heroImageUri) {
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }
            holder.itemView.setOnClickListener { onClick(project) }
        }

        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}