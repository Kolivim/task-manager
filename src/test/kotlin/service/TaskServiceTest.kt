package service

import dto.TaskCreateRequest
import dto.TaskStatusUpdateRequest
import exception.TaskNotFoundException
import model.Task
import model.TaskStatus
import repository.TaskRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import reactor.test.StepVerifier
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class TaskServiceTest {

    @Mock
    private lateinit var taskRepository: TaskRepository

    @InjectMocks
    private lateinit var taskService: TaskService

    @Test
    fun `createTask should return TaskResponse`() {
        val request = TaskCreateRequest("Test Task", "Description")
        val savedTask = Task(
            id = 1L,
            title = "Test Task",
            description = "Description",
            status = TaskStatus.NEW,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        whenever(taskRepository.save(any())).thenReturn(savedTask)

        val result = taskService.createTask(request)

        StepVerifier.create(result)
            .assertNext { response ->
                assert(response.id == 1L)
                assert(response.title == "Test Task")
                assert(response.description == "Description")
                assert(response.status == TaskStatus.NEW)
            }
            .verifyComplete()
    }

    @Test
    fun `getTaskById should return TaskResponse when exists`() {
        val task = Task(
            id = 1L,
            title = "Test",
            description = null,
            status = TaskStatus.NEW,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        whenever(taskRepository.findById(1L)).thenReturn(task)

        val result = taskService.getTaskById(1L)

        StepVerifier.create(result)
            .expectNextMatches { it.id == 1L }
            .verifyComplete()
    }

    @Test
    fun `getTaskById should fail when task not found`() {
        whenever(taskRepository.findById(99L)).thenReturn(null)

        val result = taskService.getTaskById(99L)

        StepVerifier.create(result)
            .expectError(TaskNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `updateStatus should return updated TaskResponse`() {
        val task = Task(
            id = 1L,
            title = "Test",
            description = null,
            status = TaskStatus.NEW,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val updatedTask = task.copy(status = TaskStatus.DONE, updatedAt = LocalDateTime.now())
        whenever(taskRepository.updateStatus(eq(1L), eq(TaskStatus.DONE))).thenReturn(1)
        whenever(taskRepository.findById(1L)).thenReturn(updatedTask)

        val request = TaskStatusUpdateRequest(TaskStatus.DONE)
        val result = taskService.updateStatus(1L, request)

        StepVerifier.create(result)
            .assertNext { response ->
                assert(response.status == TaskStatus.DONE)
            }
            .verifyComplete()
    }

    @Test
    fun `updateStatus should fail when task not found`() {
        whenever(taskRepository.updateStatus(eq(99L), any())).thenReturn(0)

        val request = TaskStatusUpdateRequest(TaskStatus.DONE)
        val result = taskService.updateStatus(99L, request)

        StepVerifier.create(result)
            .expectError(TaskNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `deleteTask should complete when task exists`() {
        whenever(taskRepository.deleteById(1L)).thenReturn(1)

        val result = taskService.deleteTask(1L)

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `deleteTask should fail when task not found`() {
        whenever(taskRepository.deleteById(99L)).thenReturn(0)

        val result = taskService.deleteTask(99L)

        StepVerifier.create(result)
            .expectError(TaskNotFoundException::class.java)
            .verify()
    }

    @Test
    fun `getTasks should return page response`() {
        val tasks = listOf(
            Task(1L, "Task1", null, TaskStatus.NEW, LocalDateTime.now(), LocalDateTime.now())
        )
        val total = 1L
        whenever(taskRepository.findAll(0, 10, null)).thenReturn(tasks to total)

        val result = taskService.getTasks(0, 10, null)

        StepVerifier.create(result)
            .assertNext { page ->
                assert(page.content.size == 1)
                assert(page.page == 0)
                assert(page.size == 10)
                assert(page.totalElements == 1L)
                assert(page.totalPages == 1)
            }
            .verifyComplete()
    }
}