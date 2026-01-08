package com.realstate.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "scraper_config")
data class ScraperConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    // Ciudades a scrapear (PostgreSQL TEXT[])
    @Column(name = "cities", columnDefinition = "TEXT[]")
    var cities: Array<String> = arrayOf(
        "Madrid", "Barcelona", "Valencia", "Sevilla", "Zaragoza", "Málaga", "Murcia",
        "Palma de Mallorca", "Bilbao", "Alicante", "Córdoba", "Valladolid", "Granada"
    ),

    // Tipos de operación: VENTA, ALQUILER o ambos
    @Column(name = "operation_types", columnDefinition = "TEXT[]")
    var operationTypes: Array<String> = arrayOf("VENTA", "ALQUILER"),

    // Tipos de propiedad (null = todos)
    @Column(name = "property_types", columnDefinition = "TEXT[]")
    var propertyTypes: Array<String>? = null,

    // Filtros de precio
    @Column(name = "min_price", precision = 12, scale = 2)
    var minPrice: BigDecimal? = null,

    @Column(name = "max_price", precision = 12, scale = 2)
    var maxPrice: BigDecimal? = null,

    // Filtros de habitaciones
    @Column(name = "min_rooms")
    var minRooms: Int? = null,

    @Column(name = "max_rooms")
    var maxRooms: Int? = null,

    // Filtros de área
    @Column(name = "min_area", precision = 10, scale = 2)
    var minArea: BigDecimal? = null,

    @Column(name = "max_area", precision = 10, scale = 2)
    var maxArea: BigDecimal? = null,

    // Configuración de ejecución
    @Column(name = "enabled")
    var enabled: Boolean = true,

    // Expresión cron para programación (default: cada 30 minutos)
    @Column(name = "cron_expression", length = 50)
    var cronExpression: String = "0 */30 * * * *",

    // Fuentes activas: PISOSCOM, FOTOCASA (Idealista requiere API oficial por protección anti-bot)
    @Column(name = "sources", columnDefinition = "TEXT[]")
    var sources: Array<String> = arrayOf("PISOSCOM", "FOTOCASA"),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ScraperConfig
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
