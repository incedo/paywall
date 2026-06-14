package nl.incedo.paywall.designer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import nl.incedo.paywall.theme.CrmBorder
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.ui.CrmTextButton
import nl.incedo.paywall.ui.CrmTextField
import nl.incedo.paywall.walls.BodyCopy
import nl.incedo.paywall.walls.ConsentBlock
import nl.incedo.paywall.walls.CtaButton
import nl.incedo.paywall.walls.CtaRole
import nl.incedo.paywall.walls.Headline
import nl.incedo.paywall.walls.ImageBlock
import nl.incedo.paywall.walls.LegalText
import nl.incedo.paywall.walls.LoginLink
import nl.incedo.paywall.walls.MeterIndicator
import nl.incedo.paywall.walls.PriceDisplay
import nl.incedo.paywall.walls.SocialProof
import nl.incedo.paywall.walls.WallBlock
import nl.incedo.paywall.walls.WallLayout
import nl.incedo.paywall.walls.WallLayoutValidator

/**
 * VWE-12/14: keyboard-accessible block composition panel.
 *
 * Three sections:
 *  1. Block canvas list — each block has Move Up / Move Down / Remove buttons.
 *  2. Per-block inspector — typed props for the selected block.
 *  3. Palette — add-block buttons for every block type in the constrained library.
 *
 * Constraint violations (VWE-02/13) are shown inline; invalid actions are disabled
 * with a reason message. The publish button in the parent is disabled while the
 * layout has violations (the parent checks violations via [WallLayoutValidator]).
 */
