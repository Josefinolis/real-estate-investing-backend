package com.realstate.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "properties",
    uniqueConstraints = [UniqueConstraint(columnNames = ["external_id", "source"])]
)
data class Property(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "external_id", nullable = false, length = 100)
    val externalId: String,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    val source: PropertySource,

    @Column(length = 500)
    var title: String? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(precision = 12, scale = 2)
    var price: BigDecimal? = null,

    @Column(name = "price_per_m2", precision = 10, scale = 2)
    var pricePerM2: BigDecimal? = null,

    @Column(name = "property_type", length = 50)
    @Enumerated(EnumType.STRING)
    var propertyType: PropertyType? = null,

    @Column(name = "operation_type", length = 20)
    @Enumerated(EnumType.STRING)
    var operationType: OperationType? = null,

    var rooms: Int? = null,

    var bathrooms: Int? = null,

    @Column(name = "area_m2", precision = 10, scale = 2)
    var areaM2: BigDecimal? = null,

    @Column(length = 500)
    var address: String? = null,

    @Column(length = 100)
    var city: String? = null,

    @Column(length = 100)
    var zone: String? = null,

    @Column(precision = 10, scale = 8)
    var latitude: BigDecimal? = null,

    @Column(precision = 11, scale = 8)
    var longitude: BigDecimal? = null,

    @Column(name = "image_urls", columnDefinition = "TEXT[]")
    var imageUrls: Array<String>? = null,

    @Column(length = 500)
    var url: String? = null,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(name = "first_seen_at")
    val firstSeenAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_seen_at")
    var lastSeenAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "property", cascade = [CascadeType.ALL], orphanRemoval = true)
    val priceHistory: MutableList<PriceHistory> = mutableListOf(),

    @OneToMany(mappedBy = "property", cascade = [CascadeType.ALL], orphanRemoval = true)
    val favorites: MutableList<Favorite> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Property
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

enum class PropertySource {
    IDEALISTA,
    FOTOCASA,
    PISOSCOM
}

enum class PropertyType {
    APARTAMENTO,
    PISO,
    CASA,
    CHALET,
    DUPLEX,
    ATICO,
    ESTUDIO,
    LOFT,
    OTRO
}

enum class OperationType {
    VENTA,
    ALQUILER
}
