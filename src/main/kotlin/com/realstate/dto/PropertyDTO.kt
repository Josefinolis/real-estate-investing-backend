package com.realstate.dto

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.Property
import com.realstate.domain.entity.PropertySource
import com.realstate.domain.entity.PropertyType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class PropertyDTO(
    val id: UUID,
    val externalId: String,
    val source: PropertySource,
    val title: String?,
    val description: String?,
    val price: BigDecimal?,
    val pricePerM2: BigDecimal?,
    val propertyType: PropertyType?,
    val operationType: OperationType?,
    val rooms: Int?,
    val bathrooms: Int?,
    val areaM2: BigDecimal?,
    val address: String?,
    val city: String?,
    val postalCode: String?,
    val zone: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val imageUrls: List<String>?,
    val url: String?,
    val isActive: Boolean,
    val firstSeenAt: LocalDateTime,
    val lastSeenAt: LocalDateTime
) {
    companion object {
        fun fromEntity(property: Property): PropertyDTO = PropertyDTO(
            id = property.id!!,
            externalId = property.externalId,
            source = property.source,
            title = property.title,
            description = property.description,
            price = property.price,
            pricePerM2 = property.pricePerM2,
            propertyType = property.propertyType,
            operationType = property.operationType,
            rooms = property.rooms,
            bathrooms = property.bathrooms,
            areaM2 = property.areaM2,
            address = property.address,
            city = property.city,
            postalCode = property.postalCode,
            zone = property.zone,
            latitude = property.latitude,
            longitude = property.longitude,
            imageUrls = property.imageUrls?.toList(),
            url = property.url,
            isActive = property.isActive,
            firstSeenAt = property.firstSeenAt,
            lastSeenAt = property.lastSeenAt
        )
    }
}

data class PropertyListDTO(
    val properties: List<PropertyDTO>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int
)

data class PriceHistoryDTO(
    val id: UUID,
    val price: BigDecimal,
    val recordedAt: LocalDateTime
)

data class PropertyDetailDTO(
    val property: PropertyDTO,
    val priceHistory: List<PriceHistoryDTO>,
    val isFavorite: Boolean = false
)
