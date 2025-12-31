package com.realstate.domain.repository

import com.realstate.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByFirebaseUid(firebaseUid: String): User?
    fun findByEmail(email: String): User?
    fun existsByFirebaseUid(firebaseUid: String): Boolean
}
