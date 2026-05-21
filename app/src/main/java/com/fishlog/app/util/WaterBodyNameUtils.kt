package com.fishlog.app.util

import com.fishlog.app.data.FishingTrip

object WaterBodyNameUtils {

    /**
     * Normalizes a water body name for comparison.
     * Trims, lowercases, collapses spaces, and removes common punctuation.
     */
    fun normalize(name: String): String {
        return name.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9 ]"), "")
    }

    /**
     * Checks if two names are similar based on normalized forms.
     */
    fun areSimilar(a: String, b: String): Boolean {
        val normA = normalize(a)
        val normB = normalize(b)
        if (normA == normB) return true
        if (normA.isEmpty() || normB.isEmpty()) return false

        // Simple containment or overlap logic
        return normA.contains(normB) || normB.contains(normA)
    }

    /**
     * Finds the best existing suggestion for a given input.
     * Returns null if no similar name is found or if an exact match exists.
     */
    fun findBestSuggestion(input: String, existingNames: List<String>): String? {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) return null

        val normInput = normalize(trimmedInput)
        
        // If an exact match (case-insensitive) exists, no suggestion needed
        if (existingNames.any { it.trim().equals(trimmedInput, ignoreCase = true) }) {
            return null
        }

        // 1. Look for exact normalized match (different case or punctuation)
        val exactNormMatch = existingNames.find { normalize(it) == normInput }
        if (exactNormMatch != null) return exactNormMatch

        // 2. Look for fuzzy matches (containment)
        // Sort by length difference to find the closest containment
        return existingNames
            .filter { areSimilar(it, trimmedInput) }
            .minByOrNull { Math.abs(it.length - trimmedInput.length) }
    }

    /**
     * Extracts a unique, clean list of water body names from trips.
     * Prefers the most commonly used casing/version of a name.
     */
    fun getUniqueWaterBodies(trips: List<FishingTrip>): List<String> {
        return trips.map { it.waterBody.trim() }
            .filter { it.isNotBlank() }
            .groupBy { normalize(it) }
            .map { (_, group) ->
                // Choose the most frequent version, or just the first if tied
                group.groupingBy { it }.eachCount().maxBy { it.value }.key
            }
            .sortedBy { it.lowercase() }
    }
}
