package com.resonance.player.feature.player

import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import android.content.Context
import android.content.Intent
import android.media.MediaRouter2
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonance.player.R
import com.resonance.player.core.common.formatBytes
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.media.AudioOutput
import com.resonance.player.core.media.AudioOutputKind
import com.resonance.player.core.media.observeAudioOutput
import com.resonance.player.core.model.RepeatMode
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.LocalMusicActions
import com.resonance.player.core.ui.components.ResonanceBottomSheet
import com.resonance.player.core.ui.components.ResonanceMetric
import com.resonance.player.core.ui.components.shouldDismiss
import com.resonance.player.core.ui.theme.ArtworkPalette
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.core.ui.theme.rememberArtworkPalette
import com.resonance.player.domain.playback.PlaybackSource
import kotlin.math.abs
import kotlinx.coroutines.delay

private val ArtworkShape = RoundedCornerShape(28.dp)

/**
 * Now Playing. Fixed layout (no scrolling): the artwork and its glow lead,
 * colors come from the artwork, every gesture follows the finger.
 *
 * - Pull down anywhere: the screen follows and closes past a threshold or on a flick.
 * - Drag the artwork sideways: it follows, then flies out to skip (or springs back).
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onOpenQueue: () -> Unit,
    onBack: () -> Unit,
    /** Vertical drag on the screen: shrinks Now Playing back into the mini player (down is positive). */
    onCollapseDrag: (Float) -> Unit = {},
    onCollapseDragStopped: suspend (velocity: Float) -> Unit = {},
    /** Where the big cover sits, for the cover flying in from the mini player. */
    onArtworkBounds: (Rect) -> Unit = {},
    /** 0 while the flying cover stands in for the real one. Read in the draw phase. */
    coverAlpha: () -> Float = { 1f }
) {
    val colors = ResonanceTheme.colors
    val motion = ResonanceTheme.motion
    val look = ResonanceTheme.look
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val details by viewModel.details.collectAsStateWithLifecycle()
    val commandError by viewModel.commandError.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val source by viewModel.source.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val output by remember(context) { observeAudioOutput(context.applicationContext) }
        .collectAsStateWithLifecycle(initialValue = null)
    val haptics = LocalHapticFeedback.current

    val song = snapshot.song ?: details
    val hasQueue = snapshot.queue.isNotEmpty()
    val palette = rememberArtworkPalette(song?.artworkUri, look.artworkColors)
    var techSheetOpen by remember { mutableStateOf(false) }

    // Pulling down shrinks the player back into the mini player, following the finger (NowPlayingSheet).
    val collapseDrag by rememberUpdatedState(onCollapseDrag)
    val pullState = rememberDraggableState { delta -> collapseDrag(delta) }

    Box(
        Modifier
            .fillMaxSize()
            .draggable(
                state = pullState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> onCollapseDragStopped(velocity) }
            )
            .background(palette.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            PlayerTopBar(
                label = sourceLabel(source),
                name = source?.name ?: song?.albumName,
                onClose = onBack,
                onMore = { techSheetOpen = true },
                moreEnabled = song != null
            )
            Spacer(Modifier.weight(0.5f))
            Artwork(
                song = song,
                palette = palette,
                playing = snapshot.isPlaying,
                onNext = viewModel::onNext,
                onPrevious = viewModel::onPrevious,
                onBounds = onArtworkBounds,
                coverAlpha = coverAlpha,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.weight(0.7f))
            TitleRow(
                song = song,
                isFavorite = isFavorite,
                accent = palette.accent,
                onToggleFavorite = viewModel::onToggleFavorite
            )
            Spacer(Modifier.height(20.dp))
            Scrubber(
                positionMs = snapshot.positionMs,
                durationMs = snapshot.durationMs,
                // While buffering the position is not moving: do not extrapolate it.
                playing = snapshot.isPlaying && !snapshot.isBuffering,
                enabled = hasQueue && snapshot.durationMs > 0L,
                accent = palette.accent,
                onSeek = viewModel::onSeek
            )
            Spacer(Modifier.weight(0.6f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                ToggleButton(
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = stringResource(R.string.cd_shuffle),
                    active = snapshot.shuffle == ShuffleMode.ON,
                    accent = palette.accent,
                    onClick = { viewModel.onToggleShuffle(snapshot.shuffle) }
                )
                TransportButton(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous),
                    enabled = hasQueue,
                    onClick = viewModel::onPrevious
                )
                PlayPauseButton(
                    playing = snapshot.isPlaying,
                    enabled = hasQueue,
                    accent = palette.accent,
                    onClick = viewModel::onTogglePlayPause
                )
                TransportButton(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.cd_next),
                    enabled = hasQueue,
                    onClick = viewModel::onNext
                )
                ToggleButton(
                    icon = if (snapshot.repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = stringResource(R.string.cd_repeat),
                    active = snapshot.repeat != RepeatMode.OFF,
                    accent = palette.accent,
                    onClick = { viewModel.onCycleRepeat(snapshot.repeat) }
                )
            }
            val visibleError = commandError ?: snapshot.error
            if (visibleError != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = visibleError.userMessage(),
                        style = ResonanceTheme.typography.bodySm,
                        color = colors.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = viewModel::clearCommandError) {
                        Text(stringResource(R.string.action_dismiss))
                    }
                }
            }
            Spacer(Modifier.weight(0.6f))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutputChip(output = output, accent = palette.accent, onClick = { openOutputSwitcher(context) })
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenQueue, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = stringResource(R.string.nav_queue),
                        tint = colors.textSecondary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (techSheetOpen && song != null) {
        val actions = LocalMusicActions.current
        ResonanceBottomSheet(onDismiss = { techSheetOpen = false }) {
            TextButton(onClick = {
                techSheetOpen = false
                actions.editGenre(listOf(song)) {}
            }) {
                Text(stringResource(R.string.genre_edit_action), color = palette.accent)
            }
            TechSheetContent(title = song.title, rows = techRows(song))
        }
    }
}

