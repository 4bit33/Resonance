package com.resonance.player.feature.library

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.resonance.player.R
import com.resonance.player.core.common.Result
import com.resonance.player.core.common.formatDurationMs
import com.resonance.player.core.model.ShuffleMode
import com.resonance.player.core.model.Song
import com.resonance.player.core.ui.components.ArtworkImage
import com.resonance.player.core.ui.components.EqualizerBars
import com.resonance.player.core.ui.components.pressClickable
import com.resonance.player.core.ui.theme.ArtworkPalette
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.core.ui.theme.rememberArtworkPalette
import com.resonance.player.domain.library.CollectionRef
import com.resonance.player.domain.library.GetAlbumSongsUseCase
import com.resonance.player.domain.library.GetArtistSongsUseCase
import com.resonance.player.domain.library.GetFolderSongsUseCase
import com.resonance.player.domain.library.GetGenreSongsUseCase
import com.resonance.player.domain.playback.PlaySongsUseCase
import com.resonance.player.domain.playback.SetShuffleModeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CollectionViewModel(
    val ref: CollectionRef,
    private val getAlbumSongs: GetAlbumSongsUseCase,
    private val getArtistSongs: GetArtistSongsUseCase,
    private val getGenreSongs: GetGenreSongsUseCase,
    private val getFolderSongs: GetFolderSongsUseCase,
    private val playSongs: PlaySongsUseCase,
    private val setShuffleMode: SetShuffleModeUseCase
) : ViewModel() {
    /** null while loading. */
    private val songsMutable = MutableStateFlow<List<Song>?>(null)
    val songs: StateFlow<List<Song>?> = songsMutable.asStateFlow()

    init {
        viewModelScope.launch {
            val result = when (ref) {
                is CollectionRef.Album -> getAlbumSongs(ref.name, ref.albumArtist)
                is CollectionRef.Artist -> getArtistSongs(ref.name)
                is CollectionRef.Genre -> getGenreSongs(ref.name)
                is CollectionRef.Folder -> getFolderSongs(ref.path.ifBlank { null })
            }
            songsMutable.value = (result as? Result.Success)?.value.orEmpty()
        }
    }

    fun playFrom(index: Int) {
        val list = songs.value ?: return
        viewModelScope.launch { playSongs(list, index) }
    }

    fun playAll(shuffled: Boolean) {
        val list = songs.value.orEmpty()
        if (list.isEmpty()) return
        viewModelScope.launch {
            playSongs(list, if (shuffled) list.indices.random() else 0)
            setShuffleMode(if (shuffled) ShuffleMode.ON else ShuffleMode.OFF)
        }
    }
}

/**
 * An album, artist, genre or folder: cover with its glow, Play / Shuffle,
 * then the songs. Albums list track numbers; the others show each cover.
 */
@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel,
    currentSongId: Long?,
    onBack: () -> Unit,
    onSongClick: (Long) -> Unit
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val ref = viewModel.ref
    val list = songs.orEmpty()
    val cover = list.firstNotNullOfOrNull { it.artworkUri }
    val palette = rememberArtworkPalette(cover, ResonanceTheme.look.artworkColors)
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val isAlbum = ref is CollectionRef.Album

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        0f to palette.background,
                        0.35f to palette.background.copy(alpha = 0.6f),
                        0.75f to colors.background,
                        endY = size.height
                    )
                )
            }
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
            item(key = "top") {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, top = 4.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = colors.textPrimary)
                    }
                }
            }
            item(key = "header") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    CollectionCover(ref, cover, palette)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        title(ref),
                        style = typography.headlineLg,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitle(ref, list),
                        style = typography.bodyMd,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionPill(
                            Icons.Rounded.PlayArrow,
                            stringResource(R.string.cd_play),
                            filled = true,
                            accent = palette.accent,
                            enabled = list.isNotEmpty()
                        ) { viewModel.playAll(shuffled = false) }
                        ActionPill(
                            Icons.Rounded.Shuffle,
                            stringResource(R.string.cd_shuffle),
                            filled = false,
                            accent = palette.accent,
                            enabled = list.isNotEmpty()
                        ) { viewModel.playAll(shuffled = true) }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            itemsIndexed(list, key = { _, song -> song.id }) { index, song ->
                CollectionSongRow(
                    song = song,
                    leadingNumber = if (isAlbum) (song.trackNumber ?: (index + 1)) else null,
                    isCurrent = song.id == currentSongId,
                    accent = palette.accent,
                    showArtist = ref !is CollectionRef.Artist
                ) {
                    viewModel.playFrom(index)
                    onSongClick(song.id)
                }
            }
        }
    }
}

