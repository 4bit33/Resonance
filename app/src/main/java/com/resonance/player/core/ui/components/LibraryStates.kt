package com.resonance.player.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.resonance.player.R
import com.resonance.player.core.ui.theme.ResonanceTheme
import kotlinx.coroutines.delay

/**
 * Shared deterministic states. Layouts use constraints + dp spacing only —
 * no fixed screen coordinates, safe under font scaling and all form factors.
 */
@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

/**
 * "No music yet" with the two ways to add some. Used by Home and every Library
 * tab, so the CTA is one place.
 */
@Composable
fun EmptyLibraryView(modifier: Modifier = Modifier) {
    val actions = LocalMusicActions.current
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    // ponytail: the lists start empty until Room's first emission; waiting a beat keeps the CTA
    // from flashing on a library that does have songs. A real loading state per list replaces this.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(EMPTY_STATE_DELAY_MS)
        visible = true
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.xxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.empty_library_title),
                style = typography.titleMd,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = stringResource(R.string.empty_library_body),
                style = typography.bodyMd,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(spacing.lg))
            ResonancePrimaryButton(
                label = stringResource(R.string.music_add_folder),
                onClick = { actions.addFolder(null) }
            )
            Spacer(Modifier.height(spacing.sm))
            TextButton(onClick = actions.addSongs) {
                Text(
                    stringResource(R.string.music_add_songs),
                    style = typography.labelLg,
                    color = colors.accent
                )
            }
        }
    }
}

private const val EMPTY_STATE_DELAY_MS = 150L

@Composable
fun ErrorView(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.error_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}
