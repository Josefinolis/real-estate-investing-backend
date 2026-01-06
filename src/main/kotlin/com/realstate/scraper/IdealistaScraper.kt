package com.realstate.scraper

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.Property
import com.realstate.domain.entity.PropertySource
import com.realstate.domain.entity.PropertyType
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class IdealistaScraper(
    rateLimiter: RateLimiter
) : BaseScraper(rateLimiter) {

    override val source = PropertySource.IDEALISTA
    override val baseUrl = "https://www.idealista.com"

    // All Spanish province capitals - Format: city-province for Idealista
    private val cityPairs = listOf(
        "madrid-madrid", "barcelona-barcelona", "valencia-valencia", "sevilla-sevilla",
        "zaragoza-zaragoza", "malaga-malaga", "murcia-murcia", "palma-de-mallorca-balears-illes",
        "las-palmas-de-gran-canaria-las-palmas", "bilbao-vizcaya", "alicante-alacant-alicante",
        "cordoba-cordoba", "valladolid-valladolid", "vigo-pontevedra", "gijon-asturias",
        "vitoria-gasteiz-alava", "a-coruna-a-coruna", "granada-granada", "elche-elx-alicante",
        "oviedo-asturias", "santa-cruz-de-tenerife-santa-cruz-de-tenerife",
        "pamplona-iruna-navarra", "almeria-almeria", "donostia-san-sebastian-guipuzcoa",
        "santander-cantabria", "burgos-burgos", "albacete-albacete", "castellon-de-la-plana-castello-de-la-plana-castellon",
        "logrono-la-rioja", "badajoz-badajoz", "salamanca-salamanca", "huelva-huelva",
        "lleida-lleida", "tarragona-tarragona", "leon-leon", "cadiz-cadiz", "jaen-jaen",
        "ourense-ourense", "lugo-lugo", "girona-girona", "caceres-caceres", "guadalajara-guadalajara",
        "toledo-toledo", "pontevedra-pontevedra", "palencia-palencia", "ciudad-real-ciudad-real",
        "zamora-zamora", "avila-avila", "cuenca-cuenca", "huesca-huesca", "segovia-segovia",
        "soria-soria", "teruel-teruel", "ceuta-ceuta", "melilla-melilla"
    )

    private val searchUrls = cityPairs.flatMap { cityPair ->
        listOf(
            "$baseUrl/venta-viviendas/$cityPair/",
            "$baseUrl/alquiler-viviendas/$cityPair/"
        )
    }

    override fun scrape(): List<Property> {
        val properties = mutableListOf<Property>()

        for (searchUrl in searchUrls) {
            try {
                logger.info("Scraping Idealista: $searchUrl")
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

        val items = document.select("article.item")

        for (item in items) {
            try {
                val property = parsePropertyItem(item, operationType, city)
                if (property != null) {
                    properties.add(property)
                }
            } catch (e: Exception) {
                logger.warn("Error parsing property item: ${e.message}")
            }
        }

        return properties
    }

    private fun parsePropertyItem(item: Element, operationType: OperationType, city: String): Property? {
        val linkElement = item.selectFirst("a.item-link") ?: return null
        val href = linkElement.attr("href")
        val externalId = extractIdFromUrl(href) ?: return null

        val title = item.selectFirst("a.item-link")?.text()?.trim()
        val priceText = item.selectFirst(".item-price")?.text()
        val price = extractPrice(priceText)

        val detailItems = item.select(".item-detail")
        var rooms: Int? = null
        var area: java.math.BigDecimal? = null
        var bathrooms: Int? = null

        for (detail in detailItems) {
            val text = detail.text().lowercase()
            when {
                text.contains("hab") -> rooms = extractNumber(text)
                text.contains("m²") || text.contains("m2") -> area = extractArea(text)
                text.contains("baño") -> bathrooms = extractNumber(text)
            }
        }

        val address = item.selectFirst(".item-detail-char .item-link")?.text()?.trim()
        val zone = item.selectFirst(".item-detail-char .item-link + span")?.text()?.trim()
        val postalCode = extractPostalCodeFromAddress(address) ?: extractPostalCodeFromTitle(title)

        val imageUrl = item.selectFirst("img.item-gallery")?.attr("src")
        val imageUrls = if (imageUrl != null) arrayOf(imageUrl) else null

        val propertyType = inferPropertyType(title)

        return Property(
            externalId = externalId,
            source = PropertySource.IDEALISTA,
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
        val regex = Regex("/inmueble/(\\d+)/")
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun extractCityFromUrl(url: String): String {
        val cityMappings = mapOf(
            "madrid" to "Madrid", "barcelona" to "Barcelona", "valencia" to "Valencia", "sevilla" to "Sevilla",
            "zaragoza" to "Zaragoza", "malaga" to "Málaga", "murcia" to "Murcia", "palma-de-mallorca" to "Palma de Mallorca",
            "las-palmas" to "Las Palmas de Gran Canaria", "bilbao" to "Bilbao", "alicante" to "Alicante",
            "cordoba" to "Córdoba", "valladolid" to "Valladolid", "vigo" to "Vigo", "gijon" to "Gijón",
            "vitoria" to "Vitoria-Gasteiz", "a-coruna" to "A Coruña", "granada" to "Granada", "elche" to "Elche",
            "oviedo" to "Oviedo", "santa-cruz-de-tenerife" to "Santa Cruz de Tenerife",
            "pamplona" to "Pamplona", "almeria" to "Almería", "donostia" to "San Sebastián", "san-sebastian" to "San Sebastián",
            "santander" to "Santander", "burgos" to "Burgos", "albacete" to "Albacete",
            "castellon" to "Castellón de la Plana", "logrono" to "Logroño", "badajoz" to "Badajoz",
            "salamanca" to "Salamanca", "huelva" to "Huelva", "lleida" to "Lleida", "tarragona" to "Tarragona",
            "leon" to "León", "cadiz" to "Cádiz", "jaen" to "Jaén", "ourense" to "Ourense", "lugo" to "Lugo",
            "girona" to "Girona", "caceres" to "Cáceres", "guadalajara" to "Guadalajara", "toledo" to "Toledo",
            "pontevedra" to "Pontevedra", "palencia" to "Palencia", "ciudad-real" to "Ciudad Real",
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
            lowerTitle.contains("ático") || lowerTitle.contains("atico") -> PropertyType.ATICO
            lowerTitle.contains("dúplex") || lowerTitle.contains("duplex") -> PropertyType.DUPLEX
            lowerTitle.contains("estudio") -> PropertyType.ESTUDIO
            lowerTitle.contains("loft") -> PropertyType.LOFT
            else -> PropertyType.OTRO
        }
    }

    private fun extractPostalCodeFromAddress(address: String?): String? {
        if (address == null) return null
        // Look for 5-digit postal code in the address
        val regex = Regex("\\b(\\d{5})\\b")
        return regex.find(address)?.groupValues?.get(1)
    }

    private fun extractPostalCodeFromTitle(title: String?): String? {
        if (title == null) return null
        // Sometimes the postal code appears in the title
        val regex = Regex("\\b(\\d{5})\\b")
        return regex.find(title)?.groupValues?.get(1)
    }
}
