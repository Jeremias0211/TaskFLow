package com.example.taskflow.model

data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var category: String = "",
    var priority: Int = 1,
    var status: String = "Pendiente",
    var createdAt: Long = System.currentTimeMillis(),
    var isCompleted: Boolean = false
)