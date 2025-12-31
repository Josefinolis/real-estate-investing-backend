package com.realstate.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "firebase_uid", unique = true, nullable = false, length = 128)
    val firebaseUid: String,

    @Column(nullable = false)
    val email: String,

    @Column(name = "fcm_token")
    var fcmToken: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val searchAlerts: MutableList<SearchAlert> = mutableListOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val favorites: MutableList<Favorite> = mutableListOf()
)
