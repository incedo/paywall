package nl.incedo.paywall.backend

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * IPW-03: validates and overlap-detects CIDR ranges at partner IP config load.
 * Invalid or overlapping ranges are rejected before the event is stored.
 */
object CidrValidator {

    /** Returns an error message, or null when the CIDR is valid. */
    fun validate(cidr: String): String? {
        val slash = cidr.indexOf('/')
        if (slash < 0) return "CIDR must include prefix length (e.g. '10.0.0.0/8')"
        val ipPart = cidr.substring(0, slash)
        val prefixStr = cidr.substring(slash + 1)

        val prefixLen = prefixStr.toIntOrNull()
            ?: return "prefix length must be an integer"
        val addr = runCatching { InetAddress.getByName(ipPart) }.getOrNull()
            ?: return "invalid IP address: '$ipPart'"

        val maxPrefix = when (addr) {
            is Inet4Address -> 32
            is Inet6Address -> 128
            else -> return "unsupported address type"
        }
        if (prefixLen < 0 || prefixLen > maxPrefix) {
            return "prefix length $prefixLen out of range (0–$maxPrefix for ${addr.javaClass.simpleName})"
        }

        // Verify host bits are zero — network address only.
        val bytes = addr.address
        if (!networkBitsOnly(bytes, prefixLen)) {
            val expectedNet = networkAddress(bytes, prefixLen)
            val expectedCidr = "${InetAddress.getByAddress(expectedNet).hostAddress}/$prefixLen"
            return "host bits must be zero; did you mean $expectedCidr?"
        }
        return null
    }

    /**
     * IPW-03: returns true when [candidate] overlaps with any CIDR in [existing].
     * Overlap means one range is a subset of the other (or they are equal).
     * IPv4 only; IPv6 CIDRs are compared by exact match.
     */
    fun overlaps(candidate: String, existing: Iterable<String>): String? {
        for (other in existing) {
            if (candidate == other) return "duplicate CIDR $candidate already active for this partner"
            if (cidrsOverlap(candidate, other)) {
                return "CIDR $candidate overlaps with existing range $other"
            }
        }
        return null
    }

    private fun cidrsOverlap(a: String, b: String): Boolean {
        val (addrA, prefA) = parseCidr(a) ?: return false
        val (addrB, prefB) = parseCidr(b) ?: return false
        if (addrA !is Inet4Address || addrB !is Inet4Address) return a == b
        val ia = toInt(addrA.address)
        val ib = toInt(addrB.address)
        // Smaller prefix = larger block. A is contained in B iff masking A's address
        // with B's mask gives B's network, and vice versa.
        val maskA = prefixMask(prefA)
        val maskB = prefixMask(prefB)
        return (ia and maskB) == (ib and maskB) || (ib and maskA) == (ia and maskA)
    }

    private data class ParsedCidr(val addr: InetAddress, val prefix: Int)

    private fun parseCidr(cidr: String): ParsedCidr? {
        val slash = cidr.indexOf('/') .takeIf { it >= 0 } ?: return null
        val addr = runCatching { InetAddress.getByName(cidr.substring(0, slash)) }.getOrNull() ?: return null
        val prefix = cidr.substring(slash + 1).toIntOrNull() ?: return null
        return ParsedCidr(addr, prefix)
    }

    private fun networkBitsOnly(bytes: ByteArray, prefixLen: Int): Boolean {
        val totalBits = bytes.size * 8
        if (prefixLen >= totalBits) return true
        val hostBits = totalBits - prefixLen
        return (0 until hostBits).all { bit ->
            val byteIdx = bytes.size - 1 - (bit / 8)
            val bitIdx = bit % 8
            (bytes[byteIdx].toInt() and (1 shl bitIdx)) == 0
        }
    }

    private fun networkAddress(bytes: ByteArray, prefixLen: Int): ByteArray {
        val result = bytes.copyOf()
        val totalBits = bytes.size * 8
        val hostBits = totalBits - prefixLen
        for (bit in 0 until hostBits) {
            val byteIdx = result.size - 1 - (bit / 8)
            val bitIdx = bit % 8
            result[byteIdx] = (result[byteIdx].toInt() and (1 shl bitIdx).inv()).toByte()
        }
        return result
    }

    private fun toInt(bytes: ByteArray): Int =
        ((bytes[0].toInt() and 0xFF) shl 24) or
        ((bytes[1].toInt() and 0xFF) shl 16) or
        ((bytes[2].toInt() and 0xFF) shl 8) or
        (bytes[3].toInt() and 0xFF)

    private fun prefixMask(prefixLen: Int): Int =
        if (prefixLen == 0) 0 else (0xFFFFFFFF.toInt() shl (32 - prefixLen))
}
