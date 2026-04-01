package controller

import dto.*
import model.TaskStatus
import service.TaskService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/tasks")
class TaskController(private val taskService: TaskService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTask(@Valid @RequestBody request: TaskCreateRequest): Mono<TaskResponse> =
        taskService.createTask(request)

    @GetMapping("/{id}")
    fun getTaskById(@PathVariable id: Long): Mono<TaskResponse> =
        taskService.getTaskById(id)

    @GetMapping
    fun getTasks(
        @RequestParam page: Int,
        @RequestParam size: Int,
        @RequestParam(required = false) status: TaskStatus?
    ): Mono<PageResponse<TaskResponse>> =
        taskService.getTasks(page, size, status)

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: TaskStatusUpdateRequest
    ): Mono<TaskResponse> =
        taskService.updateStatus(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTask(@PathVariable id: Long): Mono<Void> =
        taskService.deleteTask(id)
}