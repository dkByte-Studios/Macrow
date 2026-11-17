package com.macrow.app.domain

import kotlin.math.abs

/** One line of text the camera read, with where it sits in the picture. */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Numbers found for one column of a nutrition table. Anything not found stays null. */
data class LabelValues(
    val kcal: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodiumMg: Double? = null,
    val ironMg: Double? = null,
    val calciumMg: Double? = null,
    val vitCMg: Double? = null,
    val potassiumMg: Double? = null,
) {
    /** How many of the main numbers were found: calories, protein, carbs and fat. */
    val core: Int get() = listOfNotNull(kcal, protein, carbs, fat).size
    val isEmpty: Boolean get() = this == LabelValues()

    fun scaled(f: Double) = LabelValues(
        kcal?.times(f), protein?.times(f), carbs?.times(f), fat?.times(f), fiber?.times(f), sugar?.times(f),
        sodiumMg?.times(f), ironMg?.times(f), calciumMg?.times(f), vitCMg?.times(f), potassiumMg?.times(f),
    )
}

/**
 * What a nutrition label said. [perServing] and [per100] are the two possible columns; a label often has only one.
 * [basisGuessed] is true when the label never said which one it was, so the numbers were taken as per serving.
 */
data class LabelScan(
    val servingLabel: String?,
    val servingGrams: Double?,
    val perServing: LabelValues?,
    val per100: LabelValues?,
    val basisGuessed: Boolean = false,
) {
    val found: Boolean get() = (perServing?.core ?: 0) >= 2 || (per100?.core ?: 0) >= 2
}

/** The numbers to put in the Create food form, per the serving it names. */
data class LabelPrefill(val servingLabel: String, val servingGrams: Double, val values: LabelValues, val guessed: Boolean)

enum class LabelBasis { PER_SERVING, PER_100 }

fun LabelScan.toPrefill(basis: LabelBasis): LabelPrefill? = when (basis) {
    LabelBasis.PER_SERVING -> perServing?.let { LabelPrefill(servingLabel ?: "1 serving", servingGrams ?: 100.0, it, basisGuessed || servingGrams == null) }
    LabelBasis.PER_100 -> per100?.let { LabelPrefill("100 g", 100.0, it, false) }
}

/** Groups the lines the camera read into table rows: lines at the same height become one row, left to right. */
fun rowsFrom(lines: List<OcrLine>): List<String> {
    if (lines.isEmpty()) return emptyList()
    val sorted = lines.sortedBy { (it.top + it.bottom) / 2 }
    val rows = mutableListOf<MutableList<OcrLine>>()
    for (l in sorted) {
        val mid = (l.top + l.bottom) / 2
        val row = rows.lastOrNull()
        val rowMid = row?.let { r -> r.sumOf { (it.top + it.bottom) / 2 } / r.size }
        val h = maxOf(1, l.bottom - l.top)
        if (row != null && rowMid != null && abs(mid - rowMid) < h * 0.6) row += l else rows += mutableListOf(l)
    }
    return rows.map { r -> r.sortedBy { it.left }.joinToString(" ") { it.text.trim() } }
}

private val numberUnit = Regex("""(\d+(?:\.\d+)?)\s*(kcal|cal|kj|mcg|µg|ug|mg|g|ml|%)?""")

private fun clean(row: String): String =
    row.lowercase()
        .replace(Regex("""(?<=\d),(?=\d{1,2}(?!\d))"""), ".") // 1,5 g
        .replace(Regex("""(?<=\d)[oO](?=\s*(?:g|mg|%|\b))"""), "0") // 5O g
        .replace("µ", "u")
        .replace(Regex("""\s+"""), " ")
        .trim()

private data class Num(val value: Double, val unit: String?)

private fun numbers(text: String): List<Num> = numberUnit.findAll(text).map { Num(it.groupValues[1].toDouble(), it.groupValues[2].ifEmpty { null }) }.toList()

private val skipWords = listOf("saturated", "trans", "mono", "polyun", "cholesterol", "added", "includes", "from fat")

