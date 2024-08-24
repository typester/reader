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
import uniffi.manga.MangaData
import uniffi.manga.MangaException

private const val TAG = "MangaListViewModel"



data class MangaListUIState(
    var loading: Boolean = true,
    var list: List<MangaData> = emptyList(),
)

class MangaListViewModel : ViewModel() {
    private var _uiState = MutableStateFlow(MangaListUIState())
    var uiState = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loading = true) }

                val list = withContext(Dispatchers.IO) {
                    Shared.instance().listManga()
                }
                _uiState.update { it.copy(list = list) }
            } catch (e: MangaException) {
                Log.e(TAG, "failed to fetch mangas", e)
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun deleteManga(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val core = Shared.instance()
                withContext(Dispatchers.IO) {
                    core.deleteManga(id)
                }
                load()
            } catch (e: MangaException) {
                Log.e(TAG, "failed to delete manga", e)
            }
        }
    }
}
