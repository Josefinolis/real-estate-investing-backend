package com.realstate.dto

import com.realstate.domain.entity.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import java.util.UUID

data class UserDTO(
    val id: UUID,
    val firebaseUid: String,
    val email: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun fromEntity(user: User): UserDTO = UserDTO(
            id = user.id!!,
            firebaseUid = user.firebaseUid,
            email = user.email,
            createdAt = user.createdAt
        )
    }
}

data class RegisterUserRequest(
    @field:NotBlank
    val firebaseUid: String,
    @field:Email
    @field:NotBlank
    val email: String
)

data class UpdateFcmTokenRequest(
    @field:NotBlank
    val fcmToken: String
)
