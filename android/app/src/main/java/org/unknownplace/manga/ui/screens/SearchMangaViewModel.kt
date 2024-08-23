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
import uniffi.manga.Link
import uniffi.manga.MangaException
import uniffi.manga.MangaSite

private const val TAG = "SearchMangaViewModel";

data class SearchMangaUiState(
    val text: String = "",
    val sites: List<MangaSite> = Shared.instance().supportedSites(),
    val selectedIndex: Int = 0,
    val searching: Boolean = false,
    val links: List<Link> = emptyList(),
    val error: String? = null,
)

class SearchMangaViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SearchMangaUiState())
    val uiState = _uiState.asStateFlow()

    fun setText(text: String) {
        _uiState.update { currentState ->
            currentState.copy(
                text = text,
            )
        }
    }

    fun select(index: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedIndex = index,
            )
        }
    }

    fun search() {
        if (uiState.value.searching) {
            return
        }

        uiState.value.sites.getOrNull(uiState.value.selectedIndex).let { site ->
            _uiState.update { currentState ->
                currentState.copy(
                    searching = true,
                )
            }
            viewModelScope.launch {
                try {
                    val res = withContext(Dispatchers.IO) {
                        val res = site?.search(uiState.value.text)
                        Log.d(TAG, "res: ${res}")
                        res
                    }
                    withContext(Dispatchers.Main) {
                        _uiState.update { currentState ->
                            currentState.copy(
                                links = res ?: emptyList()
                            )
                        }
                    }
                } catch (e: MangaException) {
                    Log.e(TAG, "failed to search", e);
                    _uiState.update { it.copy(error = e.toString()) }
                } finally {
                    _uiState.update { currentState ->
                        currentState.copy(
                            searching = false,
                        )
                    }
                }
            }

        }
    }

    suspend fun openManga(link: Link): String? {
        return try {
            val core = Shared.instance()
            val manga = withContext(Dispatchers.IO) {
                core.openManga(link)
            }
            Log.i(TAG, "manga opened")
            manga.id.toString()
        } catch (e: MangaException) {
            Log.e(TAG, "failed to open manga", e)
            null
        }
    }
}

