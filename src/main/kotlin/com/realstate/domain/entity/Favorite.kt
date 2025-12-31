package com.realstate.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "favorites",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "property_id"])]
)
data class Favorite(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    val property: Property,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Favorite
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
