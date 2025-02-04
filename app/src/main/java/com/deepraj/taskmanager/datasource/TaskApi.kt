package com.deepraj.taskmanager.datasource

import com.deepraj.taskmanager.database.entity.Task
import retrofit2.http.GET

interface TaskApi {
    @GET("todos")
    suspend fun getTasks(): List<Task>?
}