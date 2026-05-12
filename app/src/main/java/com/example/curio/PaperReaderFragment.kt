package com.example.curio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.curio.databinding.FragmentPaperReaderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A dedicated fragment for reading the final text of an archived project.
 * It functions as a clean, read-only "Paper" view for completed research.
 */
class PaperReaderFragment : Fragment(R.layout.fragment_paper_reader) {

    private var _binding: FragmentPaperReaderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaperReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve data passed from the Archive fragment via NavController arguments
        val title       = arguments?.getString("title") ?: "Untitled"
        val body        = arguments?.getString("body")  ?: ""
        val completedAt = arguments?.getLong("completedAt", -1L) ?: -1L

        // Set the toolbar title and back button behavior
        binding.readerToolbar.title = title
        binding.readerToolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Populate the UI components with the project data
        binding.readerTitle.text = title

        // Show the paper body, or a fallback message if the content is empty
        binding.readerBody.text  = body.ifBlank { "(No content was saved for this paper.)" }

        // Format the millisecond timestamp into a human-readable "Published" date
        binding.readerDate.text  = if (completedAt > 0) {
            "Published ${SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(completedAt))}"
        } else "Published —"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up binding to prevent memory leaks in the fragment lifecycle
        _binding = null
    }
}