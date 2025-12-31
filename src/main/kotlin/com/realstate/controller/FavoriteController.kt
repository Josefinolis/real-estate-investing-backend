package com.realstate.controller

import com.realstate.dto.AddFavoriteRequest
import com.realstate.dto.FavoriteDTO
import com.realstate.dto.UpdateFavoriteRequest
import com.realstate.service.FavoriteService
import com.realstate.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/favorites")
class FavoriteController(
    private val favoriteService: FavoriteService,
    private val userService: UserService
) {

    @GetMapping
    fun getFavorites(
        @RequestHeader("X-Firebase-UID") firebaseUid: String
    ): ResponseEntity<List<FavoriteDTO>> {
        val user = userService.findByFirebaseUid(firebaseUid)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val favorites = favoriteService.getFavoritesByUser(user.id!!)
        return ResponseEntity.ok(favorites)
    }

    @PostMapping
    fun addFavorite(
        @RequestHeader("X-Firebase-UID") firebaseUid: String,
        @Valid @RequestBody request: AddFavoriteRequest
    ): ResponseEntity<FavoriteDTO> {
        val user = userService.findByFirebaseUid(firebaseUid)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val favorite = favoriteService.addFavorite(user.id!!, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(favorite)
    }

    @PutMapping("/{propertyId}")
    fun updateFavorite(
        @PathVariable propertyId: UUID,
        @RequestHeader("X-Firebase-UID") firebaseUid: String,
        @Valid @RequestBody request: UpdateFavoriteRequest
    ): ResponseEntity<FavoriteDTO> {
        val user = userService.findByFirebaseUid(firebaseUid)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val favorite = favoriteService.updateFavorite(user.id!!, propertyId, request)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(favorite)
    }

    @DeleteMapping("/{propertyId}")
    fun removeFavorite(
        @PathVariable propertyId: UUID,
        @RequestHeader("X-Firebase-UID") firebaseUid: String
    ): ResponseEntity<Void> {
        val user = userService.findByFirebaseUid(firebaseUid)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val removed = favoriteService.removeFavorite(user.id!!, propertyId)
        return if (removed) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/check/{propertyId}")
    fun checkFavorite(
        @PathVariable propertyId: UUID,
        @RequestHeader("X-Firebase-UID") firebaseUid: String
    ): ResponseEntity<Map<String, Boolean>> {
        val user = userService.findByFirebaseUid(firebaseUid)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val isFavorite = favoriteService.isFavorite(user.id!!, propertyId)
        return ResponseEntity.ok(mapOf("isFavorite" to isFavorite))
    }
}
