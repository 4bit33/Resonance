package com.resonance.player.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.ui.theme.ResonanceTheme

/**
 * Shared scan progress/error banner (Library + Home). Determinate while the
 * scanner reports totals, failed-count note on completion, error otherwise.
 */
@Composable
fun ScanProgressBanner(state: ScanState, modifier: Modifier = Modifier) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    when (state) {
        is ScanState.Scanning -> {
            Column(modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm)) {
                Text(stringResource(R.string.scan_scanning), style = typography.bodySm)
                Spacer(Modifier.height(spacing.xs))
                val progress = if (state.total > 0) {
                    state.processed.toFloat() / state.total.toFloat()
                } else {
                    null
                }
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        is ScanState.Completed -> {
            if (state.report.failed > 0) {
                Text(
                    text = stringResource(R.string.scan_failed_note),
                    style = typography.bodySm,
                    color = colors.error,
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs)
                )
            }
        }
        is ScanState.Failed -> {
            Text(
                text = state.error.userMessage(),
                style = typography.bodySm,
                color = colors.error,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs)
            )
        }
        else -> Unit
    }
}
