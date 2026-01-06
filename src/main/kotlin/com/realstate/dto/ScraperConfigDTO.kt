package com.realstate.dto

import com.realstate.domain.entity.ScraperConfig
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class ScraperConfigDTO(
    val id: UUID?,
    val cities: List<String>,
    val operationTypes: List<String>,
    val propertyTypes: List<String>?,
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?,
    val minRooms: Int?,
    val maxRooms: Int?,
    val minArea: BigDecimal?,
    val maxArea: BigDecimal?,
    val enabled: Boolean,
    val cronExpression: String,
    val sources: List<String>,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(entity: ScraperConfig): ScraperConfigDTO = ScraperConfigDTO(
            id = entity.id,
            cities = entity.cities.toList(),
            operationTypes = entity.operationTypes.toList(),
            propertyTypes = entity.propertyTypes?.toList(),
            minPrice = entity.minPrice,
            maxPrice = entity.maxPrice,
            minRooms = entity.minRooms,
            maxRooms = entity.maxRooms,
            minArea = entity.minArea,
            maxArea = entity.maxArea,
            enabled = entity.enabled,
            cronExpression = entity.cronExpression,
            sources = entity.sources.toList(),
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(existingId: UUID? = null): ScraperConfig = ScraperConfig(
        id = existingId ?: id,
        cities = cities.toTypedArray(),
        operationTypes = operationTypes.toTypedArray(),
        propertyTypes = propertyTypes?.toTypedArray(),
        minPrice = minPrice,
        maxPrice = maxPrice,
        minRooms = minRooms,
        maxRooms = maxRooms,
        minArea = minArea,
        maxArea = maxArea,
        enabled = enabled,
        cronExpression = cronExpression,
        sources = sources.toTypedArray(),
        updatedAt = LocalDateTime.now()
    )
}

// DTO para actualizar configuración (sin id ni updatedAt)
data class ScraperConfigUpdateDTO(
    val cities: List<String>,
    val operationTypes: List<String>,
    val propertyTypes: List<String>?,
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?,
    val minRooms: Int?,
    val maxRooms: Int?,
    val minArea: BigDecimal?,
    val maxArea: BigDecimal?,
    val enabled: Boolean,
    val cronExpression: String,
    val sources: List<String>
)
