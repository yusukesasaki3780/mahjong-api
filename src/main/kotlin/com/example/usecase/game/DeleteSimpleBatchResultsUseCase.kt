package com.example.usecase.game

import com.example.domain.repository.GameResultRepository
import java.util.UUID

class DeleteSimpleBatchResultsUseCase(
    private val repository: GameResultRepository
) {

    data class Command(val userId: Long, val simpleBatchId: UUID)

    data class Result(val deletedCount: Int)

    suspend operator fun invoke(command: Command): Result {
        val deleted = repository.deleteBySimpleBatch(command.userId, command.simpleBatchId)
        return Result(deletedCount = deleted)
    }
}
