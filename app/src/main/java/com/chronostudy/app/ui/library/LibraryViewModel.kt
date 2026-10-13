package com.chronostudy.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.data.local.entity.ChapterEntity
import com.chronostudy.app.data.local.entity.CourseEntity
import com.chronostudy.app.data.local.entity.SubjectEntity
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.domain.model.ChapterProgress
import com.chronostudy.app.domain.model.EntryType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val library: LibraryRepository,
) : ViewModel() {

    val courses: StateFlow<List<CourseEntity>> =
        library.allCourses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subjects: StateFlow<List<SubjectEntity>> =
        library.allSubjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val chapters: StateFlow<List<ChapterEntity>> =
        library.chapters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCourse(name: String, branch: String, semester: Int?, entryType: EntryType) {
        if (name.isBlank()) return
        viewModelScope.launch { library.addCourse(name = name, branch = branch, semester = semester, entryType = entryType) }
    }

    fun updateCourse(course: CourseEntity) = viewModelScope.launch { library.updateCourse(course) }

    fun deleteCourse(id: Long) = viewModelScope.launch { library.deleteCourse(id) }

    fun addSubject(
        name: String,
        courseId: Long?,
        semester: Int?,
        colorHex: String,
        targetMinutesPerWeek: Int?,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            library.addSubject(
                name = name,
                courseId = courseId,
                semester = semester,
                colorHex = colorHex,
                targetMinutesPerWeek = targetMinutesPerWeek,
            )
        }
    }

    fun updateSubject(subject: SubjectEntity) = viewModelScope.launch { library.updateSubject(subject) }

    fun deleteSubject(id: Long) = viewModelScope.launch { library.deleteSubject(id) }

    fun addChapter(subjectId: Long, name: String, unitLabel: String) {
        if (name.isBlank()) return
        viewModelScope.launch { library.addChapter(subjectId = subjectId, name = name, unitLabel = unitLabel) }
    }

    fun updateChapter(chapter: ChapterEntity) = viewModelScope.launch { library.updateChapter(chapter) }

    fun deleteChapter(id: Long) = viewModelScope.launch { library.deleteChapter(id) }

    fun setChapterProgress(id: Long, progress: ChapterProgress) =
        viewModelScope.launch { library.setChapterProgress(id, progress) }
}
