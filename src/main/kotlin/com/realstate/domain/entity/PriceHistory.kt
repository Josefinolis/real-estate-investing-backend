package com.realstate.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "price_history")
data class PriceHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    val property: Property,

    @Column(precision = 12, scale = 2, nullable = false)
    val price: BigDecimal,

    @Column(name = "recorded_at")
    val recordedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PriceHistory
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
