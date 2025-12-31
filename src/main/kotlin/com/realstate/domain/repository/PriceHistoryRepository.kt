package com.realstate.domain.repository

import com.realstate.domain.entity.PriceHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PriceHistoryRepository : JpaRepository<PriceHistory, UUID> {

    fun findByPropertyIdOrderByRecordedAtDesc(propertyId: UUID): List<PriceHistory>

    @Query("""
        SELECT ph FROM PriceHistory ph
        WHERE ph.property.id = :propertyId
        ORDER BY ph.recordedAt DESC
        LIMIT 1
    """)
    fun findLatestByPropertyId(@Param("propertyId") propertyId: UUID): PriceHistory?
}
