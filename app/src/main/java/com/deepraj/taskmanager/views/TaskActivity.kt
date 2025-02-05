package com.deepraj.taskmanager.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepraj.taskmanager.database.entity.Task
import com.deepraj.taskmanager.ui.theme.TaskManagerTheme
import com.deepraj.taskmanager.viewmodels.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaskActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskManagerTheme {
                val taskList by viewModel.tasks.collectAsState()
                val showDialog = remember { mutableStateOf(false) }
                val selectedTask = remember { mutableStateOf<Task?>(null) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Task Manager",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
                { innerPadding ->
                    TaskListScreen(
                        viewModel = viewModel,
                        innerPadding = innerPadding,
                        taskList = taskList,
                        showDialog = showDialog,
                        selectedTask = selectedTask
                    )
                }
            }
        }
    }
}


@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    innerPadding: PaddingValues,
    taskList: List<Task>,
    showDialog: MutableState<Boolean>,
    selectedTask: MutableState<Task?>
) {
    val lazyListState = rememberLazyListState()

    val onAddOrUpdateTask: (Task) -> Unit = { task ->
        if (taskList.any { it.id == task.id }) {
            viewModel.toggleTaskCompletion(task)
        } else {
            viewModel.addTask(task.title)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .simpleVerticalScrollbar(lazyListState)
        ) {
            items(taskList) { task ->
                TaskItem(
                    task = task,
                    onClick = {
                        selectedTask.value = task
                        showDialog.value = true
                    }
                )
            }
        }
        FloatingActionButton(
            onClick = {
                selectedTask.value = null
                showDialog.value = true
            },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }

        if (showDialog.value) {
            TaskDialog(
                task = selectedTask.value,
                onDismiss = { showDialog.value = false },
                onAddOrUpdateTask = onAddOrUpdateTask
            )
        }
    }

}


@Composable
fun TaskItem(task: Task, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
            .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = task.title,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 8.dp
): Modifier {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration)
    )

    return drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || alpha > 0.0f

        if (needDrawScrollbar && firstVisibleElementIndex != null) {
            val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
            val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
            val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight

            drawRect(
                color = Color.DarkGray,
                topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
                alpha = alpha
            )
        }
    }
}

@Composable
fun TaskDialog(
    task: Task?,
    onDismiss: () -> Unit,
    onAddOrUpdateTask: (Task) -> Unit
) {
    var taskTitle by remember { mutableStateOf(task?.title ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (task == null) "Add Task" else "Update Task")
        },
        text = {
            Column {
                TextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task Title") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (task == null) {
                        onAddOrUpdateTask(Task(title = taskTitle, completed = false))
                    } else {
                        task.title = taskTitle
                        task.completed = false
                        onAddOrUpdateTask(task)
                    }
                    onDismiss.invoke()
                }
            ) {
                Text(text = if (task == null) "Add Task" else "Update Task")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TaskManagerTheme {

    }
}