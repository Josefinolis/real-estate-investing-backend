package com.realstate.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.FileInputStream
import java.io.IOException
import jakarta.annotation.PostConstruct

@Configuration
class FirebaseConfig(
    @Value("\${firebase.credentials-path:firebase-service-account.json}")
    private val credentialsPath: String
) {
    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @PostConstruct
    fun initialize() {
        if (FirebaseApp.getApps().isNotEmpty()) {
            logger.info("Firebase already initialized")
            return
        }

        try {
            val credentials = loadCredentials()

            if (credentials != null) {
                val options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build()

                FirebaseApp.initializeApp(options)
                logger.info("Firebase initialized successfully")
            } else {
                logger.warn("Firebase credentials not found, notifications will be disabled")
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase: ${e.message}", e)
        }
    }

    private fun loadCredentials(): GoogleCredentials? {
        // Try to load from classpath first
        try {
            val resource = ClassPathResource(credentialsPath)
            if (resource.exists()) {
                return GoogleCredentials.fromStream(resource.inputStream)
            }
        } catch (e: IOException) {
            logger.debug("Credentials not found in classpath: ${e.message}")
        }

        // Try to load from file system
        try {
            val file = java.io.File(credentialsPath)
            if (file.exists()) {
                return GoogleCredentials.fromStream(FileInputStream(file))
            }
        } catch (e: IOException) {
            logger.debug("Credentials not found in file system: ${e.message}")
        }

        // Try environment variable
        val envPath = System.getenv("FIREBASE_CREDENTIALS_PATH")
        if (envPath != null) {
            try {
                val file = java.io.File(envPath)
                if (file.exists()) {
                    return GoogleCredentials.fromStream(FileInputStream(file))
                }
            } catch (e: IOException) {
                logger.debug("Credentials not found at env path: ${e.message}")
            }
        }

        return null
    }
}
