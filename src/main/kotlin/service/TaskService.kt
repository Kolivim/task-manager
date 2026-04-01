package service

import dto.*
import exception.TaskNotFoundException
import model.*
import org.slf4j.LoggerFactory
import repository.TaskRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class TaskService(private val taskRepository: TaskRepository) {

    private val logger = LoggerFactory.getLogger(javaClass)


    fun createTask(request: TaskCreateRequest): Mono<TaskResponse> {
        logger.debug("startMethod, creating task with request: {}", request)
        return Mono.fromCallable {
            val task = Task(
                title = request.title,
                description = request.description,
                status = TaskStatus.NEW,
                createdAt = java.time.LocalDateTime.now(),
                updatedAt = java.time.LocalDateTime.now()
            )
            taskRepository.save(task)
        }.subscribeOn(Schedulers.boundedElastic())
            .map { toResponse(it) }
            .doOnSuccess { logger.info("endMethod, task created with id: {}", it.id) }
            .doOnError { e -> logger.error("endMethod, failed to create task", e) }
    }


    fun getTaskById(id: Long): Mono<TaskResponse> {
        logger.debug("startMethod, fetching task by id: {}", id)
        return Mono.fromCallable {
            taskRepository.findById(id) ?: throw TaskNotFoundException(id)
        }.subscribeOn(Schedulers.boundedElastic())
            .map { toResponse(it) }
            .doOnError { e ->
                if (e is TaskNotFoundException) logger.warn("endMethod, task not found: {}", id)
                else logger.error("endMethod, error fetching task {}", id, e)
            }
    }


    fun getTasks(page: Int, size: Int, status: TaskStatus?): Mono<PageResponse<TaskResponse>> {
        logger.debug("startMethod, fetching tasks: page = {}, size = {}, status = {}", page, size, status)
        return Mono.fromCallable {
            val (tasks, total) = taskRepository.findAll(page, size, status)
            val content = tasks.map { toResponse(it) }
            val totalPages = if (size > 0) ((total + size - 1) / size).toInt() else 0
            PageResponse(
                content = content,
                page = page,
                size = size,
                totalElements = total,
                totalPages = totalPages
            )
        }.subscribeOn(Schedulers.boundedElastic())
            .doOnError { e -> logger.error("endMethod, error fetching tasks", e) }
    }


    fun updateStatus(id: Long, request: TaskStatusUpdateRequest): Mono<TaskResponse> {
        logger.debug("startMethod, updating task {} status to {}", id, request.status)
        return Mono.fromCallable {
            val updatedRows = taskRepository.updateStatus(id, request.status)
            if (updatedRows == 0) throw TaskNotFoundException(id)
            taskRepository.findById(id) ?: throw TaskNotFoundException(id)
        }.subscribeOn(Schedulers.boundedElastic())
            .map { toResponse(it) }
            .doOnSuccess { logger.info("endMethod, task {} status updated to {}", id, it.status) }
            .doOnError { e ->
                if (e is TaskNotFoundException) logger.warn("endMethod, task not found for status update: {}", id)
                else logger.error("endMethod, error updating status for task {}", id, e)
            }
    }


    fun deleteTask(id: Long): Mono<Void> {
        logger.debug("startMethod, deleting task with id = {}", id)
        return Mono.fromCallable {
            val deletedRows = taskRepository.deleteById(id)
            if (deletedRows == 0) throw TaskNotFoundException(id)
        }.subscribeOn(Schedulers.boundedElastic()).then()
            .doOnSuccess { logger.info("endMethod, task deleted: {}", id) }
            .doOnError { e ->
                if (e is TaskNotFoundException) logger.warn("endMethod, task not found for deletion: {}", id)
                else logger.error("endMethod, error deleting task with id = {}", id, e)
            }
    }


    private fun toResponse(task: Task): TaskResponse = TaskResponse(
        id = task.id!!,
        title = task.title,
        description = task.description,
        status = task.status,
        createdAt = task.createdAt,
        updatedAt = task.updatedAt
    )


}