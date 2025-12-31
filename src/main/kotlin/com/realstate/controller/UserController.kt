package com.realstate.controller

import com.realstate.dto.RegisterUserRequest
import com.realstate.dto.UpdateFcmTokenRequest
import com.realstate.dto.UserDTO
import com.realstate.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class UserController(
    private val userService: UserService
) {

    @PostMapping("/register")
    fun registerUser(
        @Valid @RequestBody request: RegisterUserRequest
    ): ResponseEntity<UserDTO> {
        val user = userService.registerUser(request)
        return ResponseEntity.ok(user)
    }

    @PostMapping("/fcm-token")
    fun updateFcmToken(
        @RequestHeader("X-Firebase-UID") firebaseUid: String,
        @Valid @RequestBody request: UpdateFcmTokenRequest
    ): ResponseEntity<Map<String, Boolean>> {
        val updated = userService.updateFcmToken(firebaseUid, request.fcmToken)
        return ResponseEntity.ok(mapOf("success" to updated))
    }

    @GetMapping("/me")
    fun getCurrentUser(
        @RequestHeader("X-Firebase-UID") firebaseUid: String
    ): ResponseEntity<UserDTO> {
        val user = userService.getUserByFirebaseUid(firebaseUid)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user)
    }
}
