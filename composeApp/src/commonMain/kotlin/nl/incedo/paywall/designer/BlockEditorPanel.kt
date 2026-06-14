package nl.incedo.paywall.designer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
import nl.incedo.paywall.walls.resolvedText

/** VWE-11: in-progress canvas reorder drag. */
private data class CanvasDragState(
    val blockId: String,
    val fromIndex: Int,
    val accDeltaY: Float = 0f,
    val dropIndex: Int,
)

/** VWE-10: in-progress palette → canvas drag insertion. */
private data class PaletteDragState(
    val label: String,
    val factory: (String) -> WallBlock,
    val paletteItemRootY: Float,
    val accDeltaY: Float = 0f,
    val dropIndex: Int = -1,
)

/**
 * VWE-10/11/12/14: block editor panel.
 *
 * Three sections:
 *  1. Block canvas — each block has a drag handle (VWE-11 reorder), plus ↑↓✕ keyboard controls (VWE-12/14).
 *  2. Per-block inspector.
 *  3. Palette — tap + to append, or drag onto canvas to insert at a specific position (VWE-10).
 *
 * Constraint violations (VWE-02/13) are shown inline; invalid drops are silently rejected.
 */
@Composable
fun BlockEditorPanel(
    layout: WallLayout,
    onLayoutChange: (WallLayout) -> Unit,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    /** VWE-17: called when the author taps "Save as template"; null hides the button. */
    onSaveAsTemplate: ((templateName: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val blocks = layout.blocks
    val violations = WallLayoutValidator.validate(blocks)
    val blocksState = rememberUpdatedState(blocks)

    // VWE-17: inline template-name form state
    var saveTemplateMode by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }

    // Root-coordinate positions captured via onGloballyPositioned, read inside gesture lambdas.
    val blockRootY = remember { mutableStateMapOf<String, Float>() }
    val blockRootH = remember { mutableStateMapOf<String, Float>() }
    var canvasRootY by remember { mutableStateOf(0f) }
    val paletteRootY = remember { mutableStateMapOf<String, Float>() }

    var canvasDrag by remember { mutableStateOf<CanvasDragState?>(null) }
    var paletteDrag by remember { mutableStateOf<PaletteDragState?>(null) }

    val dropIndicatorAt = when {
        canvasDrag != null -> canvasDrag!!.dropIndex
        paletteDrag != null && paletteDrag!!.dropIndex >= 0 -> paletteDrag!!.dropIndex
        else -> -1
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg)) {

        // VWE-18: undo/redo controls + VWE-17: save-as-template in the panel header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrmText("Block editor", style = CrmTheme.typography.h3)
            Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
                CrmTextButton("↩") { if (canUndo) onUndo() }
                CrmTextButton("↪") { if (canRedo) onRedo() }
                if (onSaveAsTemplate != null) {
                    CrmTextButton("Save as template") { saveTemplateMode = !saveTemplateMode }
                }
            }
        }
        // VWE-17: inline template name form — shown when author taps "Save as template"
        if (saveTemplateMode && onSaveAsTemplate != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrmTextField(
                    "Template name",
                    templateName,
                    onValueChange = { templateName = it },
                    modifier = Modifier.weight(1f),
                )
                CrmSecondaryButton("Save") {
                    if (templateName.isNotBlank()) {
                        onSaveAsTemplate(templateName.trim())
                        saveTemplateMode = false
                        templateName = ""
                    }
                }
                CrmTextButton("✕") { saveTemplateMode = false; templateName = "" }
            }
        }

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

        // ── Block canvas ───────────────────────────────────────────────────────
        CrmText("Layout", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        if (blocks.isEmpty()) {
            val hovering = paletteDrag != null && paletteDrag!!.dropIndex >= 0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords -> canvasRootY = coords.positionInRoot().y }
                    .then(
                        if (hovering) Modifier
                            .background(CrmTheme.colors.infoContainer)
                            .border(CrmBorder.thick, CrmTheme.colors.primary, CrmTheme.shapes.md)
                        else Modifier
                    )
                    .padding(CrmTheme.spacing.md),
            ) {
                CrmText(
                    if (hovering) "Release to add here" else "No blocks yet — drag from the palette or tap + below.",
                    style = CrmTheme.typography.caption,
                    color = if (hovering) CrmTheme.colors.primary else CrmTheme.colors.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier.onGloballyPositioned { coords ->
                    canvasRootY = coords.positionInRoot().y
                },
            ) {
                blocks.forEachIndexed { index, block ->
                    val isSelected = block.id == selectedId
                    val isDragging = canvasDrag?.blockId == block.id
                    val removalReason = WallLayoutValidator.removalBlockedReason(blocks, block.id)

                    // Drop indicator above this block
                    if (dropIndicatorAt == index) {
                        DropIndicator()
                    } else if (index > 0) {
                        Spacer(Modifier.height(CrmTheme.spacing.xs))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                blockRootY[block.id] = coords.positionInRoot().y
                                blockRootH[block.id] = coords.size.height.toFloat()
                            }
                            .background(
                                when {
                                    isDragging -> CrmTheme.colors.surfaceVariant
                                    isSelected -> CrmTheme.colors.infoContainer
                                    else -> CrmTheme.colors.surface
                                },
                            )
                            .border(
                                if (isDragging) CrmBorder.thick else CrmBorder.default,
                                if (isDragging) CrmTheme.colors.primary else CrmTheme.colors.divider,
                                CrmTheme.shapes.md,
                            )
                            .clickable { selectedId = if (isSelected) null else block.id }
                            .padding(CrmTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
                    ) {
                        // VWE-11: drag handle — pointer input only, no click semantics
                        Box(
                            modifier = Modifier
                                .padding(end = CrmTheme.spacing.xs)
                                .pointerInput(block.id) {
                                    detectDragGestures(
                                        onDragStart = { _ ->
                                            val cb = blocksState.value
                                            val fromIdx = cb.indexOfFirst { it.id == block.id }
                                            if (fromIdx >= 0) {
                                                canvasDrag = CanvasDragState(block.id, fromIdx, 0f, fromIdx)
                                            }
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            val ds = canvasDrag ?: return@detectDragGestures
                                            val newDelta = ds.accDeltaY + amount.y
                                            val originY = blockRootY[ds.blockId] ?: return@detectDragGestures
                                            val halfH = (blockRootH[ds.blockId] ?: 0f) / 2f
                                            val pointerY = originY + halfH + newDelta
                                            val cb = blocksState.value
                                            val newDrop = calcDropIdx(cb, blockRootY, blockRootH, pointerY)
                                            canvasDrag = ds.copy(accDeltaY = newDelta, dropIndex = newDrop)
                                        },
                                        onDragEnd = {
                                            val ds = canvasDrag
                                            if (ds != null) {
                                                val cb = blocksState.value
                                                // dropIndex is the target position in the original list;
                                                // after removing the dragged block, positions above fromIndex shift down by 1.
                                                val insertAt = if (ds.dropIndex > ds.fromIndex) ds.dropIndex - 1 else ds.dropIndex
                                                val clamped = insertAt.coerceIn(0, cb.lastIndex)
                                                if (clamped != ds.fromIndex) {
                                                    onLayoutChange(WallLayout(reorder(cb, ds.fromIndex, clamped)))
                                                }
                                            }
                                            canvasDrag = null
                                        },
                                        onDragCancel = { canvasDrag = null },
                                    )
                                },
                        ) {
                            CrmText(
                                "⋮⋮",
                                style = CrmTheme.typography.caption,
                                color = CrmTheme.colors.onSurfaceVariant,
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            CrmText(blockTypeLabel(block), style = CrmTheme.typography.label)
                            CrmText(
                                blockPreview(block),
                                style = CrmTheme.typography.caption,
                                color = CrmTheme.colors.onSurfaceVariant,
                            )
                        }
                        // Move up — disabled when ordering rule blocks it
                        val moveUpReason = canMoveUpReason(blocks, index)
                        CrmTextButton("↑") {
                            if (moveUpReason == null) onLayoutChange(WallLayout(swap(blocks, index, index - 1)))
                        }
                        // Move down
                        val moveDownReason = canMoveDownReason(blocks, index)
                        CrmTextButton("↓") {
                            if (moveDownReason == null) onLayoutChange(WallLayout(swap(blocks, index, index + 1)))
                        }
                        // Remove — blocked when removal would violate VWE-02
                        CrmTextButton("✕") {
                            if (removalReason == null) {
                                if (selectedId == block.id) selectedId = null
                                onLayoutChange(WallLayout(blocks.filter { it.id != block.id }))
                            }
                        }
                    }

                    if (isSelected && removalReason != null) {
                        CrmText(
                            "Cannot remove: $removalReason",
                            style = CrmTheme.typography.caption,
                            color = CrmTheme.colors.error,
                            modifier = Modifier.padding(start = CrmTheme.spacing.sm),
                        )
                    }
                    // VWE-16: per-block accessibility lint (ADM-17 WCAG 2.1 AA)
                    val lintViolations = blockAccessibilityLint(block)
                    if (lintViolations.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = CrmTheme.spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xxs),
                        ) {
                            lintViolations.forEach { msg ->
                                CrmText(
                                    "⚠ $msg",
                                    style = CrmTheme.typography.caption,
                                    color = CrmTheme.colors.warning,
                                )
                            }
                        }
                    }
                }

                // Drop indicator after last block
                if (dropIndicatorAt == blocks.size) {
                    DropIndicator()
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
        CrmText(
            "Add block — tap + or drag onto canvas",
            style = CrmTheme.typography.label,
            color = CrmTheme.colors.onSurfaceVariant,
        )
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
                        val isBeingDragged = paletteDrag?.label == label
                        CrmSecondaryButton(
                            if (isBeingDragged) "✦ $label" else "+ $label",
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    paletteRootY[label] = coords.positionInRoot().y
                                }
                                // VWE-10: drag palette item onto canvas to insert at a specific position
                                .pointerInput(label) {
                                    detectDragGestures(
                                        onDragStart = { _ ->
                                            paletteDrag = PaletteDragState(
                                                label = label,
                                                factory = factory,
                                                paletteItemRootY = paletteRootY[label] ?: 0f,
                                            )
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            val pd = paletteDrag ?: return@detectDragGestures
                                            val newDelta = pd.accDeltaY + amount.y
                                            val pointerAbsY = pd.paletteItemRootY + newDelta
                                            val cb = blocksState.value
                                            val newDrop = if (pointerAbsY >= canvasRootY) {
                                                calcDropIdx(cb, blockRootY, blockRootH, pointerAbsY)
                                            } else -1
                                            paletteDrag = pd.copy(accDeltaY = newDelta, dropIndex = newDrop)
                                        },
                                        onDragEnd = {
                                            val pd = paletteDrag
                                            if (pd != null && pd.dropIndex >= 0) {
                                                val cb = blocksState.value
                                                val bType = paletteBlockTypes[pd.label]
                                                val blocked = bType != null &&
                                                    WallLayoutValidator.additionBlockedReason(cb, bType) != null
                                                if (!blocked) {
                                                    val newId = "${pd.label.lowercase().replace(" ", "-")}-${cb.size}"
                                                    val newBlock = pd.factory(newId)
                                                    // VWE-02: ConsentBlock must always be first (AC-14)
                                                    val insertIdx = if (newBlock is ConsentBlock) 0 else pd.dropIndex
                                                    onLayoutChange(WallLayout(insertAt(cb, insertIdx, newBlock)))
                                                }
                                            }
                                            paletteDrag = null
                                        },
                                        onDragCancel = { paletteDrag = null },
                                    )
                                },
                        ) {
                            // Tap + to append at end (unchanged from VWE-12 keyboard path)
                            if (addReason == null) {
                                val newId = "${label.lowercase().replace(" ", "-")}-${blocks.size}"
                                val newBlock = factory(newId)
                                val newBlocks = if (newBlock is ConsentBlock) listOf(newBlock) + blocks else blocks + newBlock
                                onLayoutChange(WallLayout(newBlocks))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Thin colored line shown at the drag insertion point between two blocks. */
@Composable
private fun DropIndicator() {
    Spacer(Modifier.height(CrmTheme.spacing.xs))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CrmTheme.spacing.xxs)
            .background(CrmTheme.colors.primary),
    )
    Spacer(Modifier.height(CrmTheme.spacing.xs))
}

@Composable
private fun BlockInspector(block: WallBlock, onUpdate: (WallBlock) -> Unit) {
    when (block) {
        is Headline -> {
            CrmTextField("Text", block.text, onValueChange = { v -> onUpdate(block.copy(text = v)) })
            LocaleOverridesEditor(block.textOverrides) { overrides -> onUpdate(block.copy(textOverrides = overrides)) }
        }
        is BodyCopy -> {
            CrmTextField("Text", block.text, onValueChange = { v -> onUpdate(block.copy(text = v)) }, singleLine = false)
            LocaleOverridesEditor(block.textOverrides) { overrides -> onUpdate(block.copy(textOverrides = overrides)) }
        }
        is CtaButton -> {
            CrmTextField("Label", block.label, onValueChange = { v -> onUpdate(block.copy(label = v)) })
            Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
                CtaRole.entries.forEach { role ->
                    CrmSecondaryButton(role.name) { onUpdate(block.copy(role = role)) }
                }
            }
            LocaleOverridesEditor(block.textOverrides) { overrides -> onUpdate(block.copy(textOverrides = overrides)) }
        }
        is LoginLink -> {
            CrmTextField("Label", block.label, onValueChange = { v -> onUpdate(block.copy(label = v)) })
            LocaleOverridesEditor(block.textOverrides) { overrides -> onUpdate(block.copy(textOverrides = overrides)) }
        }
        is ImageBlock -> {
            CrmTextField("URL", block.url, onValueChange = { v -> onUpdate(block.copy(url = v)) })
            CrmTextField("Alt text (ADM-17)", block.alt, onValueChange = { v -> onUpdate(block.copy(alt = v)) })
        }
        is LegalText -> {
            CrmTextField("Text", block.text, onValueChange = { v -> onUpdate(block.copy(text = v)) }, singleLine = false)
            LocaleOverridesEditor(block.textOverrides) { overrides -> onUpdate(block.copy(textOverrides = overrides)) }
        }
        is SocialProof -> {
            CrmTextField("Text", block.text, onValueChange = { v -> onUpdate(block.copy(text = v)) })
            LocaleOverridesEditor(block.textOverrides) { overrides -> onUpdate(block.copy(textOverrides = overrides)) }
        }
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

/**
 * VWE-04/ADM-15: editor for per-block locale overrides.
 * Shows existing overrides with a remove button, and an "Add override" form.
 */
@Composable
private fun LocaleOverridesEditor(
    overrides: Map<String, String>,
    onOverridesChange: (Map<String, String>) -> Unit,
) {
    var newLocale by remember { mutableStateOf("") }
    var newText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
        CrmText(
            "Locale overrides (VWE-04)",
            style = CrmTheme.typography.label,
            color = CrmTheme.colors.onSurfaceVariant,
        )
        if (overrides.isEmpty()) {
            CrmText(
                "No overrides — add one below to translate this block.",
                style = CrmTheme.typography.caption,
                color = CrmTheme.colors.onSurfaceVariant,
            )
        } else {
            overrides.forEach { (locale, text) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CrmText(
                        "$locale:",
                        style = CrmTheme.typography.label,
                        modifier = Modifier.weight(0.3f),
                    )
                    CrmText(
                        text.take(50),
                        style = CrmTheme.typography.caption,
                        color = CrmTheme.colors.onSurfaceVariant,
                        modifier = Modifier.weight(0.6f),
                    )
                    CrmTextButton("✕") {
                        onOverridesChange(overrides - locale)
                    }
                }
            }
        }
        // Add override
        Row(
            horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrmTextField(
                "Locale (e.g. nl-NL)",
                newLocale,
                onValueChange = { newLocale = it },
                modifier = Modifier.weight(0.35f),
            )
            CrmTextField(
                "Override text",
                newText,
                onValueChange = { newText = it },
                modifier = Modifier.weight(0.55f),
            )
            CrmSecondaryButton("+") {
                if (newLocale.isNotBlank() && newText.isNotBlank()) {
                    onOverridesChange(overrides + (newLocale.trim() to newText))
                    newLocale = ""
                    newText = ""
                }
            }
        }
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

private fun canMoveUpReason(blocks: List<WallBlock>, index: Int): String? {
    if (index == 0) return "Already at top"
    if (blocks[index - 1] is ConsentBlock) return "Cannot move above the consent step (AC-14)"
    return WallLayoutValidator.validate(swap(blocks, index, index - 1)).firstOrNull()
}

private fun canMoveDownReason(blocks: List<WallBlock>, index: Int): String? {
    if (index == blocks.lastIndex) return "Already at bottom"
    if (blocks[index] is ConsentBlock) return "Consent block must remain first (AC-14)"
    return WallLayoutValidator.validate(swap(blocks, index, index + 1)).firstOrNull()
}

private fun swap(list: List<WallBlock>, a: Int, b: Int): List<WallBlock> =
    list.toMutableList().also { it[a] = list[b]; it[b] = list[a] }

private fun reorder(list: List<WallBlock>, fromIdx: Int, toIdx: Int): List<WallBlock> {
    val mutable = list.toMutableList()
    val item = mutable.removeAt(fromIdx)
    mutable.add(toIdx.coerceIn(0, mutable.size), item)
    return mutable
}

private fun insertAt(list: List<WallBlock>, index: Int, block: WallBlock): List<WallBlock> {
    val mutable = list.toMutableList()
    mutable.add(index.coerceIn(0, mutable.size), block)
    return mutable
}

/**
 * VWE-16 (ADM-17): WCAG 2.1 AA accessibility checks for a single block.
 * Returns a list of human-readable violation messages; empty = no issues.
 * Checked inline in the block editor and used by [WallDesignerScreen] to gate publish.
 */
internal fun blockAccessibilityLint(block: WallBlock): List<String> = buildList {
    when (block) {
        is ImageBlock -> if (block.alt.isBlank())
            add("Missing alt text — add a description or mark as decorative (WCAG 1.1.1).")
        is CtaButton -> if (block.label.length > 40)
            add("CTA label is very long (${block.label.length} chars) — may truncate on small viewports (WCAG 2.5.3).")
        is LoginLink -> if (block.label.length > 40)
            add("Login link label is very long (${block.label.length} chars) — may truncate on small viewports (WCAG 2.5.3).")
        is Headline -> if (block.text.isBlank())
            add("Headline text is empty — screen readers need a meaningful heading (WCAG 1.3.1).")
        is BodyCopy -> if (block.text.isBlank())
            add("Body copy text is empty — provide meaningful content for assistive technology (WCAG 1.3.1).")
        is LegalText -> if (block.text.isBlank())
            add("Legal text is empty — fill in content or remove the block (WCAG 1.3.1).")
        is SocialProof -> if (block.text.isBlank())
            add("Social proof text is empty — fill in content or remove the block (WCAG 1.3.1).")
        else -> {}
    }
}

private fun calcDropIdx(
    blocks: List<WallBlock>,
    positions: Map<String, Float>,
    heights: Map<String, Float>,
    pointerAbsY: Float,
): Int = blocks.indices.firstOrNull { i ->
    val top = positions[blocks[i].id] ?: return@firstOrNull false
    val h = heights[blocks[i].id] ?: 0f
    pointerAbsY < top + h / 2f
} ?: blocks.size
