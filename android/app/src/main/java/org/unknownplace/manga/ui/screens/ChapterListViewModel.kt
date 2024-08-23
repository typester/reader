package org.unknownplace.manga.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.unknownplace.manga.Shared
import uniffi.manga.ChapterDb
import uniffi.manga.MangaDb
import uniffi.manga.MangaException

private const val TAG = "ChapterListViewModel"

data class ChapterListUIState(
    val loading: Boolean = true,
    val manga: MangaDb? = null,
    val chapters: List<ChapterDb> = emptyList(),
)

class ChapterListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChapterListUIState())
    val uiState = _uiState.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            try {
                val core = Shared.instance()

                val manga = withContext(Dispatchers.IO) {
                    core.getManga(id.toLong())
                }

                manga?.let { m ->
                    _uiState.update { it.copy(manga = m) }
                    val cache = withContext(Dispatchers.IO) {
                        core.getChaptersCache(m.url)
                    }
                    _uiState.update { it.copy(chapters = cache) }

                    val chapters = withContext(Dispatchers.IO) {
                        core.getChapters(manga.url)
                    }
                    _uiState.update { it.copy(chapters = chapters) }
                }
            } catch (e: MangaException) {
                Log.e(TAG, "failed to load chapters", e)
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }
}
