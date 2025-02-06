package com.deepraj.taskmanager.viewmodelsTest

import android.database.sqlite.SQLiteConstraintException
import com.deepraj.taskmanager.database.TaskDao
import com.deepraj.taskmanager.database.TaskDatabase
import com.deepraj.taskmanager.database.entity.Task
import com.deepraj.taskmanager.repository.TaskRepository
import com.deepraj.taskmanager.viewmodels.TaskViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class TaskViewModelTest {

    // Mocks
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskDatabase: TaskDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var viewModel: TaskViewModel

    // Coroutines Test Dispatcher
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // Initialize MockK
        taskRepository = mockk()
        taskDatabase = mockk()
        taskDao = mockk()

        // Mock database behavior
        every { taskDatabase.taskDao() } returns taskDao

        // Initialize ViewModel with Test Coroutine Dispatcher
        Dispatchers.setMain(testDispatcher)
        viewModel = TaskViewModel(taskRepository, taskDatabase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when ViewModel is initialized, it should fetch tasks from API and update UI state`() =
        runTest {
            // Given
            val mockTasks = listOf(Task(id = 1, title = "Test Task", completed = false))
            coEvery { taskRepository.getTasks() } returns mockTasks
            coEvery { taskDao.insertTasks(mockTasks) } just Runs
            coEvery { taskDao.getAllTasks() } returns mockTasks

            // When
            viewModel = TaskViewModel(taskRepository, taskDatabase)

            // Advance the dispatcher
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertEquals(TaskViewModel.TaskUiState.Loaded("Tasks Loaded"), viewModel.uiState.value)
            assertEquals(mockTasks, viewModel.sortedTasks.value)
        }

    @Test
    fun `addTask should insert a new task and update UI state`() = runTest {
        // Given
        val taskTitle = "New Task"
        val mockTask = Task(id = 1, title = taskTitle, completed = false)

        coEvery { taskDao.insertTask(any()) } returns 1L
        coEvery { taskDao.getAllTasks() } returns listOf(mockTask)

        // When
        viewModel.addTask(taskTitle)

        // Advance the dispatcher
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assert(viewModel.sortedTasks.value.contains(mockTask))
        assertEquals(TaskViewModel.TaskUiState.Loaded("Task added"), viewModel.uiState.value)
    }

    @Test
    fun `removeTask should remove a task and update UI state`() = runTest {
        // Given
        val task = Task(id = 1, title = "Task to Remove", completed = false)
        viewModel = TaskViewModel(taskRepository, taskDatabase)
        viewModel.addTask(task.title)

        // When
        viewModel.removeTask(task)

        // Advance the dispatcher
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.sortedTasks.value.contains(task))
        assertEquals(TaskViewModel.TaskUiState.Loaded("Task removed"), viewModel.uiState.value)
    }

    @Test
    fun `updateTask should update a task and update UI state`() = runTest {
        // Given
        val task = Task(id = 1, title = "Initial Task", completed = false)
        val updatedTask = task.copy(completed = true)

        coEvery { taskDao.updateTask(updatedTask) } just Runs
        coEvery { taskDao.getAllTasks() } returns listOf(updatedTask)

        // When
        viewModel.updateTask(updatedTask)

        // Advance the dispatcher
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.sortedTasks.value.contains(updatedTask))
        assertEquals(TaskViewModel.TaskUiState.Loaded("Task updated"), viewModel.uiState.value)
    }

    @Test
    fun `toggleSortOrder should reverse task order`() = runTest {
        // Given
        val taskList = listOf(
            Task(id = 1, title = "Task 1", completed = false),
            Task(id = 2, title = "Task 2", completed = true)
        )
        viewModel = TaskViewModel(taskRepository, taskDatabase)
        viewModel.showAllTasks()

        // When
        viewModel.toggleSortOrder()

        // Then
        val sortedTasks = viewModel.sortedTasks.value
        assertEquals(taskList.reversed(), sortedTasks)
    }

    @Test
    fun `setShowCompletedFirst should filter tasks by completion status`() = runTest {
        // Given
        val completedTask = Task(id = 1, title = "Completed Task", completed = true)
        val incompleteTask = Task(id = 2, title = "Incomplete Task", completed = false)

        coEvery { taskDao.getAllTasks() } returns listOf(completedTask, incompleteTask)

        viewModel = TaskViewModel(taskRepository, taskDatabase)

        // When
        viewModel.setShowCompletedFirst(true)

        // Then
        assertTrue(viewModel.sortedTasks.value.contains(completedTask))
        assertFalse(viewModel.sortedTasks.value.contains(incompleteTask))
    }

    @Test
    fun `exceptionWhileInsertWithoutCrash should log exception without crashing`() = runTest {
        // Given
        val task = Task(id = 1, title = "Crash Task", completed = false)
        coEvery { taskDao.insertTask(task) } throws SQLiteConstraintException("Duplicate ID")

        // When
        viewModel.exceptionWhileInsertWithoutCrash()

        // Advance the dispatcher
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify { FirebaseCrashlytics.getInstance().recordException(any()) }
    }

    @Test(expected = SQLiteConstraintException::class)
    fun `crashWhileInsert should cause an exception`(): Unit = runTest {
        // Given
        val task = Task(id = 1, title = "Crash Task", completed = false)
        coEvery { taskDao.insertTask(task) } returns 1L andThenThrows SQLiteConstraintException("Duplicate ID")

        // When
        viewModel.crashWhileInsert()

        // Advance the dispatcher
        testDispatcher.scheduler.advanceUntilIdle()
    }
}