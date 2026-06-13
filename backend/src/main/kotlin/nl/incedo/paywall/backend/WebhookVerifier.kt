package nl.incedo.paywall.backend

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * NFR-03: verifies provider webhook signatures to prevent replay/forgery.
 *
 * Header format: `X-Provider-Signature: sha256={hex-encoded-HMAC-SHA256}`
 * HMAC is computed over the raw request body with the shared [secret].
 *
 * When no secret is configured (dev/sandbox), all requests are accepted.
 * In production, `WEBHOOK_SECRET` must be set and the provider must sign.
 */
class WebhookVerifier(private val secret: String?) {

    /**
     * @return true when the signature is valid or no secret is configured.
     *         false when a secret is configured and the signature is missing or invalid.
     */
    fun verify(body: ByteArray, signatureHeader: String?): Boolean {
        if (secret == null) return true
        if (signatureHeader.isNullOrBlank()) return false
        val expected = "sha256=${hex(hmac(body, secret))}"
        return constantTimeEquals(expected, signatureHeader.trim())
    }

    private fun hmac(data: ByteArray, key: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    /** Constant-time comparison to prevent timing attacks. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    companion object {
        const val SIGNATURE_HEADER = "X-Provider-Signature"
    }
}
