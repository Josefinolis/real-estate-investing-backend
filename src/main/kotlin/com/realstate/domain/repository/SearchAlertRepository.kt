package com.realstate.domain.repository

import com.realstate.domain.entity.SearchAlert
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SearchAlertRepository : JpaRepository<SearchAlert, UUID> {

    fun findByUserIdAndIsActiveTrue(userId: UUID): List<SearchAlert>

    fun findByUserId(userId: UUID): List<SearchAlert>

    @Query("SELECT sa FROM SearchAlert sa WHERE sa.isActive = true")
    fun findAllActive(): List<SearchAlert>

    fun countByUserId(userId: UUID): Long
}
