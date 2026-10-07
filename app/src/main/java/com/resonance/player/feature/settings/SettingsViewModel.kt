package com.resonance.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.media.ScanState
import com.resonance.player.core.model.LibraryStats
import com.resonance.player.core.model.MusicSource
import com.resonance.player.data.local.LibraryPreferences
import com.resonance.player.domain.library.GetLibraryStatsUseCase
import com.resonance.player.domain.library.ObserveLastScanUseCase
import com.resonance.player.domain.library.ObserveScanStateUseCase
import com.resonance.player.domain.library.ObserveSourcesUseCase
import com.resonance.player.domain.library.RemoveSourcesUseCase
import com.resonance.player.domain.library.RescanLibraryUseCase
import com.resonance.player.core.ui.theme.DEFAULT_ACCENT_HUE
import com.resonance.player.domain.settings.ThemeMode
import com.resonance.player.domain.settings.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Settings: theme (DataStore) + music sources + library section (stats, refresh). */
class SettingsViewModel(
    private val repository: UserPreferencesRepository,
    observeScanState: ObserveScanStateUseCase,
    private val rescanLibrary: RescanLibraryUseCase,
    observeSources: ObserveSourcesUseCase,
    private val removeSourcesUseCase: RemoveSourcesUseCase,
    private val getLibraryStats: GetLibraryStatsUseCase,
    observeLastScan: ObserveLastScanUseCase,
    private val libraryPreferences: LibraryPreferences
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val accentHue: StateFlow<Float> = repository.accentHue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_ACCENT_HUE)

    val scanState: StateFlow<ScanState> = observeScanState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanState.Idle)

    val lastScan: StateFlow<Long?> = observeLastScan()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sources: StateFlow<List<MusicSource>> = observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ignoreShortFiles: StateFlow<Boolean> = libraryPreferences.ignoreShortFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val statsMutable = MutableStateFlow<LibraryStats?>(null)
    val stats: StateFlow<LibraryStats?> = statsMutable.asStateFlow()

    init {
        refreshStats()
        // Scans are also started by the app shell right after a folder/song is added.
        viewModelScope.launch {
            observeScanState().collect { if (it is ScanState.Completed) refreshStats() }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setAccentHue(hue: Float) {
        viewModelScope.launch { repository.setAccentHue(hue) }
    }

    fun setIgnoreShortFiles(ignore: Boolean) {
        viewModelScope.launch {
            try {
                libraryPreferences.setIgnoreShortFiles(ignore)
            } catch (t: Exception) {
                // Best-effort pref; the switch re-reads persisted state.
            }
        }
    }

    fun rescan() {
        viewModelScope.launch { rescanLibrary() }
    }

    fun removeSources(ids: List<Long>) {
        viewModelScope.launch { removeSourcesUseCase(ids) }
    }

    fun refreshStats() {
        viewModelScope.launch {
            statsMutable.value = try {
                getLibraryStats()
            } catch (t: Exception) {
                null
            }
        }
    }
}
