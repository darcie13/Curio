# Curio: Your Personal Research Lab 

**Curio** is a comprehensive research ecosystem designed for lifelong learners to organize digital media, annotate complex sources, and synthesize findings into formal research papers. Built for Android, it moves beyond simple bookmarking by providing a focused, distraction-free environment for deep intellectual exploration.

---

## 🚀 The Research Lifecycle
Curio's architecture follows the natural progression of a research project, powered by the Android Navigation Component:
1. **Home Library:** The central dashboard for managing active research projects. Users can create new projects with custom hero images or search through existing ones.
2. **Project Workspace:** A dedicated hub for each project that automatically categorizes collected sources into Videos, Articles, Images, and Papers.
3. **Annotation Lab:** A split-context interface where users can view sources via an integrated WebView while simultaneously capturing timestamped notes.
4. **Writing Lab:** A focused environment for drafting the final synthesis. Once completed, papers are "published" to move the project from active status to the permanent archive.
5. **Library Archive:** A read-only gallery of completed research, preserving findings in a clean, academic format.


---

## ✨ Core Features
- **Universal Media Capture:** Intercepts Android "Share" intents, allowing users to save links from any browser or media app directly into a specific Curio project.
- **Relational Data Management:** Powered by a Room SQLite Database, the app maintains strict relationships between projects, sources, and annotations.
- **Intuitive Gestures:** Utilizes modern Swipe-to-Delete interactions for efficient library maintenance.
- **Intelligent UI:** A full Material 3 implementation featuring "Academic-Chic" styling with a Forest Green and Cream White color palette.
- **Cascade Integrity:** Advanced database logic ensures that deleting a project or source automatically cleans up all associated data.

---

## 🛠 Technical Stack
- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel) with Activity-scoped ViewModels for shared state.
- **Database:** Room Persistence Library
- **Concurrency:** Kotlin Coroutines and Flow for real-time UI updates.
- **UI/UX:** Material 3, View Binding, and Motion Layout.
- **Image Loading:** Coil for asynchronous hero image and thumbnail rendering.

---

## 📝 Setup & Installation
- **Minimum SDK:** Android 10 (API 29)
- **Permissions:** Requires INTERNET for web viewing and READ_MEDIA_IMAGES (on Android 13+) for project customization.
- **Deployment:** Open the project in Android Studio, sync Gradle, and deploy to a physical device or emulator.

---

## 👤 Credits
- **Developer:** Darcie Raymond
- **Version:** Final Release (May 2026)

---