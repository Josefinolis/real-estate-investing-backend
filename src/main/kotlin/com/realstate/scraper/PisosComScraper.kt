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

    // Data class for location info (city/municipality -> slug, province)
    data class LocationInfo(val slug: String, val province: String)

    // Mapping from display name to URL slug and province
    // Includes both province capitals and municipalities
    private val locationToInfo = mapOf(
        // Province capitals
        "Madrid" to LocationInfo("madrid", "Madrid"),
        "Barcelona" to LocationInfo("barcelona", "Barcelona"),
        "Valencia" to LocationInfo("valencia", "Valencia"),
        "Sevilla" to LocationInfo("sevilla", "Sevilla"),
        "Zaragoza" to LocationInfo("zaragoza", "Zaragoza"),
        "Málaga" to LocationInfo("malaga", "Málaga"),
        "Murcia" to LocationInfo("murcia", "Murcia"),
        "Palma de Mallorca" to LocationInfo("palma_de_mallorca", "Illes Balears"),
        "Las Palmas de Gran Canaria" to LocationInfo("las_palmas_de_gran_canaria", "Las Palmas"),
        "Bilbao" to LocationInfo("bilbao", "Vizcaya"),
        "Alicante" to LocationInfo("alicante", "Alicante"),
        "Córdoba" to LocationInfo("cordoba", "Córdoba"),
        "Valladolid" to LocationInfo("valladolid", "Valladolid"),
        "Vigo" to LocationInfo("vigo", "Pontevedra"),
        "Gijón" to LocationInfo("gijon", "Asturias"),
        "L'Hospitalet de Llobregat" to LocationInfo("hospitalet_de_llobregat", "Barcelona"),
        "Vitoria-Gasteiz" to LocationInfo("vitoria_gasteiz", "Álava"),
        "A Coruña" to LocationInfo("a_coruna", "A Coruña"),
        "Granada" to LocationInfo("granada", "Granada"),
        "Elche" to LocationInfo("elche", "Alicante"),
        "Oviedo" to LocationInfo("oviedo", "Asturias"),
        "Santa Cruz de Tenerife" to LocationInfo("santa_cruz_de_tenerife", "Santa Cruz de Tenerife"),
        "Badalona" to LocationInfo("badalona", "Barcelona"),
        "Cartagena" to LocationInfo("cartagena", "Murcia"),
        "Terrassa" to LocationInfo("terrassa", "Barcelona"),
        "Jerez de la Frontera" to LocationInfo("jerez_de_la_frontera", "Cádiz"),
        "Sabadell" to LocationInfo("sabadell", "Barcelona"),
        "Móstoles" to LocationInfo("mostoles", "Madrid"),
        "Alcalá de Henares" to LocationInfo("alcala_de_henares", "Madrid"),
        "Pamplona" to LocationInfo("pamplona", "Navarra"),
        "Almería" to LocationInfo("almeria", "Almería"),
        "San Sebastián" to LocationInfo("san_sebastian", "Guipúzcoa"),
        "Santander" to LocationInfo("santander", "Cantabria"),
        "Burgos" to LocationInfo("burgos", "Burgos"),
        "Albacete" to LocationInfo("albacete", "Albacete"),
        "Castellón de la Plana" to LocationInfo("castellon_de_la_plana", "Castellón"),
        "Logroño" to LocationInfo("logrono", "La Rioja"),
        "Badajoz" to LocationInfo("badajoz", "Badajoz"),
        "Salamanca" to LocationInfo("salamanca", "Salamanca"),
        "Huelva" to LocationInfo("huelva", "Huelva"),
        "Lleida" to LocationInfo("lleida", "Lleida"),
        "Tarragona" to LocationInfo("tarragona", "Tarragona"),
        "León" to LocationInfo("leon", "León"),
        "Cádiz" to LocationInfo("cadiz", "Cádiz"),
        "Jaén" to LocationInfo("jaen", "Jaén"),
        "Ourense" to LocationInfo("ourense", "Ourense"),
        "Lugo" to LocationInfo("lugo", "Lugo"),
        "Girona" to LocationInfo("girona", "Girona"),
        "Cáceres" to LocationInfo("caceres", "Cáceres"),
        "Guadalajara" to LocationInfo("guadalajara", "Guadalajara"),
        "Toledo" to LocationInfo("toledo", "Toledo"),
        "Pontevedra" to LocationInfo("pontevedra", "Pontevedra"),
        "Palencia" to LocationInfo("palencia", "Palencia"),
        "Ciudad Real" to LocationInfo("ciudad_real", "Ciudad Real"),
        "Zamora" to LocationInfo("zamora", "Zamora"),
        "Ávila" to LocationInfo("avila", "Ávila"),
        "Cuenca" to LocationInfo("cuenca", "Cuenca"),
        "Huesca" to LocationInfo("huesca", "Huesca"),
        "Segovia" to LocationInfo("segovia", "Segovia"),
        "Soria" to LocationInfo("soria", "Soria"),
        "Teruel" to LocationInfo("teruel", "Teruel"),
        "Ceuta" to LocationInfo("ceuta", "Ceuta"),
        "Melilla" to LocationInfo("melilla", "Melilla"),

        // Municipalities of Toledo province
        "Ocaña" to LocationInfo("ocana", "Toledo"),
        "Talavera de la Reina" to LocationInfo("talavera_de_la_reina", "Toledo"),
        "Illescas" to LocationInfo("illescas", "Toledo"),
        "Seseña" to LocationInfo("sesena", "Toledo"),
        "Torrijos" to LocationInfo("torrijos", "Toledo"),
        "Mora" to LocationInfo("mora", "Toledo"),
        "Consuegra" to LocationInfo("consuegra", "Toledo"),
        "Madridejos" to LocationInfo("madridejos", "Toledo"),
        "Quintanar de la Orden" to LocationInfo("quintanar_de_la_orden", "Toledo"),
        "Villacañas" to LocationInfo("villacanas", "Toledo"),
        "Sonseca" to LocationInfo("sonseca", "Toledo"),

        // Municipalities of Madrid province
        "Getafe" to LocationInfo("getafe", "Madrid"),
        "Alcorcón" to LocationInfo("alcorcon", "Madrid"),
        "Leganés" to LocationInfo("leganes", "Madrid"),
        "Fuenlabrada" to LocationInfo("fuenlabrada", "Madrid"),
        "Parla" to LocationInfo("parla", "Madrid"),
        "Torrejón de Ardoz" to LocationInfo("torrejon_de_ardoz", "Madrid"),
        "Alcobendas" to LocationInfo("alcobendas", "Madrid"),
        "Las Rozas de Madrid" to LocationInfo("las_rozas", "Madrid"),
        "San Sebastián de los Reyes" to LocationInfo("san_sebastian_de_los_reyes", "Madrid"),
        "Pozuelo de Alarcón" to LocationInfo("pozuelo_de_alarcon", "Madrid"),
        "Coslada" to LocationInfo("coslada", "Madrid"),
        "Rivas-Vaciamadrid" to LocationInfo("rivas_vaciamadrid", "Madrid"),
        "Majadahonda" to LocationInfo("majadahonda", "Madrid"),
        "Aranjuez" to LocationInfo("aranjuez", "Madrid"),
        "Arganda del Rey" to LocationInfo("arganda_del_rey", "Madrid"),
        "Collado Villalba" to LocationInfo("collado_villalba", "Madrid"),
        "Tres Cantos" to LocationInfo("tres_cantos", "Madrid"),
        "San Fernando de Henares" to LocationInfo("san_fernando_de_henares", "Madrid"),
        "Boadilla del Monte" to LocationInfo("boadilla_del_monte", "Madrid"),
        "Pinto" to LocationInfo("pinto", "Madrid"),
        "Valdemoro" to LocationInfo("valdemoro", "Madrid"),
        "Colmenar Viejo" to LocationInfo("colmenar_viejo", "Madrid"),

        // Other important municipalities
        "Marbella" to LocationInfo("marbella", "Málaga"),
        "Fuengirola" to LocationInfo("fuengirola", "Málaga"),
        "Torremolinos" to LocationInfo("torremolinos", "Málaga"),
        "Benalmádena" to LocationInfo("benalmadena", "Málaga"),
        "Estepona" to LocationInfo("estepona", "Málaga"),
        "Mijas" to LocationInfo("mijas", "Málaga"),
        "Ronda" to LocationInfo("ronda", "Málaga"),
        "Vélez-Málaga" to LocationInfo("velez_malaga", "Málaga"),
        "Torrevieja" to LocationInfo("torrevieja", "Alicante"),
        "Benidorm" to LocationInfo("benidorm", "Alicante"),
        "Orihuela" to LocationInfo("orihuela", "Alicante"),
        "Dénia" to LocationInfo("denia", "Alicante"),
        "Gandía" to LocationInfo("gandia", "Valencia"),
        "Sagunto" to LocationInfo("sagunto", "Valencia"),
        "Paterna" to LocationInfo("paterna", "Valencia"),
        "Torrent" to LocationInfo("torrent", "Valencia"),
        "Lorca" to LocationInfo("lorca", "Murcia"),
        "Molina de Segura" to LocationInfo("molina_de_segura", "Murcia"),
        "Algeciras" to LocationInfo("algeciras", "Cádiz"),
        "San Fernando" to LocationInfo("san_fernando", "Cádiz"),
        "El Puerto de Santa María" to LocationInfo("el_puerto_de_santa_maria", "Cádiz"),
        "Chiclana de la Frontera" to LocationInfo("chiclana_de_la_frontera", "Cádiz"),
        "Dos Hermanas" to LocationInfo("dos_hermanas", "Sevilla"),
        "Alcalá de Guadaíra" to LocationInfo("alcala_de_guadaira", "Sevilla"),
        "Utrera" to LocationInfo("utrera", "Sevilla"),
        "Mairena del Aljarafe" to LocationInfo("mairena_del_aljarafe", "Sevilla"),
        "Mataró" to LocationInfo("mataro", "Barcelona"),
        "Sitges" to LocationInfo("sitges", "Barcelona"),
        "Castelldefels" to LocationInfo("castelldefels", "Barcelona"),
        "Sant Cugat del Vallès" to LocationInfo("sant_cugat_del_valles", "Barcelona"),
        "Vic" to LocationInfo("vic", "Barcelona"),
        "Manresa" to LocationInfo("manresa", "Barcelona"),
        "Rubí" to LocationInfo("rubi", "Barcelona"),
        "Viladecans" to LocationInfo("viladecans", "Barcelona"),
        "El Prat de Llobregat" to LocationInfo("el_prat_de_llobregat", "Barcelona"),
        "Granollers" to LocationInfo("granollers", "Barcelona"),
        "Santiago de Compostela" to LocationInfo("santiago_de_compostela", "A Coruña"),
        "Ferrol" to LocationInfo("ferrol", "A Coruña"),
        "Ponferrada" to LocationInfo("ponferrada", "León"),
        "Reus" to LocationInfo("reus", "Tarragona"),
        "Salou" to LocationInfo("salou", "Tarragona"),
        "Cambrils" to LocationInfo("cambrils", "Tarragona"),
        "Lloret de Mar" to LocationInfo("lloret_de_mar", "Girona"),
        "Blanes" to LocationInfo("blanes", "Girona"),
        "Figueres" to LocationInfo("figueres", "Girona"),
        "Roses" to LocationInfo("roses", "Girona"),
        "Avilés" to LocationInfo("aviles", "Asturias"),
        "Mieres" to LocationInfo("mieres", "Asturias"),
        "Barakaldo" to LocationInfo("barakaldo", "Vizcaya"),
        "Getxo" to LocationInfo("getxo", "Vizcaya"),
        "Portugalete" to LocationInfo("portugalete", "Vizcaya"),
        "Irún" to LocationInfo("irun", "Guipúzcoa"),
        "Eibar" to LocationInfo("eibar", "Guipúzcoa"),
        "Zarautz" to LocationInfo("zarautz", "Guipúzcoa"),
        "Torrelavega" to LocationInfo("torrelavega", "Cantabria"),
        "Castro Urdiales" to LocationInfo("castro_urdiales", "Cantabria"),
        "Laredo" to LocationInfo("laredo", "Cantabria"),
        "San Cristóbal de La Laguna" to LocationInfo("san_cristobal_de_la_laguna", "Santa Cruz de Tenerife"),
        "Arona" to LocationInfo("arona", "Santa Cruz de Tenerife"),
        "Adeje" to LocationInfo("adeje", "Santa Cruz de Tenerife"),
        "Arrecife" to LocationInfo("arrecife", "Las Palmas"),
        "Telde" to LocationInfo("telde", "Las Palmas"),
        "San Bartolomé de Tirajana" to LocationInfo("san_bartolome_de_tirajana", "Las Palmas")
    )

    // For backwards compatibility
    private val cityToSlug: Map<String, String> = locationToInfo.mapValues { it.value.slug }

    private val searchUrls = locationToInfo.values.map { it.slug }.distinct().flatMap { slug ->
        listOf(
            "$baseUrl/venta/pisos-$slug/",
            "$baseUrl/alquiler/pisos-$slug/"
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

    override fun scrapeWithConfig(cities: List<String>, operationTypes: List<String>): List<Property> {
        val properties = mutableListOf<Property>()

        // Build URLs dynamically based on cities and operation types
        val dynamicUrls = cities.flatMap { city ->
            val slug = cityToSlug[city]
            if (slug == null) {
                logger.warn("Unknown city for Pisos.com: $city")
                emptyList()
            } else {
                operationTypes.mapNotNull { opType ->
                    when (opType.uppercase()) {
                        "VENTA" -> "$baseUrl/venta/pisos-$slug/"
                        "ALQUILER" -> "$baseUrl/alquiler/pisos-$slug/"
                        else -> {
                            logger.warn("Unknown operation type: $opType")
                            null
                        }
                    }
                }
            }
        }

        logger.info("Pisos.com: scraping ${dynamicUrls.size} URLs for ${cities.size} cities and ${operationTypes.size} operation types")

        for (searchUrl in dynamicUrls) {
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
        val locationData = extractLocationFromUrl(url)

        val items = document.select(".ad-preview, .ad-list-item")

        for (item in items) {
            try {
                val property = parsePropertyItem(item, operationType, locationData.first, locationData.second)
                if (property != null) {
                    properties.add(property)
                }
            } catch (e: Exception) {
                logger.warn("Error parsing Pisos.com property: ${e.message}")
            }
        }

        return properties
    }

    private fun parsePropertyItem(item: Element, operationType: OperationType, city: String, province: String?): Property? {
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
            province = province,
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

    /**
     * Extracts city/municipality name and province from URL
     * @return Pair of (city/municipality name, province name)
     */
    private fun extractLocationFromUrl(url: String): Pair<String, String?> {
        val urlLower = url.lowercase()

        // Try to find a matching location from our locationToInfo map
        for ((locationName, info) in locationToInfo) {
            if (urlLower.contains("-${info.slug}/") || urlLower.contains("pisos-${info.slug}/")) {
                return Pair(locationName, info.province)
            }
        }

        // Fallback: try to extract city name from URL pattern
        val regex = Regex("/(?:venta|alquiler)/pisos-([^/]+)/")
        val match = regex.find(urlLower)
        if (match != null) {
            val citySlug = match.groupValues[1]
            val cityName = citySlug.replace("_", " ").replace("-", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            return Pair(cityName, null)
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
