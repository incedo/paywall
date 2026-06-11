package nl.incedo.paywall.designer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import nl.incedo.paywall.model.Channel
import nl.incedo.paywall.model.WallStatus
import nl.incedo.paywall.model.WallSummary
import nl.incedo.paywall.model.demoWalls
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmTag
import nl.incedo.paywall.ui.CrmText

/** Walls overview (design "Wall Designer", variant B1): all walls in one table. */
@Composable
fun WallsOverviewScreen(onOpenWall: (WallSummary) -> Unit) {
    val walls = demoWalls
    Column(
        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xxs)) {
                CrmText("Walls", style = CrmTheme.typography.h1, color = CrmTheme.colors.onBackground)
                val live = walls.count { it.status == WallStatus.Live }
                val draft = walls.count { it.status == WallStatus.Draft }
                val paused = walls.count { it.status == WallStatus.Paused }
                CrmText(
                    "${walls.size} walls · $live live · $draft draft · $paused paused",
                    style = CrmTheme.typography.bodySmall,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
            }
            CrmPrimaryButton("New wall")
        }

        CrmCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = CrmTheme.spacing.lg,
                    vertical = CrmTheme.spacing.md,
                ),
            ) {
                HeaderCell("Name", 3f)
                HeaderCell("Type", 1f)
                HeaderCell("Channels", 1.5f)
                HeaderCell("Status", 1f)
                HeaderCell("A/B", 1f)
                HeaderCell("Conversion", 1f)
                HeaderCell("Updated", 1f)
            }
            CrmDivider()
            walls.forEachIndexed { index, wall ->
                WallRow(wall, onClick = { onOpenWall(wall) })
                if (index < walls.lastIndex) CrmDivider()
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    CrmText(
        text,
        style = CrmTheme.typography.label,
        color = CrmTheme.colors.onSurfaceVariant,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun WallRow(wall: WallSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrmText(wall.name, modifier = Modifier.weight(3f))
        CrmText(wall.type.label, color = CrmTheme.colors.onSurfaceVariant, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.weight(1.5f),
            horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
        ) {
            Channel.entries.filter { it in wall.channels }.forEach {
                CrmTag(it.short, CrmTheme.colors.surfaceVariant, CrmTheme.colors.onSurfaceVariant)
            }
        }
        Row(modifier = Modifier.weight(1f)) {
            StatusTag(wall.status)
        }
        CrmText(
            if (wall.variants > 1) "${wall.variants} variants" else "—",
            color = CrmTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        CrmText(wall.conversion, modifier = Modifier.weight(1f))
        CrmText(wall.updated, color = CrmTheme.colors.onSurfaceVariant, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatusTag(status: WallStatus) {
    when (status) {
        WallStatus.Live -> CrmTag(status.label, CrmTheme.colors.successContainer, CrmTheme.colors.success)
        WallStatus.Draft -> CrmTag(status.label, CrmTheme.colors.surfaceVariant, CrmTheme.colors.onSurfaceVariant)
        WallStatus.Paused -> CrmTag(status.label, CrmTheme.colors.warningContainer, CrmTheme.colors.onSurface)
    }
}
