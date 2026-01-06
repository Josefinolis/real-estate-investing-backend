package com.realstate.dto

import com.realstate.domain.entity.ScraperRun
import com.realstate.domain.entity.ScraperRunStatus
import java.time.LocalDateTime
import java.util.UUID

data class ScraperRunDTO(
    val id: UUID,
    val startedAt: LocalDateTime,
    val finishedAt: LocalDateTime?,
    val status: String,
    val durationSeconds: Long?,
    val totalPropertiesFound: Int,
    val newProperties: Int,
    val updatedProperties: Int,
    val priceChanges: Int,
    val idealistaCount: Int,
    val pisoscomCount: Int,
    val fotocasaCount: Int,
    val errorMessage: String?,
    val errorDetails: String?,
    val filtersUsed: String?
) {
    companion object {
        fun fromEntity(entity: ScraperRun): ScraperRunDTO = ScraperRunDTO(
            id = entity.id!!,
            startedAt = entity.startedAt,
            finishedAt = entity.finishedAt,
            status = entity.status.name,
            durationSeconds = entity.getDurationSeconds(),
            totalPropertiesFound = entity.totalPropertiesFound,
            newProperties = entity.newProperties,
            updatedProperties = entity.updatedProperties,
            priceChanges = entity.priceChanges,
            idealistaCount = entity.idealistaCount,
            pisoscomCount = entity.pisoscomCount,
            fotocasaCount = entity.fotocasaCount,
            errorMessage = entity.errorMessage,
            errorDetails = entity.errorDetails,
            filtersUsed = entity.filtersUsed
        )
    }
}

data class ScraperRunListDTO(
    val runs: List<ScraperRunDTO>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int
)

data class ScraperStatusDTO(
    val isRunning: Boolean,
    val lastRun: ScraperRunDTO?,
    val config: ScraperConfigDTO?
)
