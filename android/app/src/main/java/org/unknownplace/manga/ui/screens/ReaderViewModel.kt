package org.unknownplace.manga.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.unknownplace.manga.Shared
import uniffi.manga.MangaException

private const val TAG = "ReaderViewModel"

data class ReaderUiState(
    val images: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
)

class ReaderViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState = _uiState.asStateFlow()

    fun load(chapterId: String) {
        viewModelScope.launch {
            val core = Shared.instance()

            try {
                val (site, images) = withContext(Dispatchers.IO) {
                    val chapter = core.getChapter(chapterId.toLong())
                    chapter?.let { c ->
                        core.markChapterRead(c.id, true)

                        val site = core.getSite(c.url)
                        val images = core.getImages(c.url)

                        Pair(site, images)
                    } ?: run {
                        Pair(null, emptyList())
                    }
                }

                site?.let { s ->
                    _uiState.update { it.copy(
                        headers = s.requestHeaders(),
                        images = images,
                    ) }
                } ?: run {
                    _uiState.update { it.copy(images = images) }
                }
            } catch (e: MangaException) {

            }
        }
    }
}