package repository

import model.Task
import model.TaskStatus

interface TaskRepository {

    fun save(task: Task): Task

    fun findById(id: Long): Task?

    fun findAll(page: Int, size: Int, status: TaskStatus?): Pair<List<Task>, Long>

    fun updateStatus(id: Long, status: TaskStatus): Int

    fun deleteById(id: Long): Int

}