package com.realstate.service

import com.realstate.domain.entity.SearchAlert
import com.realstate.domain.entity.User
import com.realstate.domain.repository.SearchAlertRepository
import com.realstate.domain.repository.UserRepository
import com.realstate.dto.AlertDTO
import com.realstate.dto.CreateAlertRequest
import com.realstate.dto.UpdateAlertRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AlertService(
    private val searchAlertRepository: SearchAlertRepository,
    private val userRepository: UserRepository,
    private val propertyService: PropertyService,
    private val notificationService: NotificationService
) {
    private val logger = LoggerFactory.getLogger(AlertService::class.java)

    private var lastCheckTime: LocalDateTime = LocalDateTime.now().minusMinutes(30)

    fun getAlertsByUser(userId: UUID): List<AlertDTO> {
        return searchAlertRepository.findByUserId(userId)
            .map { AlertDTO.fromEntity(it) }
    }

    fun getActiveAlertsByUser(userId: UUID): List<AlertDTO> {
        return searchAlertRepository.findByUserIdAndIsActiveTrue(userId)
            .map { AlertDTO.fromEntity(it) }
    }

    fun getAlertById(id: UUID): AlertDTO? {
        return searchAlertRepository.findById(id)
            .map { AlertDTO.fromEntity(it) }
            .orElse(null)
    }

    @Transactional
    fun createAlert(userId: UUID, request: CreateAlertRequest): AlertDTO {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found: $userId") }

        val alert = SearchAlert(
            user = user,
            name = request.name,
            operationType = request.operationType,
            propertyType = request.propertyType,
            city = request.city,
            zones = request.zones?.toTypedArray(),
            minPrice = request.minPrice,
            maxPrice = request.maxPrice,
            minRooms = request.minRooms,
            maxRooms = request.maxRooms,
            minArea = request.minArea,
            maxArea = request.maxArea
        )

        val saved = searchAlertRepository.save(alert)
        logger.info("Alert created: ${saved.id} for user $userId")
        return AlertDTO.fromEntity(saved)
    }

    @Transactional
    fun updateAlert(id: UUID, userId: UUID, request: UpdateAlertRequest): AlertDTO? {
        val alert = searchAlertRepository.findById(id).orElse(null) ?: return null

        if (alert.user.id != userId) {
            throw IllegalArgumentException("Alert does not belong to user")
        }

        alert.apply {
            request.name?.let { name = it }
            request.operationType?.let { operationType = it }
            request.propertyType?.let { propertyType = it }
            request.city?.let { city = it }
            request.zones?.let { zones = it.toTypedArray() }
            request.minPrice?.let { minPrice = it }
            request.maxPrice?.let { maxPrice = it }
            request.minRooms?.let { minRooms = it }
            request.maxRooms?.let { maxRooms = it }
            request.minArea?.let { minArea = it }
            request.maxArea?.let { maxArea = it }
            request.isActive?.let { isActive = it }
        }

        val saved = searchAlertRepository.save(alert)
        return AlertDTO.fromEntity(saved)
    }

    @Transactional
    fun deleteAlert(id: UUID, userId: UUID): Boolean {
        val alert = searchAlertRepository.findById(id).orElse(null) ?: return false

        if (alert.user.id != userId) {
            throw IllegalArgumentException("Alert does not belong to user")
        }

        searchAlertRepository.delete(alert)
        logger.info("Alert deleted: $id")
        return true
    }

    @Transactional
    fun checkNewMatchesAndNotify() {
        logger.info("Checking for new property matches since $lastCheckTime")

        val activeAlerts = searchAlertRepository.findAllActive()
        val checkTime = lastCheckTime

        for (alert in activeAlerts) {
            try {
                val newProperties = propertyService.findNewPropertiesForAlert(checkTime, alert)

                if (newProperties.isNotEmpty()) {
                    logger.info("Found ${newProperties.size} new properties for alert ${alert.id}")

                    val user = alert.user
                    if (user.fcmToken != null) {
                        for (property in newProperties) {
                            notificationService.sendNewPropertyNotification(
                                fcmToken = user.fcmToken!!,
                                alertName = alert.name ?: "Tu alerta",
                                propertyTitle = property.title ?: "Nuevo inmueble",
                                propertyPrice = property.price?.toString() ?: "Precio no disponible",
                                propertyId = property.id!!.toString()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Error checking alert ${alert.id}: ${e.message}", e)
            }
        }

        lastCheckTime = LocalDateTime.now()
    }
}
