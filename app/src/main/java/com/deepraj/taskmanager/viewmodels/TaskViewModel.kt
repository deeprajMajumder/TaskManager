package com.deepraj.taskmanager.viewmodels

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    var selectedTab by mutableStateOf<String?>("All")
        private set

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())

    private val _filteredTasks = MutableStateFlow<List<Task>>(emptyList())

    // Sorting preferences
    private val _isReversed = MutableStateFlow(false)
    val isReversed: StateFlow<Boolean> = _isReversed.asStateFlow()

    private val _showCompletedFirst = MutableStateFlow<Boolean?>(null)

    // Task counts
    val allCount = _tasks.map { it.count() }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val completedCount = _tasks.map { it.count { task -> task.completed } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val incompleteCount = _tasks.map { it.count { task -> !task.completed } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // Sorted filtered tasks
    val sortedTasks = combine(_filteredTasks, _isReversed) { tasks, isReversed ->
        if (isReversed) tasks.reversed() else tasks
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
                    _filteredTasks.value = _tasks.value // Initially no filtering, show all tasks
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
            filterTasksByCompletionStatus()
            _uiState.value = TaskUiState.Loaded("Task added")
            logAnalyticsEvent(Constants.TASK_ADDED, newTask.id, newTask.title, newTask.completed)
        }
    }

    fun removeTask(task: Task) {
        _uiState.value = TaskUiState.Loading
        viewModelScope.launch {
            _tasks.value = _tasks.value.filter { it.id != task.id }
            filterTasksByCompletionStatus()
            _uiState.value = TaskUiState.Loaded("Task removed")
            logAnalyticsEvent(Constants.TASK_REMOVAL, task.id, task.title, task.completed)
        }
    }

    fun updateTask(task: Task) {
        _uiState.value = TaskUiState.Loading
        viewModelScope.launch {
            taskDatabase.taskDao().updateTask(task)
            _tasks.value = _tasks.value.map { if (it.id == task.id) task else it }
            filterTasksByCompletionStatus()
            _uiState.value = TaskUiState.Loaded("Task updated")
            logAnalyticsEvent(Constants.TASK_COMPLETION, task.id, task.title, task.completed)
        }
    }

    fun toggleSortOrder() {
        _isReversed.value = !_isReversed.value
    }

    fun setShowCompletedFirst(showCompleted: Boolean) {
        selectedTab = if (showCompleted) "Completed" else "Incomplete"
        _showCompletedFirst.value = showCompleted
        filterTasksByCompletionStatus()
    }

    fun showAllTasks() {
        selectedTab = "All"
        _showCompletedFirst.value = null
        filterTasksByCompletionStatus()
    }

    private fun filterTasksByCompletionStatus() {
        _filteredTasks.value = when (selectedTab) {
            "Completed" -> _tasks.value.filter { it.completed }
            "Incomplete" -> _tasks.value.filter { !it.completed }
            else -> _tasks.value
        }
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