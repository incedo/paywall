package nl.incedo.paywall.backend

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class BotDetectorTest {

    // isBotUserAgent ---------------------------------------------------------------

    @Test
    fun knownBotUaIsDetected() {
        assertTrue(isBotUserAgent("Googlebot/2.1 (+http://www.google.com/bot.html)"))
        assertTrue(isBotUserAgent("Mozilla/5.0 (compatible; Bingbot/2.0)"))
        assertTrue(isBotUserAgent("Twitterbot/1.0"))
        assertTrue(isBotUserAgent("curl/7.64.1"))
        assertTrue(isBotUserAgent("python-requests/2.28.1"))
        assertTrue(isBotUserAgent("LinkedInBot/1.0"))
    }

    @Test
    fun regularBrowserUaIsNotDetected() {
        assertFalse(isBotUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"))
        assertFalse(isBotUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Mobile"))
        assertFalse(isBotUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
    }

    @Test
    fun nullOrBlankUaIsNotDetected() {
        assertFalse(isBotUserAgent(null))
        assertFalse(isBotUserAgent(""))
    }

    // isBotByCfScore (INF-09) -------------------------------------------------------

    @Test
    fun scoreAtOrBelowThresholdIsBot() {
        // Cloudflare classifies ≤29 as "definitely automated"
        assertTrue(isBotByCfScore(1))
        assertTrue(isBotByCfScore(15))
        assertTrue(isBotByCfScore(29))
    }

    @Test
    fun scoreAboveThresholdIsNotBot() {
        assertFalse(isBotByCfScore(30))
        assertFalse(isBotByCfScore(50))
        assertFalse(isBotByCfScore(99))
    }

    @Test
    fun nullScoreIsNotBot() {
        // Absent header = no CF signal = do not flag
        assertFalse(isBotByCfScore(null))
    }

    // parseCfBotScore (INF-09) -------------------------------------------------------

    @Test
    fun validIntegerIsParsed() {
        assertEquals(15, parseCfBotScore("15"))
        assertEquals(1, parseCfBotScore("1"))
        assertEquals(99, parseCfBotScore("99"))
    }

    @Test
    fun whitespaceAroundValueIsTrimmed() {
        assertEquals(29, parseCfBotScore("  29  "))
    }

    @Test
    fun nonNumericValueReturnsNull() {
        assertNull(parseCfBotScore("high"))
        assertNull(parseCfBotScore(""))
        assertNull(parseCfBotScore("   "))
    }

    @Test
    fun nullHeaderReturnsNull() {
        assertNull(parseCfBotScore(null))
    }
}
