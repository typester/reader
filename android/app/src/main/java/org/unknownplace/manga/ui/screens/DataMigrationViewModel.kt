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
import uniffi.manga.MangaException

private const val TAG = "DataMigrationViewModel";

data class DataMigrationUiState(
    val migrating: Boolean = false,
    val finished: Boolean = false,
    val error: String? = null,
)

class DataMigrationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DataMigrationUiState())
    val uiState = _uiState.asStateFlow()

    fun migrate() {
        viewModelScope.launch {
            try {
                val core = Shared.instance()

                if (core.migrationAvailable()) {
                    _uiState.update { it.copy(migrating = true) }
                    withContext(Dispatchers.IO) {
                        core.doMigration()
                    }
                }
                _uiState.update { it.copy(finished = true) }
            } catch (e: MangaException) {
                Log.e(TAG, "migration error", e)
                _uiState.update { it.copy(error = e.toString()) }
            } finally {
                _uiState.update { it.copy(migrating = false) }
            }
        }
    }

    fun resetAndMigrate() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val core = Shared.instance()
                core.resetDb()
                migrate()
            } catch (e: MangaException) {
                Log.e(TAG, "database reset error", e)
                _uiState.update { it.copy(error = e.toString()) }
            }
        }
    }
}