package com.deepraj.taskmanager.repository

import com.deepraj.taskmanager.database.entity.Task
import com.deepraj.taskmanager.datasource.TaskApi
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskApi: TaskApi
)  {

    suspend fun getTasks(): List<Task>? {
        return taskApi.getTasks()
    }

}