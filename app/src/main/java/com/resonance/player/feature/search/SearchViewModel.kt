package com.resonance.player.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.player.core.model.Song
import com.resonance.player.domain.search.SearchLibraryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Thin ViewModel: query text in, local search results out. */
class SearchViewModel(searchLibrary: SearchLibraryUseCase) : ViewModel() {
    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<Song>> = query
        .flatMapLatest { searchLibrary(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentQuery: StateFlow<String> = query

    fun onQueryChange(value: String) {
        query.value = value
    }
}
