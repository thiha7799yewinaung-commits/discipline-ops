package com.disciplinequest.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun taskTypeToString(v: TaskType?) = v?.name
    @TypeConverter fun stringToTaskType(v: String?) = v?.let { runCatching { TaskType.valueOf(it) }.getOrDefault(TaskType.ONE_TIME) }

    @TypeConverter fun difficultyToString(v: Difficulty?) = v?.name
    @TypeConverter fun stringToDifficulty(v: String?) = v?.let { runCatching { Difficulty.valueOf(it) }.getOrDefault(Difficulty.MEDIUM) }

    @TypeConverter fun statusToString(v: TaskStatus?) = v?.name
    @TypeConverter fun stringToStatus(v: String?) = v?.let { runCatching { TaskStatus.valueOf(it) }.getOrDefault(TaskStatus.PENDING) }
}
