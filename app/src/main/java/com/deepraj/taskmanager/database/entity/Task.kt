package com.deepraj.taskmanager.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("title")
    var title: String,
    @SerializedName("completed")
    var completed: Boolean
)
