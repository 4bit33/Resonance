package com.resonance.player.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Stitch bottom sheet: high-container surface, 24dp top-only radius,
 * 20dp sheet gutters. Content decides its own height.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResonanceBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = ResonanceTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = ResonanceTheme.radii.sheetTop,
        containerColor = colors.surfaceHigh,
        contentColor = colors.textPrimary,
        scrimColor = colors.background.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = ResonanceTheme.spacing.sheetGutter)) {
            content()
            Spacer(Modifier.height(ResonanceTheme.spacing.xl))
        }
    }
}

/** Stitch dialog: high-container surface, 16dp card radius. */
@Composable
fun ResonanceDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = typography.titleMd, color = colors.textPrimary) },
        text = { Text(text, style = typography.bodyMd, color = colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, style = typography.labelLg, color = colors.accent)
            }
        },
        dismissButton = if (dismissLabel != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel, style = typography.labelLg, color = colors.textSecondary)
                }
            }
        } else {
            null
        },
        shape = ResonanceTheme.radii.card,
        containerColor = colors.surfaceHigh,
        modifier = modifier
    )
}

/** Floating batch multi-action bar (selection count + actions). */
@Composable
fun ResonanceBatchActionBar(
    selectedCount: Int,
    countLabel: String,
    actions: List<BatchAction>,
    modifier: Modifier = Modifier
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.lg)
            .clip(ResonanceTheme.radii.card)
            .background(colors.surfaceHigh)
            .padding(spacing.md)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(ResonanceTheme.radii.full)
                .background(colors.accent)
        ) {
            Text(selectedCount.toString(), style = typography.labelLg, color = colors.onAccent)
        }
        Text(countLabel, style = typography.labelMd, color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        actions.forEach { action ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ResonanceIconButton(
                    onClick = action.onClick,
                    icon = action.icon,
                    contentDescription = action.label
                )
                Text(action.label, style = typography.labelSm, color = colors.textSecondary)
            }
        }
    }
}

data class BatchAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * Compact technical empty state (DESIGN.md: honest explanation, no oversized
 * illustration, single primary CTA). Replaces ad-hoc empty texts in new UI.
 */
@Composable
fun ResonanceEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.xxl)
    ) {
        Text(title, style = typography.titleMd, color = colors.textPrimary)
        Spacer(Modifier.height(spacing.sm))
        Text(
            body,
            style = typography.bodyMd,
            color = colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(spacing.lg))
            ResonancePrimaryButton(label = actionLabel, onClick = onAction)
        }
    }
}

/** Snackbar visuals in Stitch voice (high container, copper action). */
class ResonanceSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: androidx.compose.material3.SnackbarDuration =
        androidx.compose.material3.SnackbarDuration.Short
) : SnackbarVisuals

/** Snackbar content matching the Stitch toast language. */
@Composable
fun ResonanceSnackbar(snackbarData: SnackbarData, modifier: Modifier = Modifier) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    androidx.compose.material3.Snackbar(
        snackbarData = snackbarData,
        shape = ResonanceTheme.radii.control,
        containerColor = colors.surfaceHighest,
        contentColor = colors.textPrimary,
        actionColor = colors.accent,
        dismissActionContentColor = colors.textSecondary,
        modifier = modifier
    )
}
