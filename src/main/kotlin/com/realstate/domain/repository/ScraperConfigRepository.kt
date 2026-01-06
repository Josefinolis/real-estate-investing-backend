package com.realstate.domain.repository

import com.realstate.domain.entity.ScraperConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScraperConfigRepository : JpaRepository<ScraperConfig, UUID> {
    // Solo debería haber una configuración, pero por si acaso
    fun findFirstByOrderByUpdatedAtDesc(): ScraperConfig?
}
