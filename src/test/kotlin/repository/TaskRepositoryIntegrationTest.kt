package repository

import model.Task
import model.TaskStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDateTime

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [TaskRepositoryImpl::class])
class TaskRepositoryIntegrationTest {

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    private lateinit var taskRepository: TaskRepository


    @BeforeEach
    fun setUp() {
        taskRepository = TaskRepositoryImpl(jdbcClient)
        jdbcClient.sql("DELETE FROM tasks").update()
    }


    @Test
    fun `save should insert task and return generated id`() {
        val task = Task(
            title = "Integration Test",
            description = "Description",
            status = TaskStatus.NEW,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val saved = taskRepository.save(task)

        assertThat(saved.id).isNotNull
        assertThat(saved.title).isEqualTo("Integration Test")
        assertThat(saved.description).isEqualTo("Description")
        assertThat(saved.status).isEqualTo(TaskStatus.NEW)

        val found = taskRepository.findById(saved.id!!)
        assertThat(found).isNotNull
        assertThat(found!!.title).isEqualTo("Integration Test")
    }


    @Test
    fun `findById should return task when exists`() {
        val task = Task(
            title = "Find Test",
            description = null,
            status = TaskStatus.NEW,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val saved = taskRepository.save(task)

        val found = taskRepository.findById(saved.id!!)

        assertThat(found).isNotNull
        assertThat(found!!.title).isEqualTo("Find Test")
    }


    @Test
    fun `findById should return null when not exists`() {
        val found = taskRepository.findById(999L)
        assertThat(found).isNull()
    }


    @Test
    fun `findAll should return paginated tasks`() {

        for (i in 1..5) {
            taskRepository.save(
                Task(
                    title = "Task $i",
                    description = null,
                    status = TaskStatus.NEW,
                    createdAt = LocalDateTime.now().minusDays(i.toLong()),
                    updatedAt = LocalDateTime.now()
                )
            )
        }

        val (tasks, total) = taskRepository.findAll(0, 3, null)

        assertThat(tasks).hasSize(3)
        assertThat(total).isEqualTo(5)

        val titles = tasks.map { it.title }
        assertThat(titles).containsExactly("Task 1", "Task 2", "Task 3") // предполагаем, что createdAt убывает
    }


    @Test
    fun `findAll with status filter`() {

        taskRepository.save(
            Task(
                title = "New Task",
                description = null,
                status = TaskStatus.NEW,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        taskRepository.save(
            Task(
                title = "Done Task",
                description = null,
                status = TaskStatus.DONE,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val (tasks, total) = taskRepository.findAll(0, 10, TaskStatus.NEW)

        assertThat(tasks).hasSize(1)
        assertThat(tasks[0].title).isEqualTo("New Task")
        assertThat(total).isEqualTo(1)
    }


    @Test
    fun `updateStatus should update status and updatedAt`() {

        val saved = taskRepository.save(
            Task(
                title = "Update Test",
                description = null,
                status = TaskStatus.NEW,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        val oldUpdatedAt = saved.updatedAt

        Thread.sleep(10)
        val updatedRows = taskRepository.updateStatus(saved.id!!, TaskStatus.DONE)

        assertThat(updatedRows).isEqualTo(1)

        val updatedTask = taskRepository.findById(saved.id!!)
        assertThat(updatedTask).isNotNull
        assertThat(updatedTask!!.status).isEqualTo(TaskStatus.DONE)
        assertThat(updatedTask.updatedAt).isAfter(oldUpdatedAt)
    }


    @Test
    fun `deleteById should delete task`() {

        val saved = taskRepository.save(
            Task(
                title = "Delete Test",
                description = null,
                status = TaskStatus.NEW,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val deletedRows = taskRepository.deleteById(saved.id!!)
        assertThat(deletedRows).isEqualTo(1)

        val found = taskRepository.findById(saved.id!!)
        assertThat(found).isNull()
    }


    @Test
    fun `deleteById should return 0 when id not exists`() {
        val deletedRows = taskRepository.deleteById(999L)
        assertThat(deletedRows).isEqualTo(0)
    }


}