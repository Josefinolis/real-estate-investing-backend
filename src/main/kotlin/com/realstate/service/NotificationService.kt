package com.realstate.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NotificationService {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    fun sendNewPropertyNotification(
        fcmToken: String,
        alertName: String,
        propertyTitle: String,
        propertyPrice: String,
        propertyId: String
    ): Boolean {
        return try {
            val message = Message.builder()
                .setToken(fcmToken)
                .setNotification(
                    Notification.builder()
                        .setTitle("Nuevo inmueble: $alertName")
                        .setBody("$propertyTitle - $propertyPrice€")
                        .build()
                )
                .putData("type", "new_property")
                .putData("propertyId", propertyId)
                .putData("alertName", alertName)
                .build()

            val response = FirebaseMessaging.getInstance().send(message)
            logger.info("Notification sent successfully: $response")
            true
        } catch (e: Exception) {
            logger.error("Failed to send notification to token $fcmToken: ${e.message}", e)
            false
        }
    }

    fun sendPriceChangeNotification(
        fcmToken: String,
        propertyTitle: String,
        oldPrice: String,
        newPrice: String,
        propertyId: String
    ): Boolean {
        return try {
            val message = Message.builder()
                .setToken(fcmToken)
                .setNotification(
                    Notification.builder()
                        .setTitle("Cambio de precio")
                        .setBody("$propertyTitle: $oldPrice€ → $newPrice€")
                        .build()
                )
                .putData("type", "price_change")
                .putData("propertyId", propertyId)
                .putData("oldPrice", oldPrice)
                .putData("newPrice", newPrice)
                .build()

            val response = FirebaseMessaging.getInstance().send(message)
            logger.info("Price change notification sent: $response")
            true
        } catch (e: Exception) {
            logger.error("Failed to send price change notification: ${e.message}", e)
            false
        }
    }
}
