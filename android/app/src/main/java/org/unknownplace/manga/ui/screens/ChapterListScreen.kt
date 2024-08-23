package org.unknownplace.manga.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import org.unknownplace.manga.ui.components.ChapterListItem

@Serializable
data class ChapterList(val id: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    id: String,
    viewModel: ChapterListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onSelectChapter: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(true) {
        viewModel.load(id)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .navigationBarsPadding()
    ) {
        item {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = uiState.manga?.title ?: "")
                },
                scrollBehavior = scrollBehavior,
            )
        }

        if (uiState.loading) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        items(uiState.chapters) { chapter ->
            ChapterListItem(
                id = chapter.id,
                title = chapter.title,
                isRead = chapter.isRead != 0L,
                onClick = { id ->
                    onSelectChapter(id.toString())
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
        }
    }
}