package org.unknownplace.manga.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import org.unknownplace.manga.ui.components.MangaListItem

private const val TAG = "SearchMangaScreen"

@Serializable
object SearchManga

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchMangaScreen(
    viewModel: SearchMangaViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onSelectManga: (id: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var menuExpanded by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        item {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Search Manga")
                },
                scrollBehavior = scrollBehavior,
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(text = "Search word")
                    },
                    value = uiState.text,
                    onValueChange = { text ->
                        viewModel.setText(text)
                    }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = uiState.sites.getOrNull(uiState.selectedIndex)?.name() ?: "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(
                                onClick = { menuExpanded = true }
                            ) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select site")
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = !menuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.sites.forEachIndexed { index, site ->
                            DropdownMenuItem(
                                text = { Text(text = site.name()) },
                                onClick = {
                                    viewModel.select(index)
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.search() },
                    enabled = !uiState.searching && uiState.text.isNotEmpty(),
                ) {
                    Text(text = "Search")
                }
            }
        }

        if (uiState.searching) {
            item {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(uiState.links) { link ->
                MangaListItem(
                    title = link.text,
                    image = link.image,
                    onClick = {
                        scope.launch {
                            Log.d(TAG, "trying to open manga")
                            val mangaId = viewModel.openManga(link)
                            Log.d(TAG, "got mangaId: ${mangaId}")
                            mangaId?.let { onSelectManga(it) }
                        }
                    },
                    hideDeleteIcon = true,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}