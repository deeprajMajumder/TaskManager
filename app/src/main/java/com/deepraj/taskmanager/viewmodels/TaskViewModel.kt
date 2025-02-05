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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskDatabase: TaskDatabase
) : ViewModel() {
    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Empty)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())

    // Sorting preferences
    private val _isReversed = MutableStateFlow(false)
    val isReversed: StateFlow<Boolean> = _isReversed.asStateFlow()

    private val _showCompletedFirst = MutableStateFlow<Boolean?>(null)
    val showCompletedFirst: StateFlow<Boolean?> = _showCompletedFirst.asStateFlow()

    // Task counts
    val completedCount = _tasks.map { it.count { task -> task.completed } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val toBeCompletedCount = _tasks.map { it.count { task -> !task.completed } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val sortedTasks = combine(_tasks, _isReversed, _showCompletedFirst) { tasks, isReversed, showCompletedFirst ->
        val sorted = when (showCompletedFirst) {
            true -> tasks.sortedByDescending { it.completed }
            false -> tasks.sortedBy { it.completed }
            null -> tasks
        }
        if (isReversed) sorted.reversed() else sorted
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val firebaseAnalytics = Firebase.analytics

    init {
        viewModelScope.launch {
            _uiState.value = TaskUiState.Loading
            try {
                val apiTasks = taskRepository.getTasks()
                apiTasks?.let { tasks ->
                    taskDatabase.taskDao().insertTasks(tasks)
                    _tasks.value = taskDatabase.taskDao().getAllTasks()
                }
                _uiState.value = TaskUiState.Loaded("Tasks Loaded")
                logAnalyticsEvent(Constants.TASK_FETCHED_SUCCESS, true)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Network Error")
                logAnalyticsEvent(Constants.TASK_FETCHED_ERROR, e.message ?: "Unknown Error")
            }
        }
    }

    fun addTask(title: String) {
        _uiState.value = TaskUiState.Loading
        viewModelScope.launch {
            val newTask = Task(title = title, completed = false)
            taskDatabase.taskDao().insertTask(newTask)
            _tasks.value += newTask
            _uiState.value = TaskUiState.Loaded("Task added")
            logAnalyticsEvent(Constants.TASK_ADDED, newTask.id, newTask.title, newTask.completed)
        }
    }

    fun removeTask(task: Task) {
        _uiState.value = TaskUiState.Loading
        viewModelScope.launch {
            _tasks.value = _tasks.value.filter { it.id != task.id }
            _uiState.value = TaskUiState.Loaded("Task removed")
            logAnalyticsEvent(Constants.TASK_REMOVAL, task.id, task.title, task.completed)
        }
    }

    fun updateTask(task: Task) {
        _uiState.value = TaskUiState.Loading
        viewModelScope.launch {
            taskDatabase.taskDao().updateTask(task)
            _tasks.value = _tasks.value.map { if (it.id == task.id) task else it }
            _uiState.value = TaskUiState.Loaded("Task updated")
            logAnalyticsEvent(Constants.TASK_COMPLETION, task.id, task.title, task.completed)
        }
    }

    fun toggleSortOrder() {
        _isReversed.value = !_isReversed.value
    }

    fun setShowCompletedFirst(showCompleted: Boolean) {
        _showCompletedFirst.value = showCompleted
    }

    private fun logAnalyticsEvent(event: String, vararg params: Any) {
        val bundle = Bundle().apply {
            params.forEachIndexed { index, value ->
                putString("param_$index", value.toString())
            }
        }
        firebaseAnalytics.logEvent(event, bundle)
    }

    sealed class TaskUiState {
        data object Loading : TaskUiState()
        data object Empty : TaskUiState()
        data class Loaded(val message: String?) : TaskUiState()
        data class Error(val message: String) : TaskUiState()
    }
}