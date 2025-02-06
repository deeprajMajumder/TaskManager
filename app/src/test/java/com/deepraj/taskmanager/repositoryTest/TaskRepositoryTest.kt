package com.deepraj.taskmanager.repositoryTest

import com.deepraj.taskmanager.database.entity.Task
import com.deepraj.taskmanager.datasource.TaskApi
import com.deepraj.taskmanager.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
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
class TaskRepositoryTest {

    // Mock dependencies
    private lateinit var taskApi: TaskApi
    private lateinit var repository: TaskRepository

    // Test coroutine dispatcher
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        taskApi = mockk()
        repository = TaskRepository(taskApi)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getTasks should return list of tasks from API`() = runTest {
        // Given
        val mockTasks = listOf(Task(id = 1, title = "Test Task", completed = false))
        coEvery { taskApi.getTasks() } returns mockTasks

        // When
        val result = repository.getTasks()

        // Then
        assertEquals(mockTasks, result)
        coVerify { taskApi.getTasks() }
    }

    @Test
    fun `getTasks should return null when API call fails`() = runTest {
        // Given
        coEvery { taskApi.getTasks() } throws Exception("Network Error")

        // When
        val result = runCatching { repository.getTasks() }.getOrNull()

        // Then
        assertNull(result)
        coVerify { taskApi.getTasks() }
    }
}