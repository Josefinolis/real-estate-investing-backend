package com.realstate.controller

import com.realstate.dto.AlertDTO
import com.realstate.dto.CreateAlertRequest
import com.realstate.dto.UpdateAlertRequest
import com.realstate.service.AlertService
import com.realstate.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/alerts")
class AlertController(
    private val alertService: AlertService,
    private val userService: UserService
) {

    @GetMapping
    fun getAlerts(
        @RequestHeader("X-Firebase-UID") firebaseUid: String,
        @RequestParam(defaultValue = "false") activeOnly: Boolean
    ): ResponseEntity<List<AlertDTO>> {
        val user = userService.findByFirebaseUid(firebaseUid)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val alerts = if (activeOnly) {
            alertService.getActiveAlertsByUser(user.id!!)
        } else {
            alertService.getAlertsByUser(user.id!!)
        }

        return ResponseEntity.ok(alerts)
    }

    @GetMapping("/{id}")
    fun getAlertById(
        @PathVariable id: UUID,
        @RequestHeader("X-Firebase-UID") firebaseUid: String
    ): ResponseEntity<AlertDTO> {
        val user = userService.findByFirebaseUid(firebaseUid)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val alert = alertService.getAlertById(id)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(alert)
    }

    @PostMapping
    fun createAlert(
        @RequestHeader("X-Firebase-UID") firebaseUid: String,
        @Valid @RequestBody request: CreateAlertRequest
    ): ResponseEntity<AlertDTO> {
        val user = userService.findByFirebaseUid(firebaseUid)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val alert = alertService.createAlert(user.id!!, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(alert)
    }

    @PutMapping("/{id}")
    fun updateAlert(
        @PathVariable id: UUID,
        @RequestHeader("X-Firebase-UID") firebaseUid: String,
        @Valid @RequestBody request: UpdateAlertRequest
    ): ResponseEntity<AlertDTO> {
        val user = userService.findByFirebaseUid(firebaseUid)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val alert = alertService.updateAlert(id, user.id!!, request)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(alert)
    }

    @DeleteMapping("/{id}")
    fun deleteAlert(
        @PathVariable id: UUID,
        @RequestHeader("X-Firebase-UID") firebaseUid: String
    ): ResponseEntity<Void> {
        val user = userService.findByFirebaseUid(firebaseUid)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val deleted = alertService.deleteAlert(id, user.id!!)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
