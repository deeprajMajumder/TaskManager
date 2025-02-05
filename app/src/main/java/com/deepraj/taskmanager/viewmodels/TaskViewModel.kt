package com.deepraj.taskmanager.viewmodels

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepraj.taskmanager.database.TaskDatabase
import com.deepraj.taskmanager.database.entity.Task
import com.deepraj.taskmanager.repository.TaskRepository
import com.deepraj.taskmanager.utils.Constants
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskDatabase: TaskDatabase
) : ViewModel(){
    private val TAG = TaskViewModel::class.java.simpleName
    private val _isReversed = MutableStateFlow(false)
    val isReversed: StateFlow<Boolean> = _isReversed.asStateFlow()
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = combine(_tasks, _isReversed) { tasks, isReversed ->
        if (isReversed) tasks.reversed() else tasks
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val firebaseAnalytics = Firebase.analytics

    init {
        viewModelScope.launch {
            try {
                val apiTasks = taskRepository.getTasks()
                apiTasks?.let { tasks ->
                    taskDatabase.taskDao().insertTasks(tasks)
                    _tasks.value = taskDatabase.taskDao().getAllTasks()
                }
                val params = Bundle().apply {
                    putBoolean(Constants.TASK_FETCHED_SUCCESS, true)
                }
                firebaseAnalytics.logEvent(Constants.TASK_FETCHED, params)
            } catch (e: Exception) {
                val exceptionMessage = e.message
                Log.e(TAG, "API Error: $exceptionMessage")
                val params = Bundle().apply {
                    putString(Constants.TASK_ERROR, exceptionMessage)
                }
                firebaseAnalytics.logEvent(Constants.TASK_FETCHED_ERROR, params)
            }
        }
    }

    fun addTask(title: String) {
        val newTask = Task(title = title, completed = false)
        viewModelScope.launch {
            taskDatabase.taskDao().insertTask(newTask)
            _tasks.value += newTask
            val params = Bundle().apply {
                putInt(Constants.TASK_ID, newTask.id)
                putString(Constants.TASK_TITLE, newTask.title)
                putBoolean(Constants.TASK_COMPLETED, newTask.completed)
            }
            firebaseAnalytics.logEvent(Constants.TASK_ADDED, params)
        }
    }
    fun removeTask(task: Task) {
        viewModelScope.launch {
            _tasks.value = _tasks.value.filter { it.id != task.id }
            val params = Bundle().apply {
                putInt(Constants.TASK_ID, task.id)
                putString(Constants.TASK_TITLE, task.title)
                putBoolean(Constants.TASK_COMPLETED, task.completed)
            }
            firebaseAnalytics.logEvent(Constants.TASK_REMOVAL, params)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDatabase.taskDao().updateTask(task)
            _tasks.value = _tasks.value.map { if (it.id == task.id) task else it }
            val params = Bundle().apply {
                putInt(Constants.TASK_ID, task.id)
                putString(Constants.TASK_TITLE, task.title)
                putBoolean(Constants.TASK_COMPLETED, task.completed)
            }
            firebaseAnalytics.logEvent(Constants.TASK_COMPLETION, params)
        }
    }
    fun toggleSortOrder() {
        _isReversed.value = !_isReversed.value
    }
}