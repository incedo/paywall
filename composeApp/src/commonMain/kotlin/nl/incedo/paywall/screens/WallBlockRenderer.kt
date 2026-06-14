package nl.incedo.paywall.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.incedo.paywall.model.WallDefinition
import nl.incedo.paywall.model.WallType
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.ui.CrmTextButton
import nl.incedo.paywall.ui.CrmUsageMeter
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
import nl.incedo.paywall.walls.toDefaultLayout

/**
 * VWE-01/05: renders a [WallLayout] by mapping each block to a CrmTheme-tokened
 * composable. This is the single renderer used by both the live preview in the
 * designer and the public gate — no rendering drift, no-JS preserved (BP-01).
 *
 * Adjacent [CtaButton] blocks are grouped into a Row for correct visual treatment.
 * [meterUsed]/[meterLimit] feed the optional [MeterIndicator] block (PW-22/23).
 */
@Composable
fun WallLayoutRenderer(
    layout: WallLayout,
    meterUsed: Int = 0,
    meterLimit: Int = 5,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
    ) {
        val blocks = layout.blocks
        var i = 0
        while (i < blocks.size) {
            val block = blocks[i]
            // Group consecutive CtaButton blocks into a Row
            if (block is CtaButton) {
                val ctaGroup = mutableListOf<CtaButton>()
                while (i < blocks.size && blocks[i] is CtaButton) {
                    ctaGroup += blocks[i] as CtaButton
                    i++
                }
                CtaButtonRow(ctaGroup)
            } else {
                RenderBlock(block, meterUsed, meterLimit)
                i++
            }
        }
    }
}

@Composable
private fun CtaButtonRow(buttons: List<CtaButton>) {
    Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md)) {
        buttons.forEach { btn ->
            when (btn.role) {
                CtaRole.PRIMARY -> CrmPrimaryButton(btn.label)
                CtaRole.SECONDARY -> CrmSecondaryButton(btn.label)
            }
        }
    }
}

@Composable
private fun RenderBlock(block: WallBlock, meterUsed: Int, meterLimit: Int) {
    when (block) {
        is Headline -> CrmText(
            block.text,
            style = CrmTheme.typography.h3,
            color = CrmTheme.colors.onBackground,
        )
        is BodyCopy -> CrmText(
            block.text,
            color = CrmTheme.colors.onSurfaceVariant,
        )
        is PriceDisplay -> CrmText(
            "€—",
            style = CrmTheme.typography.h3,
            color = CrmTheme.colors.primary,
        )
        is CtaButton -> when (block.role) {
            CtaRole.PRIMARY -> CrmPrimaryButton(block.label)
            CtaRole.SECONDARY -> CrmSecondaryButton(block.label)
        }
        is LoginLink -> CrmTextButton(block.label)
        is ImageBlock -> {
            // Image URL rendered as a placeholder until async image loading is wired in
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp)
                    .background(CrmTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                CrmText(block.alt.ifEmpty { "Image" }, color = CrmTheme.colors.onSurfaceVariant)
            }
        }
        is LegalText -> CrmText(
            block.text,
            style = CrmTheme.typography.caption,
            color = CrmTheme.colors.onSurfaceVariant,
        )
        is ConsentBlock -> ConsentStepScreen()
        is MeterIndicator -> CrmUsageMeter(
            "Articles read this month",
            meterUsed,
            meterLimit,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        is SocialProof -> CrmText(
            block.text,
            style = CrmTheme.typography.bodySmall,
            color = CrmTheme.colors.onSurfaceVariant,
        )
    }
}

/**
 * VWE-05: adapts a [WallDefinition] (frontend model) to a [WallLayout] so the
 * preview panel can render via [WallLayoutRenderer] without going through the
 * domain [nl.incedo.paywall.walls.WallConfig].
 */
fun WallDefinition.toWallLayout(): WallLayout {
    val config = nl.incedo.paywall.walls.WallConfig(
        name = "",
        wallType = type.name.lowercase(),
        title = title,
        body = body,
        primaryCta = primaryCta,
        secondaryCta = secondaryCta,
        requireConsentStep = requireConsentStep,
        imageUrl = imageUrl,
        imageAlt = imageAlt,
        legalText = legalText,
    )
    val layout = config.toDefaultLayout()
    // Prepend MeterIndicator for metered walls (PW-22/23) after any ConsentBlock
    return if (type == WallType.Metered) {
        val consentIdx = layout.blocks.indexOfFirst { it is ConsentBlock }
        val insertAt = if (consentIdx >= 0) consentIdx + 1 else 0
        WallLayout(
            layout.blocks.toMutableList().also {
                it.add(insertAt, MeterIndicator(id = "meter"))
            },
        )
    } else {
        layout
    }
}
