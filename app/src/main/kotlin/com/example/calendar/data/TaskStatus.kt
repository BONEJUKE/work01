package com.example.calendar.data

enum class TaskStatus {
    Pending,
    InProgress,
    Completed;

    fun isDone(): Boolean = this == Completed
}