/**
 * Reads the numbers out of the rows of a nutrition label. Works for the US "Nutrition Facts" panel (one column per serving),
 * and for tables with a per serving column, a per 100 g column, or both. It never invents a number: whatever it cannot
 * find stays empty so you can fill it in.
 */
fun parseLabel(rawRows: List<String>): LabelScan {
    val rows = rawRows.map(::clean).filter { it.isNotEmpty() }

    // Which columns are there, and in which order?
    var order: List<LabelBasis> = emptyList()
    var guessed = false
    for (r in rows) {
        val i100 = Regex("""per\s*100\s*(g|ml)|100\s*(g|ml)\b""").find(r)?.range?.first ?: -1
        val iServ = Regex("""per\s*serving|serving""").find(r)?.range?.first ?: -1
        if (i100 >= 0 && iServ >= 0 && "serving size" !in r) { order = if (i100 < iServ) listOf(LabelBasis.PER_100, LabelBasis.PER_SERVING) else listOf(LabelBasis.PER_SERVING, LabelBasis.PER_100); break }
    }
    if (order.isEmpty()) {
        // The two headers may have been read on separate rows: use the order they appear in.
        val i100 = rows.indexOfFirst { Regex("""per\s*100\s*(g|ml)""").containsMatchIn(it) }
        val iServ = rows.indexOfFirst { ("per serving" in it || "amount per serving" in it) && "serving size" !in it }
        if (i100 >= 0 && iServ >= 0) order = if (i100 < iServ) listOf(LabelBasis.PER_100, LabelBasis.PER_SERVING) else listOf(LabelBasis.PER_SERVING, LabelBasis.PER_100)
    }
    if (order.isEmpty()) {
        val has100 = rows.any { Regex("""per\s*100\s*(g|ml)""").containsMatchIn(it) }
        val hasServ = rows.any { "per serving" in it || "amount per serving" in it || "nutrition facts" in it }
        order = when {
            has100 && !hasServ -> listOf(LabelBasis.PER_100)
            else -> listOf(LabelBasis.PER_SERVING)
        }
        if (!has100 && !hasServ) guessed = true
    }

    // Serving size.
    var servingLabel: String? = null
    var servingGrams: Double? = null
    for (r in rows) {
        val m = Regex("""serving size\s*:?\s*(.*)""").find(r) ?: continue
        val rest = m.groupValues[1].trim()
        val paren = Regex("""\((\d+(?:\.\d+)?)\s*(g|ml)\)""").find(rest)
        val plain = Regex("""(\d+(?:\.\d+)?)\s*(g|ml)\b""").find(rest)
        servingGrams = (paren ?: plain)?.groupValues?.get(1)?.toDoubleOrNull()
        val label = rest.replace(Regex("""\(\s*\d+(?:\.\d+)?\s*(g|ml)\s*\)"""), "").replace(Regex("""\s+"""), " ").trim().trimEnd(',', '.', ';')
        servingLabel = label.take(30).ifEmpty { null }
        break
    }

    val columns = order.size
    val acc = List(columns) { mutableMapOf<String, Double>() }

    fun put(key: String, nums: List<Num>, mass: (Double, String?) -> Double?) {
        // Prefer numbers with a unit; "%" is a daily value, never the amount.
        val usable = nums.filter { it.unit != "%" }
        val withUnit = usable.filter { it.unit != null }
        var pick = if (withUnit.isNotEmpty()) withUnit else usable
        // A two-column row where one amount lost its unit: "10 g" read as "109". Keep the reading that fits the serving size.
        val grams = servingGrams
        if (columns == 2 && withUnit.size == 1 && usable.size >= 2 && grams != null && usable[0].unit == null && usable[1].unit != null) {
            val a = usable[0].value
            val b = usable[1]
            val r = grams / 100.0
            val expected = if (order[0] == LabelBasis.PER_100) b.value / r else b.value * r
            val alt = if (a >= 20 && a % 10 == 9.0) (a - 9) / 10 else null
            val chosen = if (alt != null && abs(alt - expected) < abs(a - expected)) alt else a
            pick = listOf(Num(chosen, "g"), b)
        }
        for ((ci, n) in pick.take(columns).withIndex()) {
            val v = mass(n.value, n.unit) ?: continue
            if (key !in acc[ci]) acc[ci][key] = v
        }
    }

    for (r in rows) {
        if (skipWords.any { it in r } && !r.startsWith("calories") && !r.startsWith("energy")) continue
        // The calories row of a US panel also says "calories from fat": ignore that part.
        val row = r.substringBefore("calories from fat").trim()
        when {
            row.startsWith("calories") || row.startsWith("energy") || Regex("""\b(calories|energy)\b""").containsMatchIn(row) && "kcal" in row -> {
                val nums = numbers(row)
                val kcalNums = nums.filter { it.unit == "kcal" || it.unit == "cal" }
                val chosen = when {
                    kcalNums.isNotEmpty() -> kcalNums.map { it.value }
                    nums.any { it.unit == "kj" } -> nums.filter { it.unit == "kj" }.map { it.value / 4.184 }
                    else -> nums.filter { it.unit == null && it.value in 5.0..2500.0 }.map { it.value }
                }
                for ((ci, v) in chosen.take(columns).withIndex()) if ("kcal" !in acc[ci] && v in 0.0..2500.0) acc[ci]["kcal"] = v
            }
            Regex("""^(total )?fat\b""").containsMatchIn(row) -> put("fat", numbers(row.removePrefix("total ").removePrefix("fat"))) { v, u -> if (u == null || u == "g") v else null }
            "carbohydrate" in row || row.startsWith("carbs") || row.startsWith("total carb") -> put("carbs", numbers(row.substringAfter("carb").substringAfter(" ", ""))) { v, u -> if (u == null || u == "g") v else null }
            row.startsWith("protein") -> put("protein", numbers(row.removePrefix("protein"))) { v, u -> if (u == null || u == "g") v else null }
            "fiber" in row || "fibre" in row -> put("fiber", numbers(row.substringAfter("fi", ""))) { v, u -> if (u == null || u == "g") v else null }
            "sugar" in row -> put("sugar", numbers(row.substringAfter("sugar").substringAfter(" ", ""))) { v, u -> if (u == null || u == "g") v else null }
            row.startsWith("sodium") -> put("sodium", numbers(row.removePrefix("sodium"))) { v, u -> when (u) { "g" -> v * 1000; "mg", null -> v; else -> null } }
            row.startsWith("salt") -> put("salt", numbers(row.removePrefix("salt"))) { v, u -> if (u == "g" || u == null) v * 400 else null }
            row.startsWith("iron") -> put("iron", numbers(row.removePrefix("iron"))) { v, u -> when (u) { "mg" -> v; "mcg", "ug" -> v / 1000; else -> null } }
            row.startsWith("calcium") -> put("calcium", numbers(row.removePrefix("calcium"))) { v, u -> when (u) { "mg" -> v; "mcg", "ug" -> v / 1000; else -> null } }
            row.startsWith("potassium") -> put("potassium", numbers(row.removePrefix("potassium"))) { v, u -> when (u) { "mg" -> v; "g" -> v * 1000; else -> null } }
            row.startsWith("vitamin c") || row.startsWith("vit. c") || row.startsWith("vit c") -> put("vitc", numbers(row.substringAfter("c", ""))) { v, u -> when (u) { "mg" -> v; "mcg", "ug" -> v / 1000; else -> null } }
        }
    }

    fun values(m: Map<String, Double>) = LabelValues(
        kcal = m["kcal"], protein = m["protein"], carbs = m["carbs"], fat = m["fat"], fiber = m["fiber"], sugar = m["sugar"],
        sodiumMg = m["sodium"] ?: m["salt"], ironMg = m["iron"], calciumMg = m["calcium"], vitCMg = m["vitc"], potassiumMg = m["potassium"],
    ).takeUnless { it.isEmpty }

    var perServing: LabelValues? = null
    var per100: LabelValues? = null
    for ((ci, basis) in order.withIndex()) {
        val v = values(acc[ci])
        if (basis == LabelBasis.PER_SERVING) perServing = v else per100 = v
    }
    return LabelScan(servingLabel, servingGrams, perServing, per100, basisGuessed = guessed)
}
