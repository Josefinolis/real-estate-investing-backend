package com.realstate.scraper

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.Property
import com.realstate.domain.entity.PropertySource
import com.realstate.domain.entity.PropertyType
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class PisosComScraper(
    rateLimiter: RateLimiter
) : BaseScraper(rateLimiter) {

    override val source = PropertySource.PISOSCOM
    override val baseUrl = "https://www.pisos.com"

    // All Spanish province capitals
    private val cities = listOf(
        "madrid", "barcelona", "valencia", "sevilla", "zaragoza", "malaga", "murcia", "palma_de_mallorca",
        "las_palmas_de_gran_canaria", "bilbao", "alicante", "cordoba", "valladolid", "vigo", "gijon",
        "hospitalet_de_llobregat", "vitoria_gasteiz", "a_coruna", "granada", "elche", "oviedo", "santa_cruz_de_tenerife",
        "badalona", "cartagena", "terrassa", "jerez_de_la_frontera", "sabadell", "mostoles", "alcala_de_henares",
        "pamplona", "almeria", "san_sebastian", "santander", "burgos", "albacete", "castellon_de_la_plana",
        "logrono", "badajoz", "salamanca", "huelva", "lleida", "tarragona", "leon", "cadiz", "jaen",
        "ourense", "lugo", "girona", "caceres", "guadalajara", "toledo", "pontevedra", "palencia",
        "ciudad_real", "zamora", "avila", "cuenca", "huesca", "segovia", "soria", "teruel", "ceuta", "melilla"
    )

    private val searchUrls = cities.flatMap { city ->
        listOf(
            "$baseUrl/venta/pisos-$city/",
            "$baseUrl/alquiler/pisos-$city/"
        )
    }

    override fun scrape(): List<Property> {
        val properties = mutableListOf<Property>()

        for (searchUrl in searchUrls) {
            try {
                logger.info("Scraping Pisos.com: $searchUrl")
                val pageProperties = scrapeSearchPage(searchUrl)
                properties.addAll(pageProperties)
                logger.info("Found ${pageProperties.size} properties from $searchUrl")
            } catch (e: Exception) {
                logger.error("Error scraping $searchUrl: ${e.message}", e)
            }
        }

        return properties
    }

    private fun scrapeSearchPage(url: String): List<Property> {
        val document = fetchDocument(url) ?: return emptyList()
        val properties = mutableListOf<Property>()

        val operationType = if (url.contains("alquiler")) OperationType.ALQUILER else OperationType.VENTA
        val city = extractCityFromUrl(url)

        val items = document.select(".ad-preview, .ad-list-item")

        for (item in items) {
            try {
                val property = parsePropertyItem(item, operationType, city)
                if (property != null) {
                    properties.add(property)
                }
            } catch (e: Exception) {
                logger.warn("Error parsing Pisos.com property: ${e.message}")
            }
        }

        return properties
    }

    private fun parsePropertyItem(item: Element, operationType: OperationType, city: String): Property? {
        val linkElement = item.selectFirst("a.ad-preview__title, a.ad-list-item__title")
            ?: item.selectFirst("a[href*='/piso-']")
            ?: return null

        val href = linkElement.attr("href")
        val externalId = extractIdFromUrl(href) ?: return null

        val title = linkElement.text().trim()
        val priceText = item.selectFirst(".ad-preview__price, .ad-list-item__price")?.text()
        val price = extractPrice(priceText)

        val features = item.select(".ad-preview__char, .ad-list-item__char")
        var rooms: Int? = null
        var area: java.math.BigDecimal? = null
        var bathrooms: Int? = null

        for (feature in features) {
            val text = feature.text().lowercase()
            when {
                text.contains("hab") -> rooms = extractNumber(text)
                text.contains("m²") || text.contains("m2") -> area = extractArea(text)
                text.contains("baño") -> bathrooms = extractNumber(text)
            }
        }

        val address = item.selectFirst(".ad-preview__address, .ad-list-item__address")?.text()?.trim()
        val zone = item.selectFirst(".ad-preview__zone, .ad-list-item__zone")?.text()?.trim()
        val postalCode = extractPostalCodeFromUrl(href) ?: extractPostalCodeFromAddress(address)

        val imageUrl = item.selectFirst("img.ad-preview__img, img.ad-list-item__img")?.attr("src")
            ?: item.selectFirst("img")?.attr("data-src")
        val imageUrls = if (imageUrl != null) arrayOf(imageUrl) else null

        val propertyType = inferPropertyType(title)

        return Property(
            externalId = externalId,
            source = PropertySource.PISOSCOM,
            title = title,
            price = price,
            operationType = operationType,
            propertyType = propertyType,
            rooms = rooms,
            bathrooms = bathrooms,
            areaM2 = area,
            address = address,
            city = city,
            postalCode = postalCode,
            zone = zone,
            imageUrls = imageUrls,
            url = if (href.startsWith("http")) href else "$baseUrl$href"
        )
    }

    private fun extractIdFromUrl(url: String): String? {
        val regex = Regex("/piso-[^/]+-([a-z0-9]+)/")
        return regex.find(url)?.groupValues?.get(1)
            ?: Regex("(\\d+)").find(url)?.value
    }

    private fun extractCityFromUrl(url: String): String {
        val cityMappings = mapOf(
            "madrid" to "Madrid", "barcelona" to "Barcelona", "valencia" to "Valencia", "sevilla" to "Sevilla",
            "zaragoza" to "Zaragoza", "malaga" to "Málaga", "murcia" to "Murcia", "palma_de_mallorca" to "Palma de Mallorca",
            "las_palmas" to "Las Palmas de Gran Canaria", "bilbao" to "Bilbao", "alicante" to "Alicante",
            "cordoba" to "Córdoba", "valladolid" to "Valladolid", "vigo" to "Vigo", "gijon" to "Gijón",
            "hospitalet" to "L'Hospitalet de Llobregat", "vitoria" to "Vitoria-Gasteiz", "a_coruna" to "A Coruña",
            "coruña" to "A Coruña", "granada" to "Granada", "elche" to "Elche", "oviedo" to "Oviedo",
            "santa_cruz" to "Santa Cruz de Tenerife", "badalona" to "Badalona", "cartagena" to "Cartagena",
            "terrassa" to "Terrassa", "jerez" to "Jerez de la Frontera", "sabadell" to "Sabadell",
            "mostoles" to "Móstoles", "alcala_de_henares" to "Alcalá de Henares", "pamplona" to "Pamplona",
            "almeria" to "Almería", "san_sebastian" to "San Sebastián", "donostia" to "San Sebastián",
            "santander" to "Santander", "burgos" to "Burgos", "albacete" to "Albacete",
            "castellon" to "Castellón de la Plana", "logrono" to "Logroño", "badajoz" to "Badajoz",
            "salamanca" to "Salamanca", "huelva" to "Huelva", "lleida" to "Lleida", "tarragona" to "Tarragona",
            "leon" to "León", "cadiz" to "Cádiz", "jaen" to "Jaén", "ourense" to "Ourense", "lugo" to "Lugo",
            "girona" to "Girona", "caceres" to "Cáceres", "guadalajara" to "Guadalajara", "toledo" to "Toledo",
            "pontevedra" to "Pontevedra", "palencia" to "Palencia", "ciudad_real" to "Ciudad Real",
            "zamora" to "Zamora", "avila" to "Ávila", "cuenca" to "Cuenca", "huesca" to "Huesca",
            "segovia" to "Segovia", "soria" to "Soria", "teruel" to "Teruel", "ceuta" to "Ceuta", "melilla" to "Melilla"
        )
        val urlLower = url.lowercase()
        return cityMappings.entries.firstOrNull { urlLower.contains(it.key) }?.value ?: "España"
    }

    private fun inferPropertyType(title: String?): PropertyType {
        if (title == null) return PropertyType.OTRO

        val lowerTitle = title.lowercase()
        return when {
            lowerTitle.contains("piso") -> PropertyType.PISO
            lowerTitle.contains("apartamento") -> PropertyType.APARTAMENTO
            lowerTitle.contains("chalet") -> PropertyType.CHALET
            lowerTitle.contains("casa") -> PropertyType.CASA
            lowerTitle.contains("ático") -> PropertyType.ATICO
            lowerTitle.contains("dúplex") -> PropertyType.DUPLEX
            lowerTitle.contains("estudio") -> PropertyType.ESTUDIO
            else -> PropertyType.PISO
        }
    }

    private fun extractPostalCodeFromUrl(url: String): String? {
        // URLs like: /piso-sant_pere_santa_caterina_la_ribera08003-55851209181_104700/
        // The postal code (5 digits) is embedded before the ID
        val regex = Regex("(\\d{5})-[a-z0-9]+_\\d+")
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun extractPostalCodeFromAddress(address: String?): String? {
        if (address == null) return null
        // Look for 5-digit postal code in the address
        val regex = Regex("\\b(\\d{5})\\b")
        return regex.find(address)?.groupValues?.get(1)
    }
}
