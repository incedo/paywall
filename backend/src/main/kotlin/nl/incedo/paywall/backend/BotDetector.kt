package nl.incedo.paywall.backend

// AN-05: bot traffic must be flagged in events, not silently dropped, so that
// filtering is auditable. These patterns cover verified search crawlers (which
// the edge allows through to serve teaser-only — BP-02/03) and archive bots.
private val BOT_PATTERN = Regex(
    "bot|crawler|spider|archive|slurp|bingpreview|facebookexternalhit|twitterbot" +
        "|linkedinbot|whatsapp|pingdom|headlessChrome|python-requests|curl/",
    RegexOption.IGNORE_CASE,
)

fun isBotUserAgent(userAgent: String?): Boolean =
    userAgent != null && BOT_PATTERN.containsMatchIn(userAgent)