@Composable
private fun PlayerTopBar(label: String, name: String?, onClose: () -> Unit, onMore: () -> Unit, moreEnabled: Boolean) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        IconButton(onClick = onClose) {
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_close_player),
                tint = colors.textPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                style = typography.labelSm,
                color = colors.textSecondary,
                maxLines = 1
            )
            if (!name.isNullOrBlank()) {
                Text(
                    name,
                    style = typography.labelLg,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onMore, enabled = moreEnabled) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_more), tint = colors.textPrimary)
        }
    }
}

@Composable
private fun sourceLabel(source: PlaybackSource?): String = stringResource(
    when (source?.kind) {
        null -> R.string.nav_player
        PlaybackSource.Kind.LIBRARY -> R.string.nav_library
        PlaybackSource.Kind.ALBUM -> R.string.source_album
        PlaybackSource.Kind.ARTIST -> R.string.source_artist
        PlaybackSource.Kind.GENRE -> R.string.collection_genre
        PlaybackSource.Kind.FOLDER -> R.string.collection_folder
        PlaybackSource.Kind.PLAYLIST -> R.string.source_playlist
        PlaybackSource.Kind.FAVORITES -> R.string.home_favorites
        PlaybackSource.Kind.SEARCH -> R.string.nav_search
        PlaybackSource.Kind.MOST_PLAYED -> R.string.home_most_played
        PlaybackSource.Kind.SHUFFLE_ALL -> R.string.home_shuffle_all
    }
)

/**
 * The cover with its glow. The glow is the cover itself, blurred (Android 12+),
 * over a soft halo in the palette's glow color (every version), so its color
 * always matches the picture. It breathes slowly while music plays.
 */
