import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = [
    "controller", "service", "repository", "model", "dto", "exception", "config"
])
class TaskManagerApplication

fun main(args: Array<String>) {
    runApplication<TaskManagerApplication>(*args)
}