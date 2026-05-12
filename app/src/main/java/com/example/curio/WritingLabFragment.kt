package com.example.curio

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.curio.data.CurioDatabase
import com.example.curio.databinding.FragmentWritingLabBinding

/**
 * A distraction-free writing environment where users synthesize their research
 * into a final paper. Handles drafting (saving) and final publication to the archive.
 */
class WritingLabFragment : Fragment(R.layout.fragment_writing_lab) {

    private val viewModel: CurioViewModel by activityViewModels {
        val database = CurioDatabase.getDatabase(requireContext())
        CurioViewModelFactory(database.projectDao(), database.sourceDao(), database.noteDao())
    }

    // Flag to ensure the text fields are only populated once when the fragment is created
    private var contentLoaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWritingLabBinding.bind(view)
        val currentProjectId = arguments?.getInt("projectId") ?: -1

        // Load existing project data (title and draft content) into the editor
        viewModel.getProjectById(currentProjectId).observe(viewLifecycleOwner) { project ->
            project ?: return@observe
            if (!contentLoaded) {
                contentLoaded = true
                binding.editPaperTitle.setText(project.title)
                binding.editPaperBody.setText(project.finalPaperContent ?: "")
            }
        }

        binding.writingToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Toolbar actions for saving drafts and completing the research project
        binding.writingToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save_draft -> {
                    // Update the draft in the database without closing the screen
                    viewModel.saveDraft(currentProjectId, binding.editPaperBody.text.toString())
                    Toast.makeText(requireContext(), "Progress saved", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_upload -> {
                    // Finalize the project, moving it from 'Active' to 'Archived'
                    val paperText = binding.editPaperBody.text.toString()
                    if (paperText.isNotBlank()) {
                        viewModel.saveDraft(currentProjectId, paperText)
                        viewModel.completeProject(currentProjectId)
                        Toast.makeText(requireContext(), "Paper Published to Archive!", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp() // Return to Workspace
                    } else {
                        Toast.makeText(requireContext(), "Write something before publishing!", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
    }
}