@Composable
private fun Artwork(
    song: Song?,
    palette: ArtworkPalette,
    playing: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onBounds: (Rect) -> Unit,
    coverAlpha: () -> Float,
    modifier: Modifier = Modifier
) {
    val motion = ResonanceTheme.motion
    val look = ResonanceTheme.look
    val haptics = LocalHapticFeedback.current
    var widthPx by remember { mutableFloatStateOf(1f) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val dragState = rememberDraggableState { delta -> dragX += delta }

    // Animated values below are read only inside graphicsLayer {} blocks: they
    // re-draw the layer each frame without recomposing (which made opening lag).
    val pausedScale = animateFloatAsState(if (playing) 1f else 0.86f, motion.expressive(), label = "art-scale")
    // The glow blooms in once Now Playing has slid up: the blur is not computed during the open animation.
    var bloomed by remember { mutableStateOf(!motion.enabled) }
    LaunchedEffect(Unit) {
        delay(motion.millis(280).toLong())
        bloomed = true
    }
    val glowTarget = if (bloomed) look.glowStrength * if (playing) 1f else 0.55f else 0f
    val glowAlpha = animateFloatAsState(glowTarget, motion.duration(if (bloomed) 700 else 0), label = "glow-alpha")
    val breathing = look.glowBreathing && playing && motion.enabled
    val breath = if (breathing) {
        rememberInfiniteTransition(label = "glow-breath").animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                tween(motion.millis(2400).coerceAtLeast(1), easing = FastOutSlowInEasing),
                AnimationRepeatMode.Reverse
            ),
            label = "glow-breath-scale"
        )
    } else {
        null
    }

    Box(
        modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp)
            .aspectRatio(1f)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .graphicsLayer {
                translationX = dragX
                rotationZ = dragX / widthPx * 5f
                alpha = 1f - (abs(dragX) / widthPx * 0.7f).coerceIn(0f, 0.7f)
                // An offscreen layer would be clipped to the artwork bounds and cut the glow.
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    val next = dragX < -widthPx * 0.28f || (velocity < -1400f && dragX < 0f)
                    val previous = dragX > widthPx * 0.28f || (velocity > 1400f && dragX > 0f)
                    if (next || previous) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val direction = if (next) -1f else 1f
                        animate(dragX, direction * widthPx * 1.1f, animationSpec = motion.spatialFast()) { v, _ -> dragX = v }
                        if (next) onNext() else onPrevious()
                        dragX = -direction * widthPx * 0.5f
                    }
                    animate(dragX, 0f, animationSpec = motion.spatial()) { v, _ -> dragX = v }
                }
            )
    ) {
        if (glowTarget > 0f || glowAlpha.value > 0.01f) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val s = pausedScale.value * (breath?.value ?: 1f) * 1.1f
                        scaleX = s
                        scaleY = s
                        alpha = glowAlpha.value
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .drawBehind {
                            val radius = size.minDimension * 0.78f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(palette.glow.copy(alpha = 0.75f), Color.Transparent),
                                    center = center,
                                    radius = radius
                                ),
                                radius = radius
                            )
                        }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && song != null) {
                    ArtworkImage(
                        artworkUri = song.artworkUri,
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .blur(60.dp, BlurredEdgeTreatment.Unbounded)
                    )
                }
            }
        }
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = pausedScale.value
                    scaleY = pausedScale.value
                    alpha = coverAlpha()
                }
                .shadow(28.dp, ArtworkShape, ambientColor = palette.glow, spotColor = palette.glow)
                .clip(ArtworkShape)
        ) {
            ArtworkImage(
                artworkUri = song?.artworkUri,
                contentDescription = song?.albumName,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TitleRow(song: Song?, isFavorite: Boolean, accent: Color, onToggleFavorite: () -> Unit) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val motion = ResonanceTheme.motion
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = song,
            contentKey = { it?.id },
            transitionSpec = {
                (fadeIn(motion.duration(260)) + slideInVertically(motion.spatial()) { it / 3 }) togetherWith
                    fadeOut(motion.duration(120))
            },
            label = "title",
            modifier = Modifier.weight(1f)
        ) { shown ->
            Column {
                Text(
                    shown?.title ?: stringResource(R.string.empty_library_title),
                    style = typography.headlineLg,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(initialDelayMillis = 2000)
                )
                if (shown != null) {
                    Text(
                        shown.artistName,
                        style = typography.bodyLg,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (song != null) {
            val pop by animateFloatAsState(if (isFavorite) 1f else 0.9f, motion.expressive(), label = "fav-pop")
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (isFavorite) accent else colors.textSecondary,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = pop
                            scaleY = pop
                        }
                )
            }
        }
    }
}

/**
 * Seek bar. The position is extrapolated every frame between the player's
 * ticks, so the bar glides instead of stepping. Tap or drag to seek; the bar
 * thickens under the finger.
 */
