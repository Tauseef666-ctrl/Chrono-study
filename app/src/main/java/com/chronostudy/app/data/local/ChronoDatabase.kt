package com.chronostudy.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chronostudy.app.data.local.dao.ChapterDao
import com.chronostudy.app.data.local.dao.CourseDao
import com.chronostudy.app.data.local.dao.DailyGoalDao
import com.chronostudy.app.data.local.dao.PlannerTaskDao
import com.chronostudy.app.data.local.dao.StudySessionDao
import com.chronostudy.app.data.local.dao.SubjectDao
import com.chronostudy.app.data.local.entity.ChapterEntity
import com.chronostudy.app.data.local.entity.CourseEntity
import com.chronostudy.app.data.local.entity.DailyGoalEntity
import com.chronostudy.app.data.local.entity.PlannerTaskEntity
import com.chronostudy.app.data.local.entity.StudySessionEntity
import com.chronostudy.app.data.local.entity.SubjectEntity

@Database(
    entities = [
        CourseEntity::class,
        SubjectEntity::class,
        ChapterEntity::class,
        StudySessionEntity::class,
        PlannerTaskEntity::class,
        DailyGoalEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ChronoDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun plannerTaskDao(): PlannerTaskDao
    abstract fun dailyGoalDao(): DailyGoalDao

    companion object {
        const val NAME = "chrono.db"

        fun build(context: Context): ChronoDatabase =
            Room.databaseBuilder(context, ChronoDatabase::class.java, NAME)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
