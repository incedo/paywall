package nl.incedo.paywall.model

/**
 * Structured wall definition (ADM-11): the visual wall editor outputs this,
 * the gate component renders it. No arbitrary HTML/JS — content only.
 * The four types mirror the paywall strategies of Doc 1 (PW-*).
 */
enum class WallType(val label: String) {
    Hard("Hard"),
    Metered("Metered"),
    Freemium("Freemium"),
    Dynamic("Dynamic"),
}

enum class Channel(val label: String, val short: String) {
    Web("Web", "W"),
    MobileApp("Mobile app", "M"),
    Chat("Chat", "C"),
    InProduct("In-product", "P"),
    Email("Email", "E"),
    Sms("SMS", "S"),
}

data class WallDefinition(
    val type: WallType = WallType.Metered,
    val title: String = "You've reached this month's free limit",
    val body: String = "Pro includes unlimited documents, exports and payment reminders for €10 per user / month.",
    val primaryCta: String = "Upgrade to Pro",
    val secondaryCta: String = "Compare plans",
    val channels: Set<Channel> = setOf(Channel.Web, Channel.MobileApp, Channel.Email),
    val darkPreview: Boolean = false,
)

enum class WallStatus(val label: String) {
    Live("Live"),
    Draft("Draft"),
    Paused("Paused"),
}

/** Row in the walls overview (design "Wall Designer", variant B1). */
data class WallSummary(
    val name: String,
    val type: WallType,
    val channels: Set<Channel>,
    val status: WallStatus,
    val variants: Int,
    val conversion: String,
    val updated: String,
)

val demoWalls = listOf(
    WallSummary(
        name = "Metered limit — invoices",
        type = WallType.Metered,
        channels = setOf(Channel.Web, Channel.MobileApp, Channel.Chat, Channel.InProduct, Channel.Email),
        status = WallStatus.Draft,
        variants = 2,
        conversion = "—",
        updated = "Today",
    ),
    WallSummary(
        name = "Hard wall — trial expiry",
        type = WallType.Hard,
        channels = setOf(Channel.Web, Channel.MobileApp, Channel.Email, Channel.Sms),
        status = WallStatus.Live,
        variants = 2,
        conversion = "4.8%",
        updated = "4 Jun",
    ),
    WallSummary(
        name = "Freemium — premium content",
        type = WallType.Freemium,
        channels = setOf(Channel.Web, Channel.InProduct, Channel.Email),
        status = WallStatus.Live,
        variants = 1,
        conversion = "2.1%",
        updated = "28 May",
    ),
    WallSummary(
        name = "Dynamic — CEP gate",
        type = WallType.Dynamic,
        channels = setOf(Channel.Web),
        status = WallStatus.Paused,
        variants = 1,
        conversion = "0.9%",
        updated = "14 May",
    ),
)
