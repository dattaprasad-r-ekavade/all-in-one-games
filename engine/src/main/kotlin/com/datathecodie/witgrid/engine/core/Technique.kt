package com.datathecodie.witgrid.engine.core

/**
 * Lane-defined solving rule. [id] is stable within the lane; [displayName] names it in hints.
 * Larger non-negative [difficultyRank] means harder within that game, not across games.
 */
data class Technique(val id: String, val displayName: String, val difficultyRank: Int) {
    init {
        require(id.isNotBlank()) { "Technique ID must not be blank" }
        require(displayName.isNotBlank()) { "Technique name must not be blank" }
        require(difficultyRank >= 0) { "Technique rank must not be negative" }
    }
}
