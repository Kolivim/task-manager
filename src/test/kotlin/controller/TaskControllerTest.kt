import controller.TaskController
import dto.*
import exception.TaskNotFoundException
import model.TaskStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [TaskManagerApplication::class]
)
class TaskControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var taskService: service.TaskService


    @Test
    fun `createTask should return 201 Created with task`() {
        val request = TaskCreateRequest("New Task", "Desc")
        val response = TaskResponse(1L, "New Task", "Desc",
            TaskStatus.NEW, LocalDateTime.now(), LocalDateTime.now())

        whenever(taskService.createTask(any())).thenReturn(Mono.just(response))

        webTestClient.post()
            .uri("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.title").isEqualTo("New Task")
    }


    @Test
    fun `createTask should return 400 when title too short`() {
        val request = mapOf("title" to "ab", "description" to "Desc")

        webTestClient.post()
            .uri("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }


    @Test
    fun `getTaskById should return 200 when task exists`() {
        val response = TaskResponse(1L, "Task", null, TaskStatus.NEW,
            LocalDateTime.now(), LocalDateTime.now())

        whenever(taskService.getTaskById(1L)).thenReturn(Mono.just(response))

        webTestClient.get()
            .uri("/api/tasks/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
    }


    @Test
    fun `getTaskById should return 404 when task not found`() {
        whenever(taskService.getTaskById(99L)).thenReturn(Mono.error(TaskNotFoundException(99L)))

        webTestClient.get()
            .uri("/api/tasks/99")
            .exchange()
            .expectStatus().isNotFound
    }


    @Test
    fun `updateStatus should return 200 with updated task`() {
        val request = TaskStatusUpdateRequest(TaskStatus.DONE)
        val response = TaskResponse(1L, "Task", null, TaskStatus.DONE,
            LocalDateTime.now(), LocalDateTime.now())

        whenever(taskService.updateStatus(eq(1L), any())).thenReturn(Mono.just(response))

        webTestClient.patch()
            .uri("/api/tasks/1/status")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("DONE")
    }


    @Test
    fun `deleteTask should return 204 No Content`() {
        whenever(taskService.deleteTask(1L)).thenReturn(Mono.empty())

        webTestClient.delete()
            .uri("/api/tasks/1")
            .exchange()
            .expectStatus().isNoContent
    }


    @Test
    fun `getTasks should return page response`() {
        val pageResponse = PageResponse(
            content = listOf(
                TaskResponse(1L, "Task", null, TaskStatus.NEW,
                    LocalDateTime.now(), LocalDateTime.now())
            ),
            page = 0,
            size = 10,
            totalElements = 1,
            totalPages = 1
        )

        whenever(taskService.getTasks(0, 10, null)).thenReturn(Mono.just(pageResponse))

        webTestClient.get()
            .uri("/api/tasks?page=0&size=10")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.content[0].id").isEqualTo(1)
            .jsonPath("$.totalElements").isEqualTo(1)
    }


}