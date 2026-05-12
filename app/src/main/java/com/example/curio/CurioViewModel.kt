package com.example.curio

import androidx.lifecycle.*
import com.example.curio.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

/**
 * Main ViewModel responsible for managing project, source,
 * and note data throughout the application.
 */
class CurioViewModel(
    private val projectDao: ProjectDAO,
    private val sourceDao: SourceDAO,
    private val noteDao: NoteDAO
) : ViewModel() {

    // Stores current search query for active projects
    private val projectSearchQuery = MutableStateFlow("")

    // Stores current search query for sources
    private val sourceSearchQuery  = MutableStateFlow("")

    // Stores current source type filter selection
    private val sourceTypeFilter   = MutableStateFlow("ALL")

    // Stores search query for archived projects
    private val archiveSearchQuery = MutableStateFlow("")

    // Dynamically filters active projects based on search query
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filteredActiveProjects: LiveData<List<Project>> = projectSearchQuery.flatMapLatest { query ->
        if (query.isEmpty()) projectDao.getActiveProjects()
        else projectDao.searchActiveProjects("%$query%")
    }.asLiveData()

    // Dynamically filters archived projects based on search query
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filteredArchivedProjects: LiveData<List<Project>> = archiveSearchQuery.flatMapLatest { query ->
        if (query.isEmpty()) projectDao.getArchivedProjects()
        else projectDao.searchArchivedProjects("%$query%")
    }.asLiveData()

    // Updates active project search query
    fun setProjectSearchQuery(query: String) { projectSearchQuery.value = query }

    // Updates archived project search query
    fun setArchiveSearchQuery(query: String) { archiveSearchQuery.value = query }

    // Returns sources filtered by both search query and source type
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getFilteredSources(projectId: Int): LiveData<List<Source>> =
        combine(sourceSearchQuery, sourceTypeFilter) { q, t -> q to t }
            .flatMapLatest { (query, type) ->

                // Retrieve all project sources and apply local filtering
                sourceDao.getSourcesForProject(projectId).map { sources ->
                    sources.filter { s ->
                        s.title.contains(query, ignoreCase = true) &&
                                (type == "ALL" || s.type == type)
                    }
                }
            }.asLiveData()

    // Updates source search query
    fun setSourceSearchQuery(query: String) { sourceSearchQuery.value = query }

    // Updates source type filter
    fun setSourceTypeFilter(type: String)   { sourceTypeFilter.value = type }

    // Retrieves a single project by ID from active projects
    fun getProjectById(projectId: Int): LiveData<Project?> =
        projectDao.getActiveProjects().map { list -> list.find { it.id == projectId } }.asLiveData()

    // Fetches a specific source by its ID
    fun getSourceById(sourceId: Int): LiveData<Source?> {
        // This assumes your SourceDAO has a query for a single source,
        // or you can use a transformation on your existing source list.
        return sourceDao.getSourceById(sourceId).asLiveData()
    }

    // LiveData list of all active projects
    val activeProjects:   LiveData<List<Project>> = projectDao.getActiveProjects().asLiveData()

    // LiveData list of archived/completed projects
    val archivedProjects: LiveData<List<Project>> = projectDao.getArchivedProjects().asLiveData()

    // Inserts a new project into the database
    fun insertProject(title: String, imageUri: String?) {
        viewModelScope.launch {
            projectDao.insertProject(Project(title = title, heroImageUri = imageUri))
        }
    }

    // Inserts a source while automatically generating metadata
    fun insertSourceWithMetadata(projectId: Int, url: String) {

        // Run networking/database operations on IO dispatcher
        viewModelScope.launch(Dispatchers.IO) {

            android.util.Log.d("CURIO_DEBUG", "insertSourceWithMetadata called with url: $url")

            // Determine source type based on URL content
            val type = when {
                url.contains("youtube.com") || url.contains("youtu.be") -> "VIDEO"
                url.contains(".jpg") || url.contains(".png") || url.contains(".gif") -> "IMAGE"
                url.contains(".pdf") -> "PAPER"
                else -> "ARTICLE"
            }

            // Attempt to fetch webpage title from HTML metadata
            val title: String = try {

                // Open connection to webpage
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection

                // Configure request headers and timeout settings
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                // Read webpage HTML content
                val stream = connection.inputStream.bufferedReader().readText()

                // Extract title using regex
                val match = Regex("<title[^>]*>(.*?)</title>", RegexOption.IGNORE_CASE).find(stream)

                android.util.Log.d("CURIO_DEBUG", "Title found: ${match?.groupValues?.get(1)}")

                // Fallback to website host if title cannot be extracted
                match?.groupValues?.get(1)?.trim()?.take(100)
                    ?: java.net.URI(url).host
                    ?: "Saved Link"

            } catch (e: Exception) {

                // Log failure and use fallback title
                android.util.Log.e("CURIO_DEBUG", "Failed to fetch title: ${e.message}")

                try {
                    java.net.URI(url).host ?: "Saved Link"
                } catch (e2: Exception) {
                    "Saved Link"
                }
            }

            // Insert source into database
            sourceDao.insertSource(Source(
                projectId = projectId,
                title = title,
                url = url,
                thumbnailUrl = null,
                type = type
            ))

            android.util.Log.d("CURIO_DEBUG", "Source inserted: $title")
        }
    }

    // Retrieves all notes associated with a source
    fun getNotesForSource(sourceId: Int): LiveData<List<Note>> =
        noteDao.getNotesForSource(sourceId).asLiveData()

    // Inserts a new note into the database
    fun insertNote(sourceId: Int, title: String, content: String) {
        viewModelScope.launch {
            noteDao.insertNote(Note(sourceId = sourceId, title = title, content = content))
        }
    }

    // Saves project draft/final paper content
    fun saveDraft(projectId: Int, content: String) {

        viewModelScope.launch {

            // Retrieve existing project
            val project = projectDao.getProjectByIdDirect(projectId) ?: return@launch

            // Update stored draft content
            projectDao.updateProject(project.copy(finalPaperContent = content))
        }
    }

    // Updates project title and optional hero image
    fun updateProjectDetails(projectId: Int, newTitle: String, newImageUri: String?) {

        viewModelScope.launch {

            // Retrieve project from database
            val project = projectDao.getProjectByIdDirect(projectId) ?: return@launch

            // Update project fields
            projectDao.updateProject(
                project.copy(
                    title        = newTitle,
                    heroImageUri = newImageUri ?: project.heroImageUri
                )
            )
        }
    }

    // Permanently deletes a project
    fun deleteProject(projectId: Int) {

        viewModelScope.launch {

            // Retrieve project before deletion
            val project = projectDao.getProjectByIdDirect(projectId) ?: return@launch

            // Remove project from database
            projectDao.deleteProject(project)
        }
    }

    // Permanently deletes a note
    fun deleteSource(source: Source) {
        viewModelScope.launch {
            sourceDao.deleteSource(source)
        }
    }

    // Permanently deletes a note
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    // Marks a project as completed and stores completion timestamp
    fun completeProject(projectId: Int) {

        viewModelScope.launch {

            // Retrieve project before update
            val project = projectDao.getProjectByIdDirect(projectId) ?: return@launch

            // Update completion state and timestamp
            projectDao.updateProject(
                project.copy(isCompleted = true, completedAt = System.currentTimeMillis())
            )
        }
    }
}

// Factory class used to construct CurioViewModel instances
class CurioViewModelFactory(
    private val projectDao: ProjectDAO,
    private val sourceDao: SourceDAO,
    private val noteDao: NoteDAO
) : ViewModelProvider.Factory {

    // Creates and returns requested ViewModel instance
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Ensure requested class is CurioViewModel
        if (modelClass.isAssignableFrom(CurioViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return CurioViewModel(projectDao, sourceDao, noteDao) as T
        }

        // Throw error if unknown ViewModel requested
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}