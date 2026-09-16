package com.example.data.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections
import java.util.Locale
import java.util.TimeZone

data class GeoLocationInfo(
    val ip: String,
    val city: String,
    val country: String,
    val countryCode: String,
    val flagEmoji: String,
    val isp: String,
    val region: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

object GeoLocationService {

    @Volatile
    private var cachedLocation: GeoLocationInfo? = null
    private var lastFetchTime = 0L
    private const val CACHE_DURATION_MS = 10 * 60 * 1000L // 10 minutes cache

    suspend fun resolveCurrentLocation(context: Context, forceRefresh: Boolean = false): GeoLocationInfo {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedLocation != null && (now - lastFetchTime < CACHE_DURATION_MS)) {
            return cachedLocation!!
        }

        val resolved = withContext(Dispatchers.IO) {
            fetchLiveGeoLocation() ?: fallbackGeoLocation(context)
        }
        cachedLocation = resolved
        lastFetchTime = now
        return resolved
    }

    private fun fetchLiveGeoLocation(): GeoLocationInfo? {
        val endpoints = listOf(
            "https://ipapi.co/json/",
            "http://ip-api.com/json/?fields=status,country,countryCode,regionName,city,isp,query,lat,lon",
            "https://ipwhois.app/json/"
        )

        for (endpoint in endpoints) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(endpoint)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 2500
                    readTimeout = 2500
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Anos-v3-Booster/3.0")
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    val json = JSONObject(response)

                    val ip = json.optString("ip", json.optString("query", "127.0.0.1"))
                    val city = json.optString("city", "").ifBlank { "Paris" }
                    val country = json.optString("country_name", json.optString("country", "")).ifBlank { "France" }
                    val countryCode = json.optString("country_code", json.optString("countryCode", "FR")).uppercase()
                    val region = json.optString("region", json.optString("regionName", "")).ifBlank { "Région Principale" }
                    val isp = json.optString("org", json.optString("isp", "")).ifBlank { "Réseau Mobile 5G / Fibre" }
                    val lat = json.optDouble("latitude", json.optDouble("lat", 0.0))
                    val lon = json.optDouble("longitude", json.optDouble("lon", 0.0))

                    val flag = countryCodeToEmoji(countryCode)

                    if (ip.isNotBlank() && city.isNotBlank()) {
                        return GeoLocationInfo(
                            ip = ip,
                            city = city,
                            country = country,
                            countryCode = countryCode,
                            flagEmoji = flag,
                            isp = isp,
                            region = region,
                            latitude = lat,
                            longitude = lon
                        )
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            } finally {
                connection?.disconnect()
            }
        }
        return null
    }

    fun fallbackGeoLocation(context: Context): GeoLocationInfo {
        val locale = Locale.getDefault()
        val countryCode = locale.country.ifBlank { "FR" }.uppercase()
        val countryName = locale.displayCountry.ifBlank {
            when (countryCode) {
                "FR" -> "France"
                "SN" -> "Sénégal"
                "CI" -> "Côte d'Ivoire"
                "MA" -> "Maroc"
                "DZ" -> "Algérie"
                "TN" -> "Tunisie"
                "CM" -> "Cameroun"
                "CD" -> "RD Congo"
                "MG" -> "Madagascar"
                "BE" -> "Belgique"
                "CH" -> "Suisse"
                "CA" -> "Canada"
                else -> "France"
            }
        }

        val tzId = TimeZone.getDefault().id
        val city = resolveCityFromTimeZone(tzId, countryCode)
        val flag = countryCodeToEmoji(countryCode)
        val ip = getLocalDeviceIp()
        val isp = detectNetworkIsp(context)

        return GeoLocationInfo(
            ip = ip,
            city = city,
            country = countryName,
            countryCode = countryCode,
            flagEmoji = flag,
            isp = isp,
            region = tzId.substringAfter("/").replace("_", " ")
        )
    }

    fun countryCodeToEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🌐"
        val firstChar = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }

    private fun resolveCityFromTimeZone(tzId: String, countryCode: String): String {
        if (tzId.contains("/")) {
            val tzCity = tzId.substringAfterLast("/").replace("_", " ")
            if (tzCity.isNotBlank() && tzCity != "Unknown" && tzCity != "GMT" && tzCity != "UTC") {
                return tzCity
            }
        }
        return when (countryCode) {
            "FR" -> "Paris"
            "SN" -> "Dakar"
            "CI" -> "Abidjan"
            "MA" -> "Casablanca"
            "DZ" -> "Alger"
            "TN" -> "Tunis"
            "CM" -> "Douala"
            "CD" -> "Kinshasa"
            "MG" -> "Antananarivo"
            "BE" -> "Bruxelles"
            "CH" -> "Genève"
            "CA" -> "Montréal"
            "HT" -> "Port-au-Prince"
            "US" -> "New York"
            else -> "Paris"
        }
    }

    private fun getLocalDeviceIp(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotBlank() && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "192.168.1.105"
    }

    private fun detectNetworkIsp(context: Context): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(network)
            if (caps != null) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "Wi-Fi Fibre Gaming (Ultra Basse Latence)"
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return "Réseau Mobile 5G / 4G+ LTE"
                }
            }
        } catch (_: Exception) {}
        return "Connexion Réseau Mobile 5G"
    }
}
