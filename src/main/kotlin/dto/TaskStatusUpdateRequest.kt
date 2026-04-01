package dto

import jakarta.validation.constraints.NotNull
import model.TaskStatus

data class TaskStatusUpdateRequest(

    @field:NotNull(message = "Status is required")
    val status: TaskStatus

)