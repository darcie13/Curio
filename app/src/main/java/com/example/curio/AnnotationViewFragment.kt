package com.example.curio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.curio.data.CurioDatabase
import com.example.curio.data.Note
import com.example.curio.databinding.FragmentAnnotationViewBinding
import com.example.curio.databinding.ItemNoteSavedBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.curio.utils.SwipeToDeleteCallback

/**
* Fragment responsible for displaying a source/article and allowing
* the user to create and view annotations/notes for that source.
*/
class AnnotationViewFragment : Fragment(R.layout.fragment_annotation_view) {

    // ViewBinding reference for safely accessing UI elements
    private var _binding: FragmentAnnotationViewBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel used to interact with database data
    private val viewModel: CurioViewModel by activityViewModels {
        val database = CurioDatabase.getDatabase(requireContext())
        CurioViewModelFactory(database.projectDao(), database.sourceDao(), database.noteDao())
    }

    // Stores the current source ID being viewed
    private var sourceId: Int = -1

    // RecyclerView adapter used to display saved notes
    private lateinit var notesAdapter: NotesAdapter

    // Inflates the fragment layout and initializes binding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnnotationViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Called after the fragment view has been created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve source ID and URL passed through navigation arguments
        sourceId = arguments?.getInt("sourceId") ?: -1
        val url = arguments?.getString("url") ?: ""

        // Observer to update the title of the annotation toolbar
        viewModel.getSourceById(sourceId).observe(viewLifecycleOwner) { source ->
            source?.let {
                binding.annotationToolbar.title = it.title
            }
        }

        // Initialize RecyclerView adapter for notes
        notesAdapter = NotesAdapter()
        binding.notesRecyclerView.adapter = notesAdapter

        // Swipe logic for swipe to delete functionality
        val swipeHandler = SwipeToDeleteCallback { position ->
            // Get the note using the position from the adapter's internal list
            val noteToDelete = viewModel.getNotesForSource(sourceId).value?.get(position)

            noteToDelete?.let {
                viewModel.deleteNote(it)
                Toast.makeText(requireContext(), "Note deleted", Toast.LENGTH_SHORT).show()
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.notesRecyclerView)

        // Configure WebView to load and display the source URL
        binding.sourceWebView.apply {
            @android.annotation.SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            loadUrl(url)
        }

        // Inflate toolbar menu options
        binding.annotationToolbar.inflateMenu(R.menu.annotation_menu)

        // Handle back navigation button in toolbar
        binding.annotationToolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Handle toolbar menu actions
        binding.annotationToolbar.setOnMenuItemClickListener { menuItem ->

            // Save note action
            if (menuItem.itemId == R.id.action_save_note) {

                // Retrieve note title and content from input fields
                val content = binding.annotationNotePad.text.toString()
                val title = binding.noteTitleInput.text.toString().ifBlank { "New Note" }

                // Only save notes that contain content
                if (content.isNotBlank()) {

                    // Insert note into database through ViewModel
                    viewModel.insertNote(sourceId, title, content)

                    // Clear input fields after successful save
                    binding.annotationNotePad.text.clear()
                    binding.noteTitleInput.setText("New Note")

                    // Display success message
                    Toast.makeText(requireContext(), "Note Saved!", Toast.LENGTH_SHORT).show()
                } else {

                    // Display validation message if note is empty
                    Toast.makeText(requireContext(), "Note cannot be empty", Toast.LENGTH_SHORT).show()
                }

                true
            } else false
        }

        // Toggle between note-writing mode and saved-notes mode
        binding.noteToggle.setOnCheckedChangeListener { _, isChecked ->

            // Show saved notes list
            if (isChecked) {
                binding.notesRecyclerView.visibility = View.VISIBLE
                binding.writingContainer.visibility = View.GONE

                // Load notes from database
                loadSavedNotes()

            } else {

                // Show note-writing interface
                binding.notesRecyclerView.visibility = View.GONE
                binding.writingContainer.visibility = View.VISIBLE
            }
        }
    }

    // Observes notes tied to the current source and updates RecyclerView
    private fun loadSavedNotes() {
        viewModel.getNotesForSource(sourceId).observe(viewLifecycleOwner) { notes ->
            notesAdapter.submitList(notes)
        }
    }

    // RecyclerView adapter responsible for rendering saved notes
    inner class NotesAdapter : RecyclerView.Adapter<NotesAdapter.ViewHolder>() {

        // Stores current list of notes
        private var notesList = listOf<Note>()

        // Updates adapter data and refreshes RecyclerView
        fun submitList(newNotes: List<Note>) {
            notesList = newNotes
            notifyDataSetChanged()
        }

        // ViewHolder containing references to note item UI
        inner class ViewHolder(val b: ItemNoteSavedBinding) : RecyclerView.ViewHolder(b.root)

        // Inflates individual note item layout
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ItemNoteSavedBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        // Binds note data to each RecyclerView item
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val note = notesList[position]

            // Set note title and body content
            holder.b.noteTitle.text = note.title
            holder.b.noteBody.text = note.content

            // Format and display note timestamp
            holder.b.noteTimestamp.text = SimpleDateFormat(
                "MMM d, yyyy · h:mm a", Locale.getDefault()
            ).format(Date(note.timestamp))
        }

        // Returns total number of notes in the adapter
        override fun getItemCount() = notesList.size
    }

    // Clears binding reference to prevent memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}