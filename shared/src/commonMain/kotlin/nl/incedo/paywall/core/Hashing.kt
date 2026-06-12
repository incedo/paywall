package nl.incedo.paywall.core

/**
 * FNV-1a 32-bit: deterministic and platform-independent (Kotlin's hashCode
 * is neither guaranteed stable across versions nor across targets). Used for
 * variant assignment (EX-01) and tag sharding (DM-06).
 */
fun fnv1a32(input: String): Int {
    var hash = -0x7ee3623b // 0x811C9DC5
    for (byte in input.encodeToByteArray()) {
        hash = hash xor (byte.toInt() and 0xFF)
        hash *= 0x01000193
    }
    return hash
}
