package com.chronostudy.app.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.data.local.entity.ChapterEntity
import com.chronostudy.app.data.local.entity.PlannerTaskEntity
import com.chronostudy.app.data.local.entity.SubjectEntity
import com.chronostudy.app.data.repository.GoalRepository
import com.chronostudy.app.data.repository.LibraryRepository
import com.chronostudy.app.data.repository.PlannerRepository
import com.chronostudy.app.data.repository.SessionRepository
import com.chronostudy.app.domain.model.TaskStatus
import com.chronostudy.app.domain.stats.StatsCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class PlannerUiState(
    val epochDay: Long = LocalDate.now().toEpochDay(),
    val tasks: List<PlannerTaskEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val chapters: List<ChapterEntity> = emptyList(),
    val goalMinutes: Int = 0,
    val plannedMinutes: Int = 0,
    val actualMillis: Long = 0L,
    val nextSubjectNameById: Map<Long, String> = emptyMap(),
    val chapterNameById: Map<Long, String> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(
    private val library: LibraryRepository,
    private val planner: PlannerRepository,
    private val goals: GoalRepository,
    sessions: SessionRepository,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val selectedDay = MutableStateFlow(LocalDate.now(zone).toEpochDay())
    val day: StateFlow<Long> = selectedDay

    val state: StateFlow<PlannerUiState> = combine(
        selectedDay.flatMapLatest { planner.observeByDay(it) },
        library.allSubjects,
        library.chapters,
        selectedDay,
        sessions.completedSessions,
    ) { tasks, subjects, chapters, day, completed ->
        val dayDate = LocalDate.ofEpochDay(day)
        PlannerUiState(
            epochDay = day,
            tasks = tasks,
            subjects = subjects.filter { !it.isArchived },
            chapters = chapters.filter { !it.isArchived },
            goalMinutes = goals.getByDay(day)?.targetMinutes ?: 0,
            plannedMinutes = tasks.sumOf { it.plannedDurationMinutes },
            actualMillis = StatsCalculator.focusOn(completed, dayDate, zone),
            nextSubjectNameById = subjects.associate { it.id to it.name },
            chapterNameById = chapters.associate { it.id to it.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannerUiState())

    fun selectDay(epochDay: Long) {
        selectedDay.value = epochDay
    }

    fun previousDay() {
        selectedDay.value -= 1
    }

    fun nextDay() {
        selectedDay.value += 1
    }

    fun jumpToToday() {
        selectedDay.value = LocalDate.now(zone).toEpochDay()
    }

    fun addTask(subjectId: Long, chapterId: Long?, startMinute: Int?, endMinute: Int?, minutes: Int, note: String) {
        viewModelScope.launch {
            planner.addTask(
                subjectId = subjectId,
                chapterId = chapterId,
                plannedEpochDay = selectedDay.value,
                startMinuteOfDay = startMinute,
                endMinuteOfDay = endMinute,
                plannedDurationMinutes = minutes,
                note = note,
            )
        }
    }

    fun updateTask(task: PlannerTaskEntity) = viewModelScope.launch { planner.updateTask(task) }

    fun deleteTask(task: PlannerTaskEntity) = viewModelScope.launch { planner.deleteTask(task) }

    fun setStatus(id: Long, status: TaskStatus) = viewModelScope.launch { planner.setStatus(id, status) }

    fun moveToDay(id: Long, epochDay: Long) = viewModelScope.launch { planner.moveToDay(id, epochDay) }

    fun setGoal(minutes: Int) = viewModelScope.launch { goals.setTargetMinutes(selectedDay.value, minutes) }

    private val _planMessage = MutableStateFlow<String?>(null)
    val planMessage: StateFlow<String?> = _planMessage

    fun clearPlanMessage() {
        _planMessage.value = null
    }

    fun loadStarterPlan() {
        viewModelScope.launch {
            val subjects = library.allSubjects.first().filter { !it.isArchived }
            fun idFor(fragment: String): Long? =
                subjects.firstOrNull { it.name.contains(fragment, ignoreCase = true) }?.id

            val template: Map<java.time.DayOfWeek, List<Pair<String, Int>>> = mapOf(
                java.time.DayOfWeek.MONDAY to listOf("Programming Using C" to 50, "Operating System" to 50),
                java.time.DayOfWeek.TUESDAY to listOf("Database" to 50, "Networking" to 50),
                java.time.DayOfWeek.WEDNESDAY to listOf("Internet" to 50, "Programming Using C" to 50),
                java.time.DayOfWeek.THURSDAY to listOf("Operating System" to 50, "Database" to 50),
                java.time.DayOfWeek.FRIDAY to listOf("Networking" to 50, "Internet" to 50),
                java.time.DayOfWeek.SATURDAY to listOf("Revision" to 60),
                java.time.DayOfWeek.SUNDAY to listOf("Weekly review" to 60),
            )

            val today = LocalDate.now(zone)
            val monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            var created = 0

            for (dow in java.time.DayOfWeek.entries) {
                val date = monday.plusDays(dow.value - 1L)
                val dayTasks = planner.getByDay(date.toEpochDay())
                for ((key, minutes) in template[dow].orEmpty()) {
                    val subjectId = idFor(key)
                    if (subjectId != null) {
                        if (dayTasks.any { it.subjectId == subjectId }) continue
                        planner.addTask(
                            subjectId = subjectId,
                            chapterId = null,
                            plannedEpochDay = date.toEpochDay(),
                            plannedDurationMinutes = minutes,
                            note = key,
                        )
                        created++
                    } else {
                        if (dayTasks.any { it.note == key }) continue
                        val fallbackSubject = subjects.firstOrNull() ?: continue
                        planner.addTask(
                            subjectId = fallbackSubject.id,
                            chapterId = null,
                            plannedEpochDay = date.toEpochDay(),
                            plannedDurationMinutes = minutes,
                            note = key,
                        )
                        created++
                    }
                }
            }

            if ((goals.getByDay(today.toEpochDay())?.targetMinutes ?: 0) <= 0) {
                goals.setTargetMinutes(today.toEpochDay(), 120)
            }

            _planMessage.value = if (created > 0) {
                "Starter plan added: $created tasks for this week"
            } else {
                "Starter plan already present for this week"
            }
        }
    }
}
