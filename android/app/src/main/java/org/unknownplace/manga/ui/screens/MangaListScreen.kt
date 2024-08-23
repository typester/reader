package org.unknownplace.manga.ui.screens

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
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.serialization.Serializable
import org.unknownplace.manga.ui.components.MangaListItem

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
                            onSelectManga(manga.id.toString())
                        },
                        onDeleteItem = {},
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}