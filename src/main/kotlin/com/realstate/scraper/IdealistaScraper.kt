package com.realstate.scraper

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.Property
import com.realstate.domain.entity.PropertySource
import com.realstate.domain.entity.PropertyType
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

/**
 * Scraper para Idealista.com
 *
 * IMPORTANTE: Este scraper está DESHABILITADO por defecto debido a que Idealista
 * utiliza DataDome, un servicio de protección anti-bot comercial que bloquea
 * navegadores automatizados incluyendo Playwright.
 *
 * Opciones para usar Idealista:
 * 1. API oficial de Idealista (requiere solicitud empresarial)
 * 2. Servicios de scraping profesional (ScraperAPI, Bright Data, etc.)
 * 3. Proxies residenciales rotativos
 *
 * El código se mantiene por si se implementa alguna de estas soluciones en el futuro.
 */
@Component
class IdealistaScraper(
    rateLimiter: RateLimiter,
    private val browserManager: PlaywrightBrowserManager
) : BaseScraper(rateLimiter) {

    override val source = PropertySource.IDEALISTA
    override val baseUrl = "https://www.idealista.com"

    private val waitForSelector = "article.item, .item-info-container, .listing-items"

    // Data class for location info (city/municipality -> slug, province)
    data class LocationInfo(val slug: String, val province: String)

    // Mapping from display name to URL slug and province
    // Includes both province capitals and municipalities
    private val locationToInfo = mapOf(
        // Province capitals (city name = province name in most cases)
        "Madrid" to LocationInfo("madrid-madrid", "Madrid"),
        "Barcelona" to LocationInfo("barcelona-barcelona", "Barcelona"),
        "Valencia" to LocationInfo("valencia-valencia", "Valencia"),
        "Sevilla" to LocationInfo("sevilla-sevilla", "Sevilla"),
        "Zaragoza" to LocationInfo("zaragoza-zaragoza", "Zaragoza"),
        "Málaga" to LocationInfo("malaga-malaga", "Málaga"),
        "Murcia" to LocationInfo("murcia-murcia", "Murcia"),
        "Palma de Mallorca" to LocationInfo("palma-de-mallorca-balears-illes", "Illes Balears"),
        "Las Palmas de Gran Canaria" to LocationInfo("las-palmas-de-gran-canaria-las-palmas", "Las Palmas"),
        "Bilbao" to LocationInfo("bilbao-vizcaya", "Vizcaya"),
        "Alicante" to LocationInfo("alicante-alacant-alicante", "Alicante"),
        "Córdoba" to LocationInfo("cordoba-cordoba", "Córdoba"),
        "Valladolid" to LocationInfo("valladolid-valladolid", "Valladolid"),
        "Vigo" to LocationInfo("vigo-pontevedra", "Pontevedra"),
        "Gijón" to LocationInfo("gijon-asturias", "Asturias"),
        "Vitoria-Gasteiz" to LocationInfo("vitoria-gasteiz-alava", "Álava"),
        "A Coruña" to LocationInfo("a-coruna-a-coruna", "A Coruña"),
        "Granada" to LocationInfo("granada-granada", "Granada"),
        "Elche" to LocationInfo("elche-elx-alicante", "Alicante"),
        "Oviedo" to LocationInfo("oviedo-asturias", "Asturias"),
        "Santa Cruz de Tenerife" to LocationInfo("santa-cruz-de-tenerife-santa-cruz-de-tenerife", "Santa Cruz de Tenerife"),
        "Pamplona" to LocationInfo("pamplona-iruna-navarra", "Navarra"),
        "Almería" to LocationInfo("almeria-almeria", "Almería"),
        "San Sebastián" to LocationInfo("donostia-san-sebastian-guipuzcoa", "Guipúzcoa"),
        "Santander" to LocationInfo("santander-cantabria", "Cantabria"),
        "Burgos" to LocationInfo("burgos-burgos", "Burgos"),
        "Albacete" to LocationInfo("albacete-albacete", "Albacete"),
        "Castellón de la Plana" to LocationInfo("castellon-de-la-plana-castello-de-la-plana-castellon", "Castellón"),
        "Logroño" to LocationInfo("logrono-la-rioja", "La Rioja"),
        "Badajoz" to LocationInfo("badajoz-badajoz", "Badajoz"),
        "Salamanca" to LocationInfo("salamanca-salamanca", "Salamanca"),
        "Huelva" to LocationInfo("huelva-huelva", "Huelva"),
        "Lleida" to LocationInfo("lleida-lleida", "Lleida"),
        "Tarragona" to LocationInfo("tarragona-tarragona", "Tarragona"),
        "León" to LocationInfo("leon-leon", "León"),
        "Cádiz" to LocationInfo("cadiz-cadiz", "Cádiz"),
        "Jaén" to LocationInfo("jaen-jaen", "Jaén"),
        "Ourense" to LocationInfo("ourense-ourense", "Ourense"),
        "Lugo" to LocationInfo("lugo-lugo", "Lugo"),
        "Girona" to LocationInfo("girona-girona", "Girona"),
        "Cáceres" to LocationInfo("caceres-caceres", "Cáceres"),
        "Guadalajara" to LocationInfo("guadalajara-guadalajara", "Guadalajara"),
        "Toledo" to LocationInfo("toledo-toledo", "Toledo"),
        "Pontevedra" to LocationInfo("pontevedra-pontevedra", "Pontevedra"),
        "Palencia" to LocationInfo("palencia-palencia", "Palencia"),
        "Ciudad Real" to LocationInfo("ciudad-real-ciudad-real", "Ciudad Real"),
        "Zamora" to LocationInfo("zamora-zamora", "Zamora"),
        "Ávila" to LocationInfo("avila-avila", "Ávila"),
        "Cuenca" to LocationInfo("cuenca-cuenca", "Cuenca"),
        "Huesca" to LocationInfo("huesca-huesca", "Huesca"),
        "Segovia" to LocationInfo("segovia-segovia", "Segovia"),
        "Soria" to LocationInfo("soria-soria", "Soria"),
        "Teruel" to LocationInfo("teruel-teruel", "Teruel"),
        "Ceuta" to LocationInfo("ceuta-ceuta", "Ceuta"),
        "Melilla" to LocationInfo("melilla-melilla", "Melilla"),

        // Municipalities of Toledo province
        "Ocaña" to LocationInfo("ocana-toledo", "Toledo"),
        "Talavera de la Reina" to LocationInfo("talavera-de-la-reina-toledo", "Toledo"),
        "Illescas" to LocationInfo("illescas-toledo", "Toledo"),
        "Seseña" to LocationInfo("sesena-toledo", "Toledo"),
        "Torrijos" to LocationInfo("torrijos-toledo", "Toledo"),
        "Mora" to LocationInfo("mora-toledo", "Toledo"),
        "Consuegra" to LocationInfo("consuegra-toledo", "Toledo"),
        "Madridejos" to LocationInfo("madridejos-toledo", "Toledo"),
        "Quintanar de la Orden" to LocationInfo("quintanar-de-la-orden-toledo", "Toledo"),
        "Villacañas" to LocationInfo("villacanas-toledo", "Toledo"),
        "Sonseca" to LocationInfo("sonseca-toledo", "Toledo"),

        // Municipalities of Madrid province (outside Madrid city)
        "Alcalá de Henares" to LocationInfo("alcala-de-henares-madrid", "Madrid"),
        "Móstoles" to LocationInfo("mostoles-madrid", "Madrid"),
        "Getafe" to LocationInfo("getafe-madrid", "Madrid"),
        "Alcorcón" to LocationInfo("alcorcon-madrid", "Madrid"),
        "Leganés" to LocationInfo("leganes-madrid", "Madrid"),
        "Fuenlabrada" to LocationInfo("fuenlabrada-madrid", "Madrid"),
        "Parla" to LocationInfo("parla-madrid", "Madrid"),
        "Torrejón de Ardoz" to LocationInfo("torrejon-de-ardoz-madrid", "Madrid"),
        "Alcobendas" to LocationInfo("alcobendas-madrid", "Madrid"),
        "Las Rozas de Madrid" to LocationInfo("las-rozas-de-madrid-madrid", "Madrid"),
        "San Sebastián de los Reyes" to LocationInfo("san-sebastian-de-los-reyes-madrid", "Madrid"),
        "Pozuelo de Alarcón" to LocationInfo("pozuelo-de-alarcon-madrid", "Madrid"),
        "Coslada" to LocationInfo("coslada-madrid", "Madrid"),
        "Rivas-Vaciamadrid" to LocationInfo("rivas-vaciamadrid-madrid", "Madrid"),
        "Majadahonda" to LocationInfo("majadahonda-madrid", "Madrid"),
        "Aranjuez" to LocationInfo("aranjuez-madrid", "Madrid"),
        "Arganda del Rey" to LocationInfo("arganda-del-rey-madrid", "Madrid"),
        "Collado Villalba" to LocationInfo("collado-villalba-madrid", "Madrid"),
        "Tres Cantos" to LocationInfo("tres-cantos-madrid", "Madrid"),
        "San Fernando de Henares" to LocationInfo("san-fernando-de-henares-madrid", "Madrid"),
        "Boadilla del Monte" to LocationInfo("boadilla-del-monte-madrid", "Madrid"),
        "Pinto" to LocationInfo("pinto-madrid", "Madrid"),
        "Valdemoro" to LocationInfo("valdemoro-madrid", "Madrid"),
        "Colmenar Viejo" to LocationInfo("colmenar-viejo-madrid", "Madrid"),
        "Galapagar" to LocationInfo("galapagar-madrid", "Madrid"),
        "Villanueva de la Cañada" to LocationInfo("villanueva-de-la-canada-madrid", "Madrid"),

        // Other important municipalities
        "Marbella" to LocationInfo("marbella-malaga", "Málaga"),
        "Fuengirola" to LocationInfo("fuengirola-malaga", "Málaga"),
        "Torremolinos" to LocationInfo("torremolinos-malaga", "Málaga"),
        "Benalmádena" to LocationInfo("benalmadena-malaga", "Málaga"),
        "Estepona" to LocationInfo("estepona-malaga", "Málaga"),
        "Mijas" to LocationInfo("mijas-malaga", "Málaga"),
        "Ronda" to LocationInfo("ronda-malaga", "Málaga"),
        "Vélez-Málaga" to LocationInfo("velez-malaga-malaga", "Málaga"),
        "Torrevieja" to LocationInfo("torrevieja-alicante", "Alicante"),
        "Benidorm" to LocationInfo("benidorm-alicante", "Alicante"),
        "Orihuela" to LocationInfo("orihuela-alicante", "Alicante"),
        "Dénia" to LocationInfo("denia-alicante", "Alicante"),
        "Calpe" to LocationInfo("calpe-alicante", "Alicante"),
        "Jávea" to LocationInfo("javea-xabia-alicante", "Alicante"),
        "Villajoyosa" to LocationInfo("villajoyosa-la-vila-joiosa-alicante", "Alicante"),
        "Altea" to LocationInfo("altea-alicante", "Alicante"),
        "Santa Pola" to LocationInfo("santa-pola-alicante", "Alicante"),
        "Gandía" to LocationInfo("gandia-valencia", "Valencia"),
        "Sagunto" to LocationInfo("sagunto-sagunt-valencia", "Valencia"),
        "Paterna" to LocationInfo("paterna-valencia", "Valencia"),
        "Torrent" to LocationInfo("torrent-valencia", "Valencia"),
        "Alcoy" to LocationInfo("alcoy-alcoi-alicante", "Alicante"),
        "Cartagena" to LocationInfo("cartagena-murcia", "Murcia"),
        "Lorca" to LocationInfo("lorca-murcia", "Murcia"),
        "Molina de Segura" to LocationInfo("molina-de-segura-murcia", "Murcia"),
        "Jerez de la Frontera" to LocationInfo("jerez-de-la-frontera-cadiz", "Cádiz"),
        "Algeciras" to LocationInfo("algeciras-cadiz", "Cádiz"),
        "San Fernando" to LocationInfo("san-fernando-cadiz", "Cádiz"),
        "El Puerto de Santa María" to LocationInfo("el-puerto-de-santa-maria-cadiz", "Cádiz"),
        "Chiclana de la Frontera" to LocationInfo("chiclana-de-la-frontera-cadiz", "Cádiz"),
        "Dos Hermanas" to LocationInfo("dos-hermanas-sevilla", "Sevilla"),
        "Alcalá de Guadaíra" to LocationInfo("alcala-de-guadaira-sevilla", "Sevilla"),
        "Utrera" to LocationInfo("utrera-sevilla", "Sevilla"),
        "Mairena del Aljarafe" to LocationInfo("mairena-del-aljarafe-sevilla", "Sevilla"),
        "Hospitalet de Llobregat" to LocationInfo("hospitalet-de-llobregat-l-barcelona", "Barcelona"),
        "Badalona" to LocationInfo("badalona-barcelona", "Barcelona"),
        "Terrassa" to LocationInfo("terrassa-barcelona", "Barcelona"),
        "Sabadell" to LocationInfo("sabadell-barcelona", "Barcelona"),
        "Mataró" to LocationInfo("mataro-barcelona", "Barcelona"),
        "Sitges" to LocationInfo("sitges-barcelona", "Barcelona"),
        "Castelldefels" to LocationInfo("castelldefels-barcelona", "Barcelona"),
        "Sant Cugat del Vallès" to LocationInfo("sant-cugat-del-valles-barcelona", "Barcelona"),
        "Vic" to LocationInfo("vic-barcelona", "Barcelona"),
        "Manresa" to LocationInfo("manresa-barcelona", "Barcelona"),
        "Rubí" to LocationInfo("rubi-barcelona", "Barcelona"),
        "Viladecans" to LocationInfo("viladecans-barcelona", "Barcelona"),
        "El Prat de Llobregat" to LocationInfo("el-prat-de-llobregat-barcelona", "Barcelona"),
        "Granollers" to LocationInfo("granollers-barcelona", "Barcelona"),
        "Cerdanyola del Vallès" to LocationInfo("cerdanyola-del-valles-barcelona", "Barcelona"),
        "Santiago de Compostela" to LocationInfo("santiago-de-compostela-a-coruna", "A Coruña"),
        "Ferrol" to LocationInfo("ferrol-a-coruna", "A Coruña"),
        "Ponferrada" to LocationInfo("ponferrada-leon", "León"),
        "San Vicente del Raspeig" to LocationInfo("sant-vicent-del-raspeig-san-vicente-del-raspeig-alicante", "Alicante"),
        "Reus" to LocationInfo("reus-tarragona", "Tarragona"),
        "Salou" to LocationInfo("salou-tarragona", "Tarragona"),
        "Cambrils" to LocationInfo("cambrils-tarragona", "Tarragona"),
        "Lloret de Mar" to LocationInfo("lloret-de-mar-girona", "Girona"),
        "Blanes" to LocationInfo("blanes-girona", "Girona"),
        "Figueres" to LocationInfo("figueres-girona", "Girona"),
        "Roses" to LocationInfo("roses-girona", "Girona"),
        "Avilés" to LocationInfo("aviles-asturias", "Asturias"),
        "Mieres" to LocationInfo("mieres-asturias", "Asturias"),
        "Barakaldo" to LocationInfo("barakaldo-vizcaya", "Vizcaya"),
        "Getxo" to LocationInfo("getxo-vizcaya", "Vizcaya"),
        "Portugalete" to LocationInfo("portugalete-vizcaya", "Vizcaya"),
        "Irún" to LocationInfo("irun-guipuzcoa", "Guipúzcoa"),
        "Eibar" to LocationInfo("eibar-guipuzcoa", "Guipúzcoa"),
        "Zarautz" to LocationInfo("zarautz-guipuzcoa", "Guipúzcoa"),
        "Torrelavega" to LocationInfo("torrelavega-cantabria", "Cantabria"),
        "Castro Urdiales" to LocationInfo("castro-urdiales-cantabria", "Cantabria"),
        "Laredo" to LocationInfo("laredo-cantabria", "Cantabria"),
        "San Cristóbal de La Laguna" to LocationInfo("san-cristobal-de-la-laguna-santa-cruz-de-tenerife", "Santa Cruz de Tenerife"),
        "Arona" to LocationInfo("arona-santa-cruz-de-tenerife", "Santa Cruz de Tenerife"),
        "Adeje" to LocationInfo("adeje-santa-cruz-de-tenerife", "Santa Cruz de Tenerife"),
        "Arrecife" to LocationInfo("arrecife-las-palmas", "Las Palmas"),
        "Telde" to LocationInfo("telde-las-palmas", "Las Palmas"),
        "San Bartolomé de Tirajana" to LocationInfo("san-bartolome-de-tirajana-las-palmas", "Las Palmas")
    )

    // For backwards compatibility, create a simple cityToSlug map
    private val cityToSlug: Map<String, String> = locationToInfo.mapValues { it.value.slug }

    private val searchUrls = locationToInfo.values.map { it.slug }.distinct().flatMap { slug ->
        listOf(
            "$baseUrl/venta-viviendas/$slug/",
            "$baseUrl/alquiler-viviendas/$slug/"
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

    override fun scrapeWithConfig(cities: List<String>, operationTypes: List<String>): List<Property> {
        val properties = mutableListOf<Property>()

        // Build URLs dynamically based on cities and operation types
        val dynamicUrls = cities.flatMap { city ->
            val slug = cityToSlug[city]
            if (slug == null) {
                logger.warn("Unknown city for Idealista: $city")
                emptyList()
            } else {
                operationTypes.mapNotNull { opType ->
                    when (opType.uppercase()) {
                        "VENTA" -> "$baseUrl/venta-viviendas/$slug/"
                        "ALQUILER" -> "$baseUrl/alquiler-viviendas/$slug/"
                        else -> {
                            logger.warn("Unknown operation type: $opType")
                            null
                        }
                    }
                }
            }
        }

        logger.info("Idealista: scraping ${dynamicUrls.size} URLs for ${cities.size} cities and ${operationTypes.size} operation types")

        for (searchUrl in dynamicUrls) {
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
        // Use Playwright for Idealista (requires JavaScript rendering)
        rateLimiter.acquire()
        val document = browserManager.fetchPageWithRetry(url, waitForSelector) ?: run {
            logger.warn("Failed to fetch page with Playwright: $url")
            return emptyList()
        }

        val properties = mutableListOf<Property>()
        val operationType = if (url.contains("alquiler")) OperationType.ALQUILER else OperationType.VENTA
        val locationData = extractLocationFromUrl(url)

        // Try multiple selectors as Idealista may change their HTML structure
        val items = document.select("article.item").ifEmpty {
            document.select(".item-info-container").ifEmpty {
                document.select("[data-element-id]")
            }
        }

        logger.info("Found ${items.size} property items on page: $url")

        for (item in items) {
            try {
                val property = parsePropertyItem(item, operationType, locationData.first, locationData.second)
                if (property != null) {
                    properties.add(property)
                }
            } catch (e: Exception) {
                logger.warn("Error parsing property item: ${e.message}")
            }
        }

        return properties
    }

    private fun parsePropertyItem(item: Element, operationType: OperationType, city: String, province: String?): Property? {
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
            province = province,
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

    /**
     * Extracts city/municipality name and province from URL
     * @return Pair of (city/municipality name, province name)
     */
    private fun extractLocationFromUrl(url: String): Pair<String, String?> {
        val urlLower = url.lowercase()

        // Try to find a matching location from our locationToInfo map
        for ((locationName, info) in locationToInfo) {
            if (urlLower.contains(info.slug)) {
                return Pair(locationName, info.province)
            }
        }

        // Fallback: try to extract province from URL pattern (e.g., "ocana-toledo" -> Toledo)
        val provincePatterns = listOf(
            "toledo", "madrid", "barcelona", "valencia", "sevilla", "malaga", "alicante",
            "murcia", "cadiz", "cordoba", "granada", "jaen", "huelva", "almeria",
            "zaragoza", "huesca", "teruel", "vizcaya", "guipuzcoa", "alava",
            "cantabria", "asturias", "la-rioja", "navarra", "leon", "salamanca",
            "zamora", "valladolid", "palencia", "burgos", "segovia", "avila", "soria",
            "a-coruna", "lugo", "ourense", "pontevedra", "caceres", "badajoz",
            "ciudad-real", "cuenca", "guadalajara", "albacete", "lleida", "girona",
            "tarragona", "castellon", "balears-illes", "las-palmas", "santa-cruz-de-tenerife"
        )

        for (province in provincePatterns) {
            if (urlLower.contains("-$province/")) {
                // Extract city name from URL
                val regex = Regex("/(?:venta|alquiler)-viviendas/([^-]+)-$province/")
                val match = regex.find(urlLower)
                if (match != null) {
                    val citySlug = match.groupValues[1]
                    val cityName = citySlug.replace("-", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    val provinceName = province.replace("-", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    return Pair(cityName, provinceName)
                }
            }
        }

        return Pair("España", null)
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
