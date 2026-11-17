package com.macrow.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LookupException(message: String) : Exception(message)

private const val BASE = "https://world.openfoodfacts.org"
private const val FIELDS = "code,product_name,brands,serving_size,serving_quantity,nutriments"
private const val MAX_BYTES = 2 * 1024 * 1024

/** Reads Open Food Facts (free, open data). Only the barcode or the words you typed are sent. */
object OpenFoodFacts {
    fun isBarcode(s: String) = s.length in 8..14 && s.all { it in '0'..'9' }

    suspend fun byBarcode(code: String): Food? {
        if (!isBarcode(code)) throw LookupException("That doesn't look like a barcode.")
        val json = get("$BASE/api/v2/product/$code.json?fields=$FIELDS")
        if (json.optInt("status", 0) != 1) return null
        return parseProduct(json.optJSONObject("product") ?: return null, code)
    }

    suspend fun search(query: String): List<Food> {
        val q = URLEncoder.encode(query.trim().take(60), "UTF-8")
        val json = get("$BASE/cgi/search.pl?search_terms=$q&search_simple=1&action=process&json=1&page_size=20&fields=$FIELDS")
        val arr = json.optJSONArray("products") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { runCatching { parseProduct(arr.getJSONObject(it), null) }.getOrNull() }
    }

    private suspend fun get(url: String): JSONObject = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 8_000
            c.readTimeout = 12_000
            c.setRequestProperty("User-Agent", "Macrow/1.0 (personal Android app)")
            if (c.responseCode == 429) throw LookupException("Open Food Facts is busy. Try again in a minute.")
            if (c.responseCode !in 200..299) throw LookupException("Open Food Facts didn't answer (${c.responseCode}).")
            val out = java.io.ByteArrayOutputStream()
            c.inputStream.use { s ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = s.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > MAX_BYTES) throw LookupException("The answer was too big to read.")
                }
            }
            JSONObject(out.toString(Charsets.UTF_8.name()))
        } catch (e: LookupException) {
            throw e
        } catch (e: IOException) {
            throw LookupException("Couldn't reach Open Food Facts. Check your connection.")
        } catch (e: org.json.JSONException) {
            throw LookupException("Open Food Facts sent something unreadable.")
        } finally {
            c.disconnect()
        }
    }
}

/** Turns one Open Food Facts product into a [Food] per serving, or null when it has no name or calories. */
fun parseProduct(p: JSONObject, barcode: String?): Food? {
    val name = p.optString("product_name", "").trim()
    if (name.isEmpty()) return null
    val n = p.optJSONObject("nutriments") ?: return null
    fun per100(key: String): Double? = n.optDouble(key, Double.NaN).takeIf { !it.isNaN() && it >= 0 }
    val kcal100 = per100("energy-kcal_100g") ?: per100("energy_100g")?.let { it / 4.184 } ?: return null
    val serving = p.optDouble("serving_quantity", Double.NaN).takeIf { !it.isNaN() && it > 0 && it <= 2000 }
    val grams = serving ?: 100.0
    val label = if (serving != null) p.optString("serving_size", "").trim().take(30).ifEmpty { "1 serving" } else "100 g"
    val k = grams / 100.0
    fun v(key: String, mult: Double = 1.0) = (per100(key) ?: 0.0) * mult * k
    val code = barcode ?: p.optString("code", "").takeIf { OpenFoodFacts.isBarcode(it) }
    return Food(
        name = name.take(60),
        brand = p.optString("brands", "").substringBefore(',').trim().take(40),
        servingLabel = label,
        servingGrams = grams,
        kcal = kcal100 * k,
        protein = v("proteins_100g"),
        carbs = v("carbohydrates_100g"),
        fat = v("fat_100g"),
        fiber = v("fiber_100g"),
        sugar = v("sugars_100g"),
        sodiumMg = v("sodium_100g", 1000.0),
        ironMg = v("iron_100g", 1000.0),
        calciumMg = v("calcium_100g", 1000.0),
        vitCMg = v("vitamin-c_100g", 1000.0),
        potassiumMg = v("potassium_100g", 1000.0),
        barcode = code,
        source = Source.ONLINE,
    )
}
