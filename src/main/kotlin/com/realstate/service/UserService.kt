package com.realstate.service

import com.realstate.domain.entity.User
import com.realstate.domain.repository.UserRepository
import com.realstate.dto.RegisterUserRequest
import com.realstate.dto.UserDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun findByFirebaseUid(firebaseUid: String): User? =
        userRepository.findByFirebaseUid(firebaseUid)

    fun findById(id: UUID): User? =
        userRepository.findById(id).orElse(null)

    @Transactional
    fun registerUser(request: RegisterUserRequest): UserDTO {
        val existingUser = userRepository.findByFirebaseUid(request.firebaseUid)
        if (existingUser != null) {
            return UserDTO.fromEntity(existingUser)
        }

        val user = User(
            firebaseUid = request.firebaseUid,
            email = request.email
        )
        val savedUser = userRepository.save(user)
        return UserDTO.fromEntity(savedUser)
    }

    @Transactional
    fun updateFcmToken(firebaseUid: String, fcmToken: String): Boolean {
        val user = userRepository.findByFirebaseUid(firebaseUid) ?: return false
        user.fcmToken = fcmToken
        userRepository.save(user)
        return true
    }

    fun getUserByFirebaseUid(firebaseUid: String): UserDTO? =
        userRepository.findByFirebaseUid(firebaseUid)?.let { UserDTO.fromEntity(it) }
}
