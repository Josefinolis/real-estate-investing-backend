package com.realstate.controller

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.PropertyType
import com.realstate.dto.PriceHistoryDTO
import com.realstate.dto.PropertyDetailDTO
import com.realstate.dto.PropertyListDTO
import com.realstate.dto.SearchFilterDTO
import com.realstate.service.PropertyService
import com.realstate.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/api/properties")
class PropertyController(
    private val propertyService: PropertyService,
    private val userService: UserService
) {

    @GetMapping
    fun searchProperties(
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) operationType: OperationType?,
        @RequestParam(required = false) propertyType: PropertyType?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) minRooms: Int?,
        @RequestParam(required = false) maxRooms: Int?,
        @RequestParam(required = false) minArea: BigDecimal?,
        @RequestParam(required = false) maxArea: BigDecimal?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PropertyListDTO> {
        val filter = SearchFilterDTO(
            city = city,
            operationType = operationType,
            propertyType = propertyType,
            minPrice = minPrice,
            maxPrice = maxPrice,
            minRooms = minRooms,
            maxRooms = maxRooms,
            minArea = minArea,
            maxArea = maxArea,
            page = page,
            size = size.coerceIn(1, 100)
        )

        val result = propertyService.searchProperties(filter)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{id}")
    fun getPropertyById(
        @PathVariable id: UUID,
        @RequestHeader("X-Firebase-UID", required = false) firebaseUid: String?
    ): ResponseEntity<PropertyDetailDTO> {
        val userId = firebaseUid?.let { userService.findByFirebaseUid(it)?.id }
        val property = propertyService.getPropertyById(id, userId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(property)
    }

    @GetMapping("/{id}/price-history")
    fun getPriceHistory(
        @PathVariable id: UUID
    ): ResponseEntity<List<PriceHistoryDTO>> {
        val history = propertyService.getPriceHistory(id)
        return ResponseEntity.ok(history)
    }
}
