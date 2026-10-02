package com.resonance.player.feature.home

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

/** Turn Home blocks on/off, reorder them, pick the card size. Saved on every change. */
@Composable
fun HomeEditorScreen(viewModel: HomeEditorViewModel, onBack: () -> Unit) {
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Column(Modifier.fillMaxSize()) {
        ResonanceTopBar(title = stringResource(R.string.home_customize), onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
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
            itemsIndexed(layout.sections, key = { _, setting -> setting.section.name }) { index, setting ->
                val name = homeSectionName(setting.section)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (setting.enabled) colors.surfaceContainer else colors.surfaceLow)
                        .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                ) {
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
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { viewModel.update { it.moved(setting.section, -1) } },
                        enabled = index > 0
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.cd_move_up, name))
                    }
                    IconButton(
                        onClick = { viewModel.update { it.moved(setting.section, 1) } },
                        enabled = index < layout.sections.lastIndex
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_move_down, name))
                    }
                }
            }
            item {
                TextButton(onClick = { viewModel.update { HomeLayout.Default } }) {
                    Text(stringResource(R.string.home_editor_reset))
                }
            }
        }
    }
}
