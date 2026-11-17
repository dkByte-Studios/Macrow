package com.macrow.app.data

import com.macrow.app.domain.LabelPrefill
import com.macrow.app.domain.LabelValues
import org.json.JSONException
import org.json.JSONObject

/** Hands a scanned label to the Create food screen as a small piece of text in the navigation address. */
fun LabelPrefill.toJson(): String = JSONObject().apply {
    put("l", servingLabel)
    put("g", servingGrams)
    put("x", guessed)
    values.kcal?.let { put("kcal", it) }
    values.protein?.let { put("p", it) }
    values.carbs?.let { put("c", it) }
    values.fat?.let { put("f", it) }
    values.fiber?.let { put("fi", it) }
    values.sugar?.let { put("su", it) }
    values.sodiumMg?.let { put("na", it) }
    values.ironMg?.let { put("fe", it) }
    values.calciumMg?.let { put("ca", it) }
    values.vitCMg?.let { put("vc", it) }
    values.potassiumMg?.let { put("k", it) }
}.toString()

/** Reads a prefill back, or null when the text is empty or not ours. Numbers are clamped like typed ones. */
fun parseLabelPrefill(json: String?): LabelPrefill? {
    if (json.isNullOrBlank()) return null
    return try {
        val o = JSONObject(json)
        fun d(k: String, max: Double): Double? = if (o.has(k)) o.getDouble(k).takeIf { !it.isNaN() && it in 0.0..max } else null
        LabelPrefill(
            servingLabel = o.optString("l", "1 serving").take(30).ifBlank { "1 serving" },
            servingGrams = (d("g", 20000.0) ?: 100.0).coerceAtLeast(1.0),
            guessed = o.optBoolean("x"),
            values = LabelValues(
                kcal = d("kcal", 20000.0), protein = d("p", 5000.0), carbs = d("c", 5000.0), fat = d("f", 5000.0), fiber = d("fi", 5000.0),
                sugar = d("su", 5000.0), sodiumMg = d("na", 1e6), ironMg = d("fe", 1e6), calciumMg = d("ca", 1e6), vitCMg = d("vc", 1e6), potassiumMg = d("k", 1e6),
            ),
        )
    } catch (e: JSONException) {
        null
    }
}