@Composable
private fun Scrubber(
    positionMs: Long,
    durationMs: Long,
    playing: Boolean,
    enabled: Boolean,
    accent: Color,
    onSeek: (Long) -> Unit
) {
    val colors = ResonanceTheme.colors
    val motion = ResonanceTheme.motion
    var anchorMs by remember { mutableLongStateOf(positionMs) }
    var anchorAt by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var shownMs by remember { mutableLongStateOf(positionMs) }
    LaunchedEffect(positionMs) {
        anchorMs = positionMs
        anchorAt = SystemClock.uptimeMillis()
    }
    LaunchedEffect(playing, durationMs) {
        if (!playing) return@LaunchedEffect
        while (true) {
            withFrameMillis {
                shownMs = (anchorMs + SystemClock.uptimeMillis() - anchorAt).coerceIn(0L, durationMs.coerceAtLeast(0L))
            }
        }
    }
    fun currentMs(): Long = if (playing) shownMs else anchorMs
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val duration = durationMs.coerceAtLeast(1L)
    fun currentFraction(): Float = dragFraction ?: (currentMs().toFloat() / duration).coerceIn(0f, 1f)
    // The bar is drawn every frame from the states above, read only while drawing;
    // the time labels recompose once per second.
    val shownSeconds by remember(playing, duration) {
        derivedStateOf { (dragFraction?.let { (it * duration).toLong() } ?: currentMs()) / 1000L }
    }
    val active = dragFraction != null
    val trackHeight by animateDpAsState(if (active) 8.dp else 5.dp, motion.spatialFast(), label = "track-h")
    val thumbRadius by animateDpAsState(if (active) 10.dp else 7.dp, motion.spatialFast(), label = "thumb-r")

    val seekDescription = stringResource(R.string.cd_seek)

    fun seekTo(f: Float) {
        val target = (f.coerceIn(0f, 1f) * duration).toLong()
        anchorMs = target
        anchorAt = SystemClock.uptimeMillis()
        shownMs = target
        onSeek(target)
    }

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .semantics {
                    contentDescription = seekDescription
                    stateDescription = formatDurationMs(shownSeconds * 1000L) + " / " + formatDurationMs(durationMs)
                }
                .pointerInput(enabled, duration) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset -> seekTo(offset.x / size.width) }
                }
                .pointerInput(enabled, duration) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> dragFraction = (offset.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            dragFraction?.let(::seekTo)
                            dragFraction = null
                        },
                        onDragCancel = { dragFraction = null }
                    ) { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                }
        ) {
            val trackColor = colors.textPrimary.copy(alpha = 0.16f)
            val thumbColor = colors.textPrimary
            Canvas(Modifier.matchParentSize()) {
                val h = trackHeight.toPx()
                val top = (size.height - h) / 2f
                val radius = CornerRadius(h / 2f, h / 2f)
                drawRoundRect(trackColor, Offset(0f, top), Size(size.width, h), radius)
                val fraction = currentFraction()
                drawRoundRect(accent, Offset(0f, top), Size(size.width * fraction, h), radius)
                if (enabled) {
                    drawCircle(thumbColor, thumbRadius.toPx(), Offset(size.width * fraction, size.height / 2f))
                }
            }
        }
        Row {
            ResonanceMetric(text = formatDurationMs(shownSeconds * 1000L))
            Spacer(Modifier.weight(1f))
            ResonanceMetric(text = formatDurationMs(durationMs))
        }
    }
}

/** Play/pause. Circle when paused, rounded square while playing; squeezes under the finger. */
@Composable
private fun PlayPauseButton(playing: Boolean, enabled: Boolean, accent: Color, onClick: () -> Unit) {
    val colors = ResonanceTheme.colors
    val motion = ResonanceTheme.motion
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, motion.expressive(), label = "play-press")
    val corner by animateDpAsState(if (playing) 26.dp else 42.dp, motion.expressive(), label = "play-shape")
    val label = stringResource(if (playing) R.string.cd_pause else R.string.cd_play)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(corner))
            .background(if (enabled) accent else colors.surfaceHighest)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = colors.onAccent),
                enabled = enabled,
                role = Role.Button,
                onClickLabel = label
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .semantics { contentDescription = label }
    ) {
        AnimatedContent(
            targetState = playing,
            transitionSpec = {
                (fadeIn(motion.duration(140)) + scaleIn(motion.expressive(), initialScale = 0.5f)) togetherWith
                    (fadeOut(motion.duration(90)) + scaleOut(motion.spatialFast(), targetScale = 0.5f))
            },
            label = "play-icon"
        ) { isPlaying ->
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (enabled) colors.onAccent else colors.textMuted,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun TransportButton(icon: ImageVector, contentDescription: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = ResonanceTheme.colors
    val motion = ResonanceTheme.motion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.82f else 1f, motion.expressive(), label = "transport-press")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription }
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) colors.textPrimary else colors.textMuted,
            modifier = Modifier.size(40.dp)
        )
    }
}

/** Shuffle/repeat: accent + a dot under the icon when on. */
@Composable
private fun ToggleButton(icon: ImageVector, contentDescription: String, active: Boolean, accent: Color, onClick: () -> Unit) {
    val colors = ResonanceTheme.colors
    val motion = ResonanceTheme.motion
    val dot by animateFloatAsState(if (active) 1f else 0f, motion.expressive(), label = "toggle-dot")
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (active) accent else colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Box(
                Modifier
                    .padding(top = 30.dp)
                    .size(4.dp)
                    .graphicsLayer {
                        scaleX = dot
                        scaleY = dot
                        alpha = dot
                    }
                    .background(accent, CircleShape)
            )
        }
    }
}

