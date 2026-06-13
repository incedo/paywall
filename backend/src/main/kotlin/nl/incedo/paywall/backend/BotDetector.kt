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

/**
 * INF-09: Cloudflare bot management score forwarded by the Worker as a trusted
 * header (X-CF-Bot-Score, 1–99). Scores ≤ 29 are "definitely automated" per
 * Cloudflare's classification; ≤ 49 are "likely automated". Only trusted when
 * the origin secret is configured (proves the header came from the Worker, not
 * a client — INF-02/BP-02).
 *
 * Returns true when the score indicates automated traffic, false when absent or
 * above the threshold. Combined with [isBotUserAgent] via logical OR so either
 * signal alone is sufficient to flag the request.
 */
fun isBotByCfScore(cfBotScore: Int?): Boolean = cfBotScore != null && cfBotScore <= 29

/**
 * INF-09: parses the X-CF-Bot-Score header value into an Int.
 * Returns null for absent, blank, or non-numeric values.
 */
fun parseCfBotScore(headerValue: String?): Int? = headerValue?.trim()?.toIntOrNull()
