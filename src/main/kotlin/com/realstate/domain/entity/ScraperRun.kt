package com.realstate.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "scraper_runs")
data class ScraperRun(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null,

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    var status: ScraperRunStatus = ScraperRunStatus.RUNNING,

    // Métricas generales
    @Column(name = "total_properties_found")
    var totalPropertiesFound: Int = 0,

    @Column(name = "new_properties")
    var newProperties: Int = 0,

    @Column(name = "updated_properties")
    var updatedProperties: Int = 0,

    @Column(name = "price_changes")
    var priceChanges: Int = 0,

    // Métricas por fuente
    @Column(name = "idealista_count")
    var idealistaCount: Int = 0,

    @Column(name = "pisoscom_count")
    var pisoscomCount: Int = 0,

    @Column(name = "fotocasa_count")
    var fotocasaCount: Int = 0,

    // Información de errores
    @Column(name = "error_message", length = 500)
    var errorMessage: String? = null,

    @Column(name = "error_details", columnDefinition = "TEXT")
    var errorDetails: String? = null,

    // Snapshot de los filtros usados (JSON)
    @Column(name = "filters_used", columnDefinition = "TEXT")
    var filtersUsed: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ScraperRun
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    fun markCompleted() {
        this.status = ScraperRunStatus.COMPLETED
        this.finishedAt = LocalDateTime.now()
    }

    fun markFailed(message: String, details: String? = null) {
        this.status = ScraperRunStatus.FAILED
        this.finishedAt = LocalDateTime.now()
        this.errorMessage = message.take(500)
        this.errorDetails = details
    }

    fun getDurationSeconds(): Long? {
        return finishedAt?.let {
            java.time.Duration.between(startedAt, it).seconds
        }
    }
}

enum class ScraperRunStatus {
    RUNNING,
    COMPLETED,
    FAILED
}
