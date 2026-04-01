package repository

import model.Task
import model.TaskStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime

@Repository
class TaskRepositoryImpl(private val jdbcClient: JdbcClient) : TaskRepository {


    override fun save(task: Task): Task {
        val sql = """
        INSERT INTO tasks (title, description, status, created_at, updated_at)
        VALUES (:title, :description, :status, :createdAt, :updatedAt)
    """.trimIndent()

        val keyHolder = GeneratedKeyHolder()

        jdbcClient.sql(sql)
            .param("title", task.title)
            .param("description", task.description)
            .param("status", task.status.name)
            .param("createdAt", task.createdAt)
            .param("updatedAt", task.updatedAt)
            .update(keyHolder)

        val keys = keyHolder.keys ?: throw RuntimeException("No keys returned")
        val id = (keys["ID"] as Number).toLong()
        return task.copy(id = id)
    }


    override fun findById(id: Long): Task? {
        val sql = "SELECT * FROM tasks WHERE id = :id"
        val results = jdbcClient.sql(sql)
            .param("id", id)
            .query { rs, _ -> mapRow(rs) }
            .list()
        return results.singleOrNull()
    }


    override fun findAll(page: Int, size: Int, status: TaskStatus?): Pair<List<Task>, Long> {
        val offset = page * size
        val whereClause = if (status != null) "WHERE status = :status" else ""
        val countSql = "SELECT COUNT(*) FROM tasks $whereClause"
        val dataSql = """
            SELECT * FROM tasks $whereClause
            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val countQuery = jdbcClient.sql(countSql)
        if (status != null) countQuery.param("status", status.name)
        val total = countQuery.query { rs, _ -> rs.getLong(1) }.single()

        val dataQuery = jdbcClient.sql(dataSql)
            .param("limit", size)
            .param("offset", offset)
        if (status != null) dataQuery.param("status", status.name)
        val tasks = dataQuery.query { rs, _ -> mapRow(rs) }.list()

        return tasks to total
    }


    override fun updateStatus(id: Long, status: TaskStatus): Int {
        val sql = """
            UPDATE tasks SET status = :status, updated_at = :updatedAt
            WHERE id = :id
        """.trimIndent()
        return jdbcClient.sql(sql)
            .param("status", status.name)
            .param("updatedAt", LocalDateTime.now())
            .param("id", id)
            .update()
    }


    override fun deleteById(id: Long): Int {
        val sql = "DELETE FROM tasks WHERE id = :id"
        return jdbcClient.sql(sql)
            .param("id", id)
            .update()
    }


    private fun mapRow(rs: ResultSet): Task = Task(
        id = rs.getLong("id"),
        title = rs.getString("title"),
        description = rs.getString("description"),
        status = TaskStatus.valueOf(rs.getString("status")),
        createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
        updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
    )


}