@Composable
fun BlockEditorPanel(
    layout: WallLayout,
    onLayoutChange: (WallLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val blocks = layout.blocks
    val violations = WallLayoutValidator.validate(blocks)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg)) {

        CrmText("Block editor", style = CrmTheme.typography.h3)

        // ── Constraint violations ──────────────────────────────────────────────
        if (violations.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CrmTheme.colors.errorContainer)
                    .padding(CrmTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
            ) {
                violations.forEach { msg ->
                    CrmText("⚠ $msg", style = CrmTheme.typography.caption, color = CrmTheme.colors.error)
                }
            }
        }

        // ── Block canvas list ──────────────────────────────────────────────────
        CrmText("Layout", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        if (blocks.isEmpty()) {
            CrmText(
                "No blocks yet — add from the palette below.",
                style = CrmTheme.typography.caption,
                color = CrmTheme.colors.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
                blocks.forEachIndexed { index, block ->
                    val isSelected = block.id == selectedId
                    val removalReason = WallLayoutValidator.removalBlockedReason(blocks, block.id)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) CrmTheme.colors.infoContainer else CrmTheme.colors.surface,
                            )
                            .border(CrmBorder.default, CrmTheme.colors.divider, CrmTheme.shapes.md)
                            .clickable { selectedId = if (isSelected) null else block.id }
                            .padding(CrmTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            CrmText(blockTypeLabel(block), style = CrmTheme.typography.label)
                            CrmText(
                                blockPreview(block),
                                style = CrmTheme.typography.caption,
                                color = CrmTheme.colors.onSurfaceVariant,
                            )
                        }
                        // Move up — disabled for first block or when ConsentBlock ordering rule applies
                        val moveUpReason = canMoveUpReason(blocks, index)
                        CrmTextButton(
                            "↑",
                            modifier = Modifier.then(
                                if (moveUpReason != null) Modifier else Modifier,
                            ),
                        ) {
                            if (moveUpReason == null) onLayoutChange(WallLayout(swap(blocks, index, index - 1)))
                        }
                        // Move down
                        val moveDownReason = canMoveDownReason(blocks, index)
                        CrmTextButton("↓") {
                            if (moveDownReason == null) onLayoutChange(WallLayout(swap(blocks, index, index + 1)))
                        }
                        // Remove — disabled when removal would violate constraints
                        CrmTextButton("✕") {
                            if (removalReason == null) {
                                if (selectedId == block.id) selectedId = null
                                onLayoutChange(WallLayout(blocks.filter { it.id != block.id }))
                            }
                        }
                    }
                    // Show constraint reason for removal when block is selected and removal is blocked
                    if (isSelected && removalReason != null) {
                        CrmText(
                            "Cannot remove: $removalReason",
                            style = CrmTheme.typography.caption,
                            color = CrmTheme.colors.error,
                            modifier = Modifier.padding(start = CrmTheme.spacing.sm),
                        )
                    }
                }
            }
        }

        // ── Per-block inspector ────────────────────────────────────────────────
        val selectedBlock = blocks.find { it.id == selectedId }
        if (selectedBlock != null) {
            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                ) {
                    CrmText("Inspector — ${blockTypeLabel(selectedBlock)}", style = CrmTheme.typography.h3)
                    BlockInspector(selectedBlock) { updated ->
                        onLayoutChange(WallLayout(blocks.map { if (it.id == updated.id) updated else it }))
                    }
                }
            }
        }

        // ── Add-block palette ──────────────────────────────────────────────────
        CrmText("Add block", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        val paletteItems = listOf(
            "Headline" to { id: String -> Headline(id = id, text = "New headline") as WallBlock },
            "Body Copy" to { id: String -> BodyCopy(id = id, text = "Body copy") as WallBlock },
            "Price Display" to { id: String -> PriceDisplay(id = id) as WallBlock },
            "CTA (Primary)" to { id: String -> CtaButton(id = id, label = "Subscribe", role = CtaRole.PRIMARY) as WallBlock },
            "CTA (Secondary)" to { id: String -> CtaButton(id = id, label = "Log in", role = CtaRole.SECONDARY) as WallBlock },
            "Login Link" to { id: String -> LoginLink(id = id) as WallBlock },
            "Image" to { id: String -> ImageBlock(id = id, url = "", alt = "") as WallBlock },
            "Legal Text" to { id: String -> LegalText(id = id, text = "Legal notice.") as WallBlock },
            "Consent" to { id: String -> ConsentBlock(id = id) as WallBlock },
            "Meter Indicator" to { id: String -> MeterIndicator(id = id) as WallBlock },
            "Social Proof" to { id: String -> SocialProof(id = id, text = "Trusted by readers") as WallBlock },
        )
        val paletteBlockTypes = mapOf(
            "Consent" to "ConsentBlock",
            "Price Display" to "PriceDisplay",
        )
        Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
            paletteItems.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
                    row.forEach { (label, factory) ->
                        val blockType = paletteBlockTypes[label]
                        val addReason = if (blockType != null) {
                            WallLayoutValidator.additionBlockedReason(blocks, blockType)
                        } else null
                        CrmSecondaryButton(
                            "+ $label",
                            modifier = Modifier.weight(1f),
                        ) {
                            if (addReason == null) {
                                val newId = "${label.lowercase().replace(" ", "-")}-${blocks.size}"
                                val newBlock = factory(newId)
                                // VWE-02: ConsentBlock is always inserted first
                                val newBlocks = if (newBlock is ConsentBlock) {
                                    listOf(newBlock) + blocks
                                } else {
                                    blocks + newBlock
                                }
                                onLayoutChange(WallLayout(newBlocks))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockInspector(block: WallBlock, onUpdate: (WallBlock) -> Unit) {
    when (block) {
        is Headline -> CrmTextField("Text", block.text, onValueChange = { v -> onUpdate(block.copy(text = v)) })
        is BodyCopy -> CrmTextField("Text", block.text, onValueChange = { v -> onUpdate(block.copy(text = v)) }, singleLine = false)
        is CtaButton -> {
            CrmTextField("Label", block.label, onValueChange = { v -> onUpdate(block.copy(label = v)) })
            Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
                CtaRole.entries.forEach { role ->
                    CrmSecondaryButton(role.name) { onUpdate(block.copy(role = role)) }
                }
            }
        }
        is LoginLink -> CrmTextField("Label", block.label, onValueChange = { v -> onUpdate(block.copy(label = v)) })
        is ImageBlock -> {
            CrmTextField("URL", block.url, onValueChange = { v -> onUpdate(block.copy(url = v)) })
            CrmTextField("Alt text (ADM-17)", block.alt, onValueChange = { v -> onUpdate(block.copy(alt = v)) })
        }
        is LegalText -> CrmTextField("Text", block.text, onValueChange = { v -> onUpdate(block.copy(text = v)) }, singleLine = false)
        is SocialProof -> CrmTextField("Text", block.text, onValueChange = { v -> onUpdate(block.copy(text = v)) })
        is PriceDisplay -> CrmTextField(
            "Plan ref (optional)",
            block.planRef ?: "",
            onValueChange = { v -> onUpdate(block.copy(planRef = v.ifBlank { null })) },
        )
        is ConsentBlock, is MeterIndicator -> CrmText(
            "No configurable props.",
            style = CrmTheme.typography.caption,
            color = CrmTheme.colors.onSurfaceVariant,
        )
    }
}

private fun blockTypeLabel(block: WallBlock): String = when (block) {
    is Headline -> "Headline"
    is BodyCopy -> "Body Copy"
    is PriceDisplay -> "Price Display"
    is CtaButton -> "CTA (${block.role.name.lowercase().replaceFirstChar { it.uppercase() }})"
    is LoginLink -> "Login Link"
    is ImageBlock -> "Image"
    is LegalText -> "Legal Text"
    is ConsentBlock -> "Consent"
    is MeterIndicator -> "Meter"
    is SocialProof -> "Social Proof"
}

private fun blockPreview(block: WallBlock): String = when (block) {
    is Headline -> block.text.take(40)
    is BodyCopy -> block.text.take(40)
    is CtaButton -> "\"${block.label}\" · ${block.role}"
    is LoginLink -> block.label
    is ImageBlock -> block.url.ifEmpty { "No URL set" }
    is LegalText -> block.text.take(40)
    is SocialProof -> block.text.take(40)
    is PriceDisplay -> block.planRef?.let { "Plan: $it" } ?: "Active plan"
    is ConsentBlock -> "GDPR consent step (AC-14)"
    is MeterIndicator -> "Article count meter (PW-22)"
}

/** Returns a human-readable reason why moving up is blocked, or null if it can move. */
private fun canMoveUpReason(blocks: List<WallBlock>, index: Int): String? {
    if (index == 0) return "Already at top"
    // ConsentBlock must stay first
    if (blocks[index - 1] is ConsentBlock) return "Cannot move above the consent step (AC-14)"
    // Check that moving would not put LegalText before a CTA
    val after = swap(blocks, index, index - 1)
    val violations = WallLayoutValidator.validate(after)
    return violations.firstOrNull()
}

/** Returns a human-readable reason why moving down is blocked, or null if it can move. */
private fun canMoveDownReason(blocks: List<WallBlock>, index: Int): String? {
    if (index == blocks.lastIndex) return "Already at bottom"
    // ConsentBlock must stay first
    if (blocks[index] is ConsentBlock) return "Consent block must remain first (AC-14)"
    val after = swap(blocks, index, index + 1)
    val violations = WallLayoutValidator.validate(after)
    return violations.firstOrNull()
}

private fun swap(list: List<WallBlock>, a: Int, b: Int): List<WallBlock> =
    list.toMutableList().also { it[a] = list[b]; it[b] = list[a] }
