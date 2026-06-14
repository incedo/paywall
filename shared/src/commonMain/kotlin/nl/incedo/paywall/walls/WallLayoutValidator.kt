package nl.incedo.paywall.walls

/**
 * VWE-02: structural validity rules for a [WallLayout].
 *
 * Enforced server-side on save (ADM-01 — the server is the security boundary).
 * Also used client-side for UX feedback before the user tries to save (VWE-13).
 *
 * Rules:
 *  1. At least one value-proposition block (Headline or BodyCopy).
 *  2. At least one PRIMARY CtaButton (PW-03).
 *  3. ConsentBlock is a singleton (at most one).
 *  4. PriceDisplay is a singleton (at most one).
 *  5. ConsentBlock, if present, must be the first block (AC-14).
 *  6. LegalText, if present, must follow all CtaButton blocks.
 */
object WallLayoutValidator {

    /** Returns a list of human-readable violation messages, empty when valid. */
    fun validate(layout: WallLayout): List<String> = validate(layout.blocks)

    fun validate(blocks: List<WallBlock>): List<String> = buildList {
        if (blocks.none { it is Headline || it is BodyCopy }) {
            add("A wall must have at least one value-proposition block (Headline or Body Copy).")
        }
        if (blocks.filterIsInstance<CtaButton>().none { it.role == CtaRole.PRIMARY }) {
            add("A wall must have at least one primary CTA button (PW-03).")
        }
        if (blocks.filterIsInstance<ConsentBlock>().size > 1) {
            add("ConsentBlock is a singleton — only one may appear per layout.")
        }
        if (blocks.filterIsInstance<PriceDisplay>().size > 1) {
            add("PriceDisplay is a singleton — only one may appear per layout.")
        }
        val consentIdx = blocks.indexOfFirst { it is ConsentBlock }
        if (consentIdx > 0) {
            add("ConsentBlock must be the first block in the layout (AC-14).")
        }
        val lastCtaIdx = blocks.indexOfLast { it is CtaButton }
        val firstLegalIdx = blocks.indexOfFirst { it is LegalText }
        if (firstLegalIdx >= 0 && lastCtaIdx >= 0 && firstLegalIdx < lastCtaIdx) {
            add("LegalText must follow all CTA buttons.")
        }
    }

    /** Returns a reason string if removing [blockId] would violate a constraint, else null. */
    fun removalBlockedReason(blocks: List<WallBlock>, blockId: String): String? {
        val after = blocks.filter { it.id != blockId }
        val violations = validate(after)
        return violations.firstOrNull()
    }

    /** Returns a reason string if adding a block of [type] would violate a constraint, else null. */
    fun additionBlockedReason(blocks: List<WallBlock>, type: String): String? = when (type) {
        "ConsentBlock" -> if (blocks.any { it is ConsentBlock }) "Only one ConsentBlock is allowed." else null
        "PriceDisplay" -> if (blocks.any { it is PriceDisplay }) "Only one PriceDisplay is allowed." else null
        else -> null
    }
}
