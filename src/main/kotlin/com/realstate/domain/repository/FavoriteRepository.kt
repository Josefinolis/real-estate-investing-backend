package com.realstate.domain.repository

import com.realstate.domain.entity.Favorite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FavoriteRepository : JpaRepository<Favorite, UUID> {

    fun findByUserId(userId: UUID): List<Favorite>

    fun findByUserIdAndPropertyId(userId: UUID, propertyId: UUID): Favorite?

    fun existsByUserIdAndPropertyId(userId: UUID, propertyId: UUID): Boolean

    fun deleteByUserIdAndPropertyId(userId: UUID, propertyId: UUID)

    @Query("""
        SELECT f FROM Favorite f
        JOIN FETCH f.property
        WHERE f.user.id = :userId
        ORDER BY f.createdAt DESC
    """)
    fun findByUserIdWithProperty(@Param("userId") userId: UUID): List<Favorite>

    fun countByUserId(userId: UUID): Long
}
