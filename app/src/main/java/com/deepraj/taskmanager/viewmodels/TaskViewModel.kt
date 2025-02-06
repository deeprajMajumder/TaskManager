package com.deepraj.taskmanager.viewmodels

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepraj.taskmanager.database.TaskDatabase
import com.deepraj.taskmanager.database.entity.Task
import com.deepraj.taskmanager.di.modules.CoroutineDispatcherProvider
import com.deepraj.taskmanager.repository.TaskRepository
import com.deepraj.taskmanager.utils.Constants
import com.deepraj.taskmanager.utils.FirebaseAnalyticsUtil
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
    private val taskDatabase: TaskDatabase,
    private val coroutineDispatcherProvider: CoroutineDispatcherProvider
) : ViewModel() {
    private val TAG = TaskViewModel::class.java.simpleName
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

    init {
        viewModelScope.launch(coroutineDispatcherProvider.IO()) {
            _uiState.value = TaskUiState.Loading
            try {
                val apiTasks = taskRepository.getTasks()
                Log.d(TAG, "API Tasks: ${apiTasks?.stream()}")
                apiTasks?.let { tasks ->
                    taskDatabase.taskDao().insertTasks(tasks)
                    _tasks.value = taskDatabase.taskDao().getAllTasks()
                    _filteredTasks.value = _tasks.value // Initially no filtering, show all tasks
                }
                _uiState.value = TaskUiState.Loaded("")
                FirebaseAnalyticsUtil.logAnalyticsEvent(Constants.TASK_FETCHED_SUCCESS, true)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Network Error")
                FirebaseAnalyticsUtil.logAnalyticsEvent(
                    Constants.TASK_FETCHED_ERROR,
                    e.message ?: "Unknown Error"
                )
            }
        }
    }

    fun addTask(title: String) {
        _uiState.value = TaskUiState.Loading
        viewModelScope.launch(coroutineDispatcherProvider.IO()) {
            try {
                val newTask = Task(title = title, completed = false)
                val taskId = taskDatabase.taskDao().insertTask(newTask)
                val insertedTask = newTask.copy(id = taskId.toInt())
                _tasks.value += insertedTask
                filterTasksByCompletionStatus()
                _uiState.value = TaskUiState.Loaded("Task added")
                FirebaseAnalyticsUtil.logAnalyticsEvent(
                    Constants.TASK_ADDED,
                    insertedTask.id,
                    insertedTask.title,
                    insertedTask.completed
                )
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Task added error")
                FirebaseAnalyticsUtil.logAnalyticsEvent(Constants.TASK_ERROR, e.message.toString())
            }
        }
    }

    fun removeTask(task: Task) {
        _uiState.value = TaskUiState.Loading
        viewModelScope.launch(coroutineDispatcherProvider.IO()) {
            try {
                _tasks.value = _tasks.value.filter { it.id != task.id }
                filterTasksByCompletionStatus()
                _uiState.value = TaskUiState.Loaded("Task removed")
                FirebaseAnalyticsUtil.logAnalyticsEvent(
                    Constants.TASK_REMOVAL,
                    task.id,
                    task.title,
                    task.completed
                )
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Task removal error")
                FirebaseAnalyticsUtil.logAnalyticsEvent(Constants.TASK_ERROR, e.message.toString())
            }
        }
    }

    fun crashWhileInsert() {
        viewModelScope.launch(coroutineDispatcherProvider.IO()) {
            val task = Task(id = 1, title = "Crash Test", completed = false)
            taskDatabase.taskDao().insertTask(task)
            taskDatabase.taskDao()
                .insertTask(task)  // Second insert with the same ID (causes crash)
            Log.e(TAG, "Crash While Insert")
        }
    }

    fun exceptionWhileInsertWithoutCrash() {
        viewModelScope.launch(coroutineDispatcherProvider.IO()) {
            try {
                val task = Task(id = 1, title = "Crash Test", completed = false)
                taskDatabase.taskDao().insertTask(task)
                taskDatabase.taskDao()
                    .insertTask(task)  // Second insert with the same ID (causes crash)
            } catch (e: SQLiteConstraintException) {
                Log.e(TAG, "Crash While Insert ", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    fun updateTask(task: Task) {
        _uiState.value = TaskUiState.Loading
        viewModelScope.launch(coroutineDispatcherProvider.IO()) {
            try {
                val existingTask =
                    taskDatabase.taskDao().getTaskById(task.id) // Fetch the existing task from DB

                if (existingTask == null) {
                    _uiState.value = TaskUiState.Error("Task not found")
                    return@launch
                }

                if (existingTask.title != task.title || existingTask.completed != task.completed) {
                    taskDatabase.taskDao().updateTask(task)
                    _tasks.value = _tasks.value.map { if (it.id == task.id) task else it }
                    filterTasksByCompletionStatus()
                    _uiState.value = TaskUiState.Loaded("Task updated")

                    val eventType =
                        if (task.completed) Constants.TASK_COMPLETED else Constants.TASK_EDITED
                    FirebaseAnalyticsUtil.logAnalyticsEvent(
                        eventType,
                        task.id,
                        task.title,
                        task.completed
                    )
                } else {
                    _uiState.value = TaskUiState.Loaded("No changes detected")
                }
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error("Task update error")
                FirebaseAnalyticsUtil.logAnalyticsEvent(Constants.TASK_ERROR, e.message.toString())
            }
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

    sealed class TaskUiState {
        data object Loading : TaskUiState()
        data object Empty : TaskUiState()
        data class Loaded(val message: String?) : TaskUiState()
        data class Error(val message: String) : TaskUiState()
    }
}