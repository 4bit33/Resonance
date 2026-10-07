package com.resonance.player.feature.home

import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.ReorderableItem
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.resonance.player.R
import com.resonance.player.core.ui.components.ResonanceSegmentedControl
import com.resonance.player.core.ui.components.ResonanceSwitch
import com.resonance.player.core.ui.components.ResonanceTopBar
import com.resonance.player.core.ui.theme.ResonanceTheme
import com.resonance.player.domain.settings.HomeLayout
import com.resonance.player.domain.settings.HomeSection
import com.resonance.player.domain.settings.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeEditorViewModel(private val settings: UserPreferencesRepository) : ViewModel() {
    val layout: StateFlow<HomeLayout> = settings.homeLayout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeLayout.Default)

    fun update(transform: (HomeLayout) -> HomeLayout) {
        viewModelScope.launch { settings.updateHomeLayout(transform) }
    }
}

@Composable
fun homeSectionName(section: HomeSection): String = stringResource(
    when (section) {
        HomeSection.GREETING -> R.string.home_section_greeting
        HomeSection.SEARCH -> R.string.home_section_search
        HomeSection.CONTINUE -> R.string.home_section_continue
        HomeSection.RECENTLY_PLAYED -> R.string.home_section_recently_played
        HomeSection.RECENTLY_ADDED -> R.string.home_section_recently_added
        HomeSection.GENRES -> R.string.home_section_genres
        HomeSection.PLAYLISTS -> R.string.home_section_playlists
        HomeSection.STATS -> R.string.home_section_stats
        HomeSection.IMPORT -> R.string.home_section_import
        HomeSection.QUICK_ACTIONS -> R.string.home_section_quick_actions
        HomeSection.MOST_PLAYED -> R.string.home_section_most_played
    }
)

/** Turn Home blocks on/off and drag them into order (long-press a row, or the handle). Saved on every change. */
@Composable
fun HomeEditorScreen(viewModel: HomeEditorViewModel, onBack: () -> Unit) {
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    val haptics = LocalHapticFeedback.current
    var order by remember { mutableStateOf(layout.sections) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(layout) { if (!dragging) order = layout.sections }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = order.indexOfFirst { it.section.name == from.key }
        val toIndex = order.indexOfFirst { it.section.name == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            order = order.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    fun commit() {
        dragging = false
        val sections = order.map { it.section }
        viewModel.update { it.reordered(sections) }
    }
    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(title = stringResource(R.string.home_customize), onBack = onBack)
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "intro") {
                Text(
                    stringResource(R.string.home_editor_body),
                    style = typography.bodyMd,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    stringResource(R.string.home_editor_cards),
                    style = typography.titleMd,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ResonanceSegmentedControl(
                    options = listOf(stringResource(R.string.home_editor_compact), stringResource(R.string.home_editor_large)),
                    selectedIndex = if (layout.compact) 0 else 1,
                    onSelect = { index -> viewModel.update { it.copy(compact = index == 0) } },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            items(order, key = { it.section.name }) { setting ->
                ReorderableItem(reorder, key = setting.section.name) { isDragging ->
                    val name = homeSectionName(setting.section)
                    val lift by animateDpAsState(if (isDragging) 12.dp else 0.dp, ResonanceTheme.motion.spatialFast(), label = "lift")
                    val scale by animateFloatAsState(if (isDragging) 1.04f else 1f, ResonanceTheme.motion.expressive(), label = "scale")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .shadow(lift, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    isDragging -> colors.surfaceHighest
                                    setting.enabled -> colors.surfaceContainer
                                    else -> colors.surfaceLow
                                }
                            )
                            .longPressDraggableHandle(
                                onDragStarted = {
                                    dragging = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = { commit() }
                            )
                            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DragIndicator,
                            contentDescription = stringResource(R.string.cd_drag_named, name),
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(12.dp)
                                .draggableHandle(
                                    onDragStarted = {
                                        dragging = true
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = { commit() }
                                )
                        )
                        Text(
                            name,
                            style = typography.titleMd,
                            color = if (setting.enabled) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        ResonanceSwitch(
                            checked = setting.enabled,
                            onCheckedChange = { viewModel.update { it.toggled(setting.section) } }
                        )
                    }
                }
            }
            item(key = "reset") {
                TextButton(onClick = { viewModel.update { HomeLayout.Default } }) {
                    Text(stringResource(R.string.home_editor_reset))
                }
            }
        }
    }
}
