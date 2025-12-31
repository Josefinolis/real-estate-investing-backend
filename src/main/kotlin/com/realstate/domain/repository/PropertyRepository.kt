package com.realstate.domain.repository

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.Property
import com.realstate.domain.entity.PropertySource
import com.realstate.domain.entity.PropertyType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface PropertyRepository : JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

    fun findByExternalIdAndSource(externalId: String, source: PropertySource): Property?

    fun existsByExternalIdAndSource(externalId: String, source: PropertySource): Boolean

    @Query("""
        SELECT p FROM Property p
        WHERE p.isActive = true
        AND (:city IS NULL OR p.city = :city)
        AND (:operationType IS NULL OR p.operationType = :operationType)
        AND (:propertyType IS NULL OR p.propertyType = :propertyType)
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND (:minRooms IS NULL OR p.rooms >= :minRooms)
        AND (:maxRooms IS NULL OR p.rooms <= :maxRooms)
        AND (:minArea IS NULL OR p.areaM2 >= :minArea)
        AND (:maxArea IS NULL OR p.areaM2 <= :maxArea)
        ORDER BY p.lastSeenAt DESC
    """)
    fun searchProperties(
        @Param("city") city: String?,
        @Param("operationType") operationType: OperationType?,
        @Param("propertyType") propertyType: PropertyType?,
        @Param("minPrice") minPrice: BigDecimal?,
        @Param("maxPrice") maxPrice: BigDecimal?,
        @Param("minRooms") minRooms: Int?,
        @Param("maxRooms") maxRooms: Int?,
        @Param("minArea") minArea: BigDecimal?,
        @Param("maxArea") maxArea: BigDecimal?,
        pageable: Pageable
    ): Page<Property>

    @Query("""
        SELECT p FROM Property p
        WHERE p.isActive = true
        AND p.firstSeenAt > :since
        AND (:city IS NULL OR p.city = :city)
        AND (:operationType IS NULL OR p.operationType = :operationType)
        AND (:propertyType IS NULL OR p.propertyType = :propertyType)
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND (:minRooms IS NULL OR p.rooms >= :minRooms)
        AND (:maxRooms IS NULL OR p.rooms <= :maxRooms)
        AND (:minArea IS NULL OR p.areaM2 >= :minArea)
        AND (:maxArea IS NULL OR p.areaM2 <= :maxArea)
    """)
    fun findNewPropertiesMatchingCriteria(
        @Param("since") since: LocalDateTime,
        @Param("city") city: String?,
        @Param("operationType") operationType: OperationType?,
        @Param("propertyType") propertyType: PropertyType?,
        @Param("minPrice") minPrice: BigDecimal?,
        @Param("maxPrice") maxPrice: BigDecimal?,
        @Param("minRooms") minRooms: Int?,
        @Param("maxRooms") maxRooms: Int?,
        @Param("minArea") minArea: BigDecimal?,
        @Param("maxArea") maxArea: BigDecimal?
    ): List<Property>

    fun findByIsActiveTrue(): List<Property>

    @Query("UPDATE Property p SET p.isActive = false WHERE p.lastSeenAt < :before")
    fun markInactiveOlderThan(@Param("before") before: LocalDateTime)
}
