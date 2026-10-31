package com.example.allowancetracker.util

object Percentages {
    /**
     * Whole-number percentages of [values] that always add up to 100
     * (largest-remainder rounding). Returns all zeros when the total is not positive.
     */
    fun of(values: List<Long>): List<Int> {
        val total = values.sum()
        if (total <= 0L) return values.map { 0 }
        val floors = values.map { (it * 100 / total).toInt() }
        val remainders = values.map { it * 100 % total }
        val shortfall = 100 - floors.sum()
        val bumped = values.indices.sortedByDescending { remainders[it] }.take(shortfall).toSet()
        return floors.mapIndexed { i, floor -> if (i in bumped) floor + 1 else floor }
    }
}
