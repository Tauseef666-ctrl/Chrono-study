package com.chronostudy.app.data.local

import androidx.room.TypeConverter
import com.chronostudy.app.domain.model.ChapterProgress
import com.chronostudy.app.domain.model.EntryType
import com.chronostudy.app.domain.model.SessionState
import com.chronostudy.app.domain.model.TaskStatus

class Converters {
    @TypeConverter fun sessionStateToString(value: SessionState): String = value.name
    @TypeConverter fun stringToSessionState(value: String): SessionState =
        runCatching { SessionState.valueOf(value) }.getOrDefault(SessionState.IDLE)

    @TypeConverter fun chapterProgressToString(value: ChapterProgress): String = value.name
    @TypeConverter fun stringToChapterProgress(value: String): ChapterProgress =
        runCatching { ChapterProgress.valueOf(value) }.getOrDefault(ChapterProgress.NOT_STARTED)

    @TypeConverter fun taskStatusToString(value: TaskStatus): String = value.name
    @TypeConverter fun stringToTaskStatus(value: String): TaskStatus =
        runCatching { TaskStatus.valueOf(value) }.getOrDefault(TaskStatus.PLANNED)

    @TypeConverter fun entryTypeToString(value: EntryType): String = value.name
    @TypeConverter fun stringToEntryType(value: String): EntryType =
        runCatching { EntryType.valueOf(value) }.getOrDefault(EntryType.NORMAL)
}
