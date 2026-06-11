package nl.incedo.paywall.model

/**
 * Structured wall definition (ADM-11): the visual wall editor outputs this,
 * the gate component renders it. No arbitrary HTML/JS — content only.
 */
enum class WallType(val label: String) {
    Hard("Hard"),
    Metered("Metered"),
}

data class WallDefinition(
    val type: WallType = WallType.Metered,
    val title: String = "You've reached this month's free limit",
    val body: String = "Pro includes unlimited documents, exports and payment reminders for €10 per user / month.",
    val primaryCta: String = "Upgrade to Pro",
    val secondaryCta: String = "Compare plans",
    val darkPreview: Boolean = false,
)
