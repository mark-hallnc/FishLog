package com.fishlog.app.billing

enum class PaidFeature {
    ADVANCED_ANALYTICS,
    CLOUD_BACKUP
}

data class FeatureAccess(
    val isEnabled: Boolean,
    val isLocked: Boolean,
    val label: String
)

object FeatureGate {
    /**
     * TODO: Replace with real entitlement check from Google Play Billing or chosen provider.
     * For now, we return true to allow development and testing of the placeholder screens.
     */
    fun isFeatureUnlocked(feature: PaidFeature): Boolean {
        return true
    }

    fun paidLabel(feature: PaidFeature): String {
        return "Planned Premium"
    }
}
