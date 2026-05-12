package com.example.curio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.curio.data.CurioDatabase
import com.example.curio.data.Project
import com.example.curio.databinding.FragmentLibraryArchiveBinding
import com.example.curio.databinding.ItemArchivePaperBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment responsible for displaying a grid of archived (completed) projects.
 * Users can search through their archive and tap on projects to read them.
 */
class LibraryArchiveFragment : Fragment(R.layout.fragment_library_archive) {

    // View binding reference to access UI elements safely
    private var _binding: FragmentLibraryArchiveBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel scoped to the Activity to maintain state across fragments
    private val viewModel: CurioViewModel by activityViewModels {
        val database = CurioDatabase.getDatabase(requireContext())
        CurioViewModelFactory(database.projectDao(), database.sourceDao(), database.noteDao())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment using View Binding
        _binding = FragmentLibraryArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the back button in the toolbar
        binding.archiveToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Configure the search menu item functionality
        binding.archiveToolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_search) {
                val searchView = menuItem.actionView as? SearchView
                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = true

                    // Update the search query in the ViewModel as the user types
                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.setArchiveSearchQuery(newText ?: "")
                        return true
                    }
                })
                true
            } else false
        }

        // Initialize the adapter with a click listener to navigate to the Reader screen
        val adapter = ArchiveAdapter { project ->
            val bundle = Bundle().apply {
                putString("title",       project.title)
                putString("body",        project.finalPaperContent ?: "")
                putLong("completedAt",   project.completedAt ?: -1L)
            }
            findNavController().navigate(R.id.action_archive_to_reader, bundle)
        }

        // Set up the RecyclerView with a 2-column grid layout
        binding.archiveRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.archiveRecyclerView.adapter = adapter

        // Observe the filtered list of archived projects and update the UI when it changes
        viewModel.filteredArchivedProjects.observe(viewLifecycleOwner) { projects ->
            adapter.submitList(projects)
        }
    }

    /**
     * Inner class for the RecyclerView adapter to manage archived project items.
     */
    inner class ArchiveAdapter(private val onClick: (Project) -> Unit) :
        RecyclerView.Adapter<ArchiveAdapter.ViewHolder>() {

        private var items = listOf<Project>()

        // Update the list of projects and refresh the display
        fun submitList(newList: List<Project>) {
            items = newList
            notifyDataSetChanged()
        }

        inner class ViewHolder(val b: ItemArchivePaperBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ItemArchivePaperBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val project = items[position]

            // Set the paper title
            holder.b.paperTitle.text = project.title

            // Load the hero image using the Coil library with placeholders and error states
            holder.b.archiveHeroImage.load(project.heroImageUri) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }

            // Format and display the completion date
            holder.b.paperDate.text = project.completedAt?.let {
                "Published ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))}"
            } ?: "Published —"

            // Trigger the click callback when an item is tapped
            holder.itemView.setOnClickListener { onClick(project) }
        }

        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear binding reference to prevent memory leaks
        _binding = null
    }
}