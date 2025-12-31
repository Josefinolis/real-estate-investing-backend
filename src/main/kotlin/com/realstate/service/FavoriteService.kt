package com.realstate.service

import com.realstate.domain.entity.Favorite
import com.realstate.domain.repository.FavoriteRepository
import com.realstate.domain.repository.PropertyRepository
import com.realstate.domain.repository.UserRepository
import com.realstate.dto.AddFavoriteRequest
import com.realstate.dto.FavoriteDTO
import com.realstate.dto.UpdateFavoriteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FavoriteService(
    private val favoriteRepository: FavoriteRepository,
    private val userRepository: UserRepository,
    private val propertyRepository: PropertyRepository
) {
    private val logger = LoggerFactory.getLogger(FavoriteService::class.java)

    fun getFavoritesByUser(userId: UUID): List<FavoriteDTO> {
        return favoriteRepository.findByUserIdWithProperty(userId)
            .map { FavoriteDTO.fromEntity(it, includeProperty = true) }
    }

    fun isFavorite(userId: UUID, propertyId: UUID): Boolean {
        return favoriteRepository.existsByUserIdAndPropertyId(userId, propertyId)
    }

    @Transactional
    fun addFavorite(userId: UUID, request: AddFavoriteRequest): FavoriteDTO {
        val existing = favoriteRepository.findByUserIdAndPropertyId(userId, request.propertyId)
        if (existing != null) {
            return FavoriteDTO.fromEntity(existing, includeProperty = true)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found: $userId") }

        val property = propertyRepository.findById(request.propertyId)
            .orElseThrow { IllegalArgumentException("Property not found: ${request.propertyId}") }

        val favorite = Favorite(
            user = user,
            property = property,
            notes = request.notes
        )

        val saved = favoriteRepository.save(favorite)
        logger.info("Favorite added: property ${request.propertyId} for user $userId")
        return FavoriteDTO.fromEntity(saved, includeProperty = true)
    }

    @Transactional
    fun updateFavorite(userId: UUID, propertyId: UUID, request: UpdateFavoriteRequest): FavoriteDTO? {
        val favorite = favoriteRepository.findByUserIdAndPropertyId(userId, propertyId) ?: return null

        favorite.notes = request.notes
        val saved = favoriteRepository.save(favorite)
        return FavoriteDTO.fromEntity(saved, includeProperty = true)
    }

    @Transactional
    fun removeFavorite(userId: UUID, propertyId: UUID): Boolean {
        val favorite = favoriteRepository.findByUserIdAndPropertyId(userId, propertyId) ?: return false

        favoriteRepository.delete(favorite)
        logger.info("Favorite removed: property $propertyId for user $userId")
        return true
    }

    fun countFavorites(userId: UUID): Long {
        return favoriteRepository.countByUserId(userId)
    }
}
