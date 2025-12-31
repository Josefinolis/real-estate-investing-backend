package com.realstate.dto

import com.realstate.domain.entity.Favorite
import java.time.LocalDateTime
import java.util.UUID

data class FavoriteDTO(
    val id: UUID,
    val propertyId: UUID,
    val property: PropertyDTO?,
    val notes: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun fromEntity(favorite: Favorite, includeProperty: Boolean = false): FavoriteDTO = FavoriteDTO(
            id = favorite.id!!,
            propertyId = favorite.property.id!!,
            property = if (includeProperty) PropertyDTO.fromEntity(favorite.property) else null,
            notes = favorite.notes,
            createdAt = favorite.createdAt
        )
    }
}

data class AddFavoriteRequest(
    val propertyId: UUID,
    val notes: String? = null
)

data class UpdateFavoriteRequest(
    val notes: String?
)
