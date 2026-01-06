package com.realstate.domain.repository

import com.realstate.domain.entity.ScraperRun
import com.realstate.domain.entity.ScraperRunStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface ScraperRunRepository : JpaRepository<ScraperRun, UUID> {

    // Obtener la última ejecución
    fun findFirstByOrderByStartedAtDesc(): ScraperRun?

    // Obtener ejecuciones paginadas (más recientes primero)
    fun findAllByOrderByStartedAtDesc(pageable: Pageable): Page<ScraperRun>

    // Buscar ejecuciones por estado
    fun findByStatus(status: ScraperRunStatus): List<ScraperRun>

    // Verificar si hay una ejecución en curso
    fun existsByStatus(status: ScraperRunStatus): Boolean

    // Obtener ejecuciones en un rango de fechas
    fun findByStartedAtBetweenOrderByStartedAtDesc(
        start: LocalDateTime,
        end: LocalDateTime
    ): List<ScraperRun>

    // Estadísticas: total de propiedades encontradas en las últimas N ejecuciones
    @Query("""
        SELECT COALESCE(SUM(r.totalPropertiesFound), 0)
        FROM ScraperRun r
        WHERE r.status = 'COMPLETED'
        AND r.startedAt > :since
    """)
    fun sumPropertiesFoundSince(since: LocalDateTime): Long

    // Contar ejecuciones fallidas en las últimas N horas
    @Query("""
        SELECT COUNT(r)
        FROM ScraperRun r
        WHERE r.status = 'FAILED'
        AND r.startedAt > :since
    """)
    fun countFailedSince(since: LocalDateTime): Long
}
