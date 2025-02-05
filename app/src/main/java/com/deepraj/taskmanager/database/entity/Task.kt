package com.deepraj.taskmanager.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @SerializedName("title")
    @ColumnInfo(name = "title")
    var title: String,

    @SerializedName("completed")
    @ColumnInfo(name = "completed")
    var completed: Boolean
)
