package com.realstate.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "search_alerts")
data class SearchAlert(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(length = 100)
    var name: String? = null,

    @Column(name = "operation_type", length = 20)
    @Enumerated(EnumType.STRING)
    var operationType: OperationType? = null,

    @Column(name = "property_type", length = 50)
    @Enumerated(EnumType.STRING)
    var propertyType: PropertyType? = null,

    @Column(length = 100)
    var city: String? = null,

    @Column(columnDefinition = "TEXT[]")
    var zones: Array<String>? = null,

    @Column(name = "min_price", precision = 12, scale = 2)
    var minPrice: BigDecimal? = null,

    @Column(name = "max_price", precision = 12, scale = 2)
    var maxPrice: BigDecimal? = null,

    @Column(name = "min_rooms")
    var minRooms: Int? = null,

    @Column(name = "max_rooms")
    var maxRooms: Int? = null,

    @Column(name = "min_area", precision = 10, scale = 2)
    var minArea: BigDecimal? = null,

    @Column(name = "max_area", precision = 10, scale = 2)
    var maxArea: BigDecimal? = null,

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SearchAlert
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
