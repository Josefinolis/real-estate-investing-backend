package com.realstate.dto

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.PropertyType
import java.math.BigDecimal

data class SearchFilterDTO(
    val city: String? = null,
    val province: String? = null,
    val postalCode: String? = null,
    val zones: List<String>? = null,
    val operationType: OperationType? = null,
    val propertyType: PropertyType? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val minRooms: Int? = null,
    val maxRooms: Int? = null,
    val minBathrooms: Int? = null,
    val minArea: BigDecimal? = null,
    val maxArea: BigDecimal? = null,
    val page: Int = 0,
    val size: Int = 20
)