/** Where the sound goes ("Nothing Ear"). Tapping opens the system output switcher. */
@Composable
private fun OutputChip(output: AudioOutput?, accent: Color, onClick: () -> Unit) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val name = when (output?.kind) {
        null -> return
        AudioOutputKind.SPEAKER -> stringResource(R.string.output_speaker)
        AudioOutputKind.BLUETOOTH -> output.name ?: stringResource(R.string.output_bluetooth)
        AudioOutputKind.USB -> output.name ?: stringResource(R.string.output_usb)
        AudioOutputKind.WIRED -> stringResource(R.string.output_headphones)
    }
    val icon = when (output.kind) {
        AudioOutputKind.SPEAKER -> Icons.Rounded.Smartphone
        AudioOutputKind.USB -> Icons.Rounded.Usb
        else -> Icons.Rounded.Headphones
    }
    val description = stringResource(R.string.cd_audio_output, name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(colors.textPrimary.copy(alpha = 0.08f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(name, style = typography.labelLg, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** System media output picker (Android 14+), the SystemUI dialog on 11-13, else Bluetooth settings. */
private fun openOutputSwitcher(context: Context) {
    if (Build.VERSION.SDK_INT >= 34 &&
        runCatching { MediaRouter2.getInstance(context).showSystemOutputSwitcher() }.getOrDefault(false)
    ) {
        return
    }
    if (Build.VERSION.SDK_INT in 30..33) {
        val sent = runCatching {
            context.sendBroadcast(
                Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
                    .setPackage("com.android.systemui")
                    .putExtra("package_name", context.packageName)
            )
        }.isSuccess
        if (sent) return
    }
    runCatching {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun albumLine(album: String, year: Int?): String =
    if (year != null && year > 0) "$album ($year)" else album

private data class TechRow(val label: String, val value: String)

/**
 * Technical sheet rows from REAL file data only. Channels and bit depth
 * are unavailable on-device (no MediaStore column, no retriever key), so
 * they are omitted rather than fabricated.
 */
private fun techRows(song: Song): List<TechRow> {
    val rows = ArrayList<TechRow>()
    rows.add(TechRow("Title", song.title))
    rows.add(TechRow("Artist", song.artistName))
    rows.add(TechRow("Album", albumLine(song.albumName, song.year)))
    song.albumArtist?.let { rows.add(TechRow("Album artist", it)) }
    song.genreName?.let { rows.add(TechRow("Genre", it)) }
    containerName(song.mimeType)?.let { rows.add(TechRow("Container", it)) }
    song.mimeType?.let { rows.add(TechRow("MIME type", it)) }
    if (song.bitrate != null && song.bitrate > 0) {
        rows.add(TechRow("Bitrate", "${song.bitrate / 1000} kbps"))
    }
    if (song.sampleRate != null && song.sampleRate > 0) {
        val khz = song.sampleRate / 1000.0
        val formatted = if (khz % 1.0 == 0.0) {
            khz.toLong().toString()
        } else {
            khz.toString()
        }
        rows.add(TechRow("Sample rate", "$formatted kHz"))
    }
    song.trackNumber?.let { track ->
        val total = song.totalTracks?.let { " / $it" } ?: ""
        rows.add(TechRow("Track", "$track$total"))
    }
    song.discNumber?.let { disc ->
        val total = song.totalDiscs?.let { " / $it" } ?: ""
        rows.add(TechRow("Disc", "$disc$total"))
    }
    rows.add(TechRow("Duration", formatDurationMs(song.durationMs)))
    rows.add(TechRow("Size", formatBytes(song.fileSizeBytes)))
    rows.add(TechRow("Location", song.path))
    return rows
}

private fun containerName(mimeType: String?): String? {
    if (mimeType == null) return null
    return when {
        "flac" in mimeType -> "FLAC"
        "mp4" in mimeType || "m4a" in mimeType || "aac" in mimeType -> "MPEG-4 Audio"
        "ogg" in mimeType || "opus" in mimeType -> "Ogg"
        "wav" in mimeType || "x-wav" in mimeType -> "WAV"
        "mpeg" in mimeType -> "MP3"
        else -> null
    }
}

@Composable
private fun TechSheetContent(title: String, rows: List<TechRow>) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val spacing = ResonanceTheme.spacing
    Column {
        Text(title, style = typography.titleMd, color = colors.textPrimary)
        Spacer(Modifier.height(spacing.md))
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xs)
            ) {
                ResonanceMetric(text = row.label, color = colors.textSecondary)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(spacing.lg))
                Text(
                    row.value,
                    style = typography.bodyMd,
                    color = colors.textPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