@Composable
private fun title(ref: CollectionRef): String = when (ref) {
    is CollectionRef.Album -> ref.name
    is CollectionRef.Artist -> ref.name
    is CollectionRef.Genre -> ref.name
    is CollectionRef.Folder -> ref.name.ifBlank { stringResource(R.string.tab_folders) }
}

@Composable
private fun subtitle(ref: CollectionRef, songs: List<Song>): String {
    val count = pluralStringResource(R.plurals.music_song_count, songs.size, songs.size)
    val duration = formatDurationMs(songs.sumOf { it.durationMs })
    val lead = when (ref) {
        is CollectionRef.Album -> listOfNotNull(
            ref.albumArtist ?: songs.map { it.artistName }.distinct().singleOrNull(),
            songs.mapNotNull { it.year }.maxOrNull()?.toString()
        ).joinToString(" · ")
        is CollectionRef.Artist -> pluralStringResource(
            R.plurals.album_count,
            songs.map { it.albumName }.distinct().size,
            songs.map { it.albumName }.distinct().size
        )
        is CollectionRef.Genre -> stringResource(R.string.collection_genre)
        is CollectionRef.Folder -> ref.path.ifBlank { stringResource(R.string.collection_folder) }
    }
    return listOf(lead, count, duration).filter { it.isNotBlank() }.joinToString(" · ")
}

/** Cover with the same glow as Now Playing; artists get a round avatar, folders an icon if they have no art. */
@Composable
private fun CollectionCover(ref: CollectionRef, cover: String?, palette: ArtworkPalette) {
    val shape: Shape = if (ref is CollectionRef.Artist) CircleShape else RoundedCornerShape(24.dp)
    val glowStrength = ResonanceTheme.look.glowStrength
    Box(Modifier.size(208.dp), contentAlignment = Alignment.Center) {
        if (glowStrength > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1.12f
                        scaleY = 1.12f
                        alpha = glowStrength
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .drawBehind {
                            val radius = size.minDimension * 0.75f
                            drawCircle(
                                Brush.radialGradient(listOf(palette.glow.copy(alpha = 0.7f), Color.Transparent), center, radius),
                                radius
                            )
                        }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && cover != null) {
                    ArtworkImage(
                        artworkUri = cover,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().blur(64.dp, BlurredEdgeTreatment.Unbounded)
                    )
                }
            }
        }
        Box(Modifier.matchParentSize().clip(shape), contentAlignment = Alignment.Center) {
            if (cover == null && ref is CollectionRef.Folder) {
                Box(Modifier.matchParentSize().background(ResonanceTheme.colors.surfaceHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = palette.accent, modifier = Modifier.size(72.dp))
                }
            } else {
                ArtworkImage(artworkUri = cover, contentDescription = null, modifier = Modifier.matchParentSize())
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    filled: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = ResonanceTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(if (filled) accent else colors.textPrimary.copy(alpha = 0.08f))
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .pressClickable(onClick = { if (enabled) onClick() })
            .padding(horizontal = 22.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) colors.onAccent else colors.textPrimary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = ResonanceTheme.typography.labelLg, color = if (filled) colors.onAccent else colors.textPrimary)
    }
}

@Composable
private fun CollectionSongRow(
    song: Song,
    leadingNumber: Int?,
    isCurrent: Boolean,
    accent: Color,
    showArtist: Boolean,
    onClick: () -> Unit
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .pressClickable(pressedScale = 0.98f, onClickLabel = song.title, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        if (leadingNumber != null) {
            Box(Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
                if (isCurrent) {
                    EqualizerBars(isPlaying = true, modifier = Modifier.size(16.dp))
                } else {
                    Text(leadingNumber.toString(), style = typography.monoMetric, color = colors.textSecondary)
                }
            }
        } else {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))) {
                ArtworkImage(artworkUri = song.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = typography.titleMd,
                color = if (isCurrent) accent else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showArtist) {
                Text(song.artistName, style = typography.bodySm, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(formatDurationMs(song.durationMs), style = typography.monoMetric, color = colors.textSecondary)
    }
}
