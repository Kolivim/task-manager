package exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)


    @ExceptionHandler(TaskNotFoundException::class)
    fun handleTaskNotFound(ex: TaskNotFoundException): ResponseEntity<Map<String, String>> {
        logger.warn("startMethod, task not found: {}", ex.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to (ex.message ?: "Task not found")))
    }


    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationErrors(ex: WebExchangeBindException): ResponseEntity<Map<String, String>> {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") { it.defaultMessage ?: it.field }
        logger.debug("startMethod, validation failed: {}", errors)
        return ResponseEntity.badRequest().body(mapOf("error" to errors))
    }


    /** Для исключений, отличных от TaskNotFoundException и WebExchangeBindException */
    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<Map<String, String>> {
        logger.error("startMethod, internal server error", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to "Internal server error: ${ex.message}"))
    }

}