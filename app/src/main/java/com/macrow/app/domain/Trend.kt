package com.macrow.app.domain

/** A weigh-in: [day] is an epoch day, [kg] the weight. */
data class WeightPoint(val day: Long, val kg: Double)

/** Smooths daily weights: each point becomes the average of the weigh-ins in the [window] days up to it. */
fun weightTrend(points: List<WeightPoint>, window: Int = 7): List<WeightPoint> {
    val sorted = points.sortedBy { it.day }
    return sorted.map { p ->
        val near = sorted.filter { it.day in (p.day - window + 1)..p.day }
        WeightPoint(p.day, near.map { it.kg }.average())
    }
}

/**
 * Days in a row, counting back from [today], with something logged. Today does not break the streak
 * while it is still empty: the streak then counts back from yesterday.
 */
fun loggingStreak(loggedDays: Set<Long>, today: Long): Int {
    var d = if (today in loggedDays) today else today - 1
    var n = 0
    while (d in loggedDays) {
        n++
        d--
    }
    return n
}
