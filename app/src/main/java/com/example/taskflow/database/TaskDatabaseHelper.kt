package com.example.taskflow.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.taskflow.model.Task

class TaskDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "tasks.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT,
                category TEXT,
                priority INTEGER,
                completed INTEGER
            )
        """.trimIndent()

        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS tasks")
        onCreate(db)
    }

    fun insertTask(task: Task) {
        val db = writableDatabase

        val values = ContentValues().apply {
            put("title", task.title)
            put("category", task.category)
            put("priority", task.priority)
            put("completed", if (task.isCompleted) 1 else 0)
        }

        db.insert("tasks", null, values)
        db.close()
    }

    fun getAllTasks(): List<Task> {
        val taskList = mutableListOf<Task>()
        val db = readableDatabase

        val cursor = db.rawQuery("SELECT * FROM tasks ORDER BY priority DESC", null)

        if (cursor.moveToFirst()) {
            do {
                val task = Task(
                    id = cursor.getInt(0),
                    title = cursor.getString(1),
                    category = cursor.getString(2),
                    priority = cursor.getInt(3),
                    isCompleted = cursor.getInt(4) == 1
                )
                taskList.add(task)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return taskList
    }
}