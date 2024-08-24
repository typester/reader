package org.unknownplace.manga.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.unknownplace.manga.Shared
import org.unknownplace.manga.ui.components.MangaListItem
import uniffi.manga.MangaData
import uniffi.manga.MangaException

private const val TAG = "MangaListScreen"

@Serializable
object MangaList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaListScreen(
    viewModel: MangaListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onSearchManga: () -> Unit,
    onSelectManga: (id: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    var deleteTarget by remember {
        mutableStateOf<MangaData?>(null)
    }

    LaunchedEffect(true) {
        viewModel.load()
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        item {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Manga")
                },
                actions = {
                    IconButton(onClick = { onSearchManga() }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Manga")
                    }
                },
                scrollBehavior = scrollBehavior,
            )

            if (deleteTarget != null) {
                AlertDialog(
                    onDismissRequest = { deleteTarget = null },
                    confirmButton = {
                        TextButton(onClick = {
                            deleteTarget?.let {
                                viewModel.deleteManga(it.id)
                            }
                            deleteTarget = null
                        }) {
                            Text(text = "Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            deleteTarget = null
                        }) {
                            Text(text = "Cancel")
                        }
                    },
                    title = {
                        Text(text = "Delete ${deleteTarget?.title}?")
                    },
                    text = {
                        Text(text = "Are you sure you want to delete this manga? This action cannot be undone.")
                    },
                )
            }
        }
        
        if (uiState.loading) {
            item { 
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            if (uiState.list.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "There are no manga read yet.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f)
                        )
                    }
                }
            } else {
                items(uiState.list) { manga ->
                    MangaListItem(
                        title = manga.title,
                        image = manga.image,
                        domain = manga.domain,
                        onClick = {
                            scope.launch {
                                try {
                                    Shared.instance().openMangaWithId(manga.id)
                                } catch (e: MangaException) {
                                    Log.e(TAG, "open manga failed", e)
                                }
                            }
                            onSelectManga(manga.id.toString())
                        },
                        onDeleteItem = {
                            deleteTarget = manga
                        },
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}