package com.fishlog.app.analytics

data class TripReview(
    val tripId: Long,
    val tripName: String,
    val waterBody: String,
    val sections: List<TripReviewSection>,
    val confidenceLabel: String,
    val observationCount: Int,
    val generatedLocally: Boolean = true
)

data class TripReviewSection(
    val heading: String,
    val text: String,
    val type: TripReviewSectionType
)

enum class TripReviewSectionType {
    WHAT_HAPPENED,
    WHAT_WORKED,
    WHAT_DID_NOT_WORK,
    CONDITIONS,
    COMPARED_TO_HISTORY
}
