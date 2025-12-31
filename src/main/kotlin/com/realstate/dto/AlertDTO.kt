package com.realstate.dto

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.PropertyType
import com.realstate.domain.entity.SearchAlert
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class AlertDTO(
    val id: UUID? = null,
    @field:Size(max = 100)
    val name: String? = null,
    val operationType: OperationType? = null,
    val propertyType: PropertyType? = null,
    val city: String? = null,
    val zones: List<String>? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val minRooms: Int? = null,
    val maxRooms: Int? = null,
    val minArea: BigDecimal? = null,
    val maxArea: BigDecimal? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime? = null
) {
    companion object {
        fun fromEntity(alert: SearchAlert): AlertDTO = AlertDTO(
            id = alert.id,
            name = alert.name,
            operationType = alert.operationType,
            propertyType = alert.propertyType,
            city = alert.city,
            zones = alert.zones?.toList(),
            minPrice = alert.minPrice,
            maxPrice = alert.maxPrice,
            minRooms = alert.minRooms,
            maxRooms = alert.maxRooms,
            minArea = alert.minArea,
            maxArea = alert.maxArea,
            isActive = alert.isActive,
            createdAt = alert.createdAt
        )
    }
}

data class CreateAlertRequest(
    @field:Size(max = 100)
    val name: String? = null,
    val operationType: OperationType? = null,
    val propertyType: PropertyType? = null,
    val city: String? = null,
    val zones: List<String>? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val minRooms: Int? = null,
    val maxRooms: Int? = null,
    val minArea: BigDecimal? = null,
    val maxArea: BigDecimal? = null
)

data class UpdateAlertRequest(
    @field:Size(max = 100)
    val name: String? = null,
    val operationType: OperationType? = null,
    val propertyType: PropertyType? = null,
    val city: String? = null,
    val zones: List<String>? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val minRooms: Int? = null,
    val maxRooms: Int? = null,
    val minArea: BigDecimal? = null,
    val maxArea: BigDecimal? = null,
    val isActive: Boolean? = null
)
