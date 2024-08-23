package org.unknownplace.manga

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.unknownplace.manga.ui.screens.ChapterList
import org.unknownplace.manga.ui.screens.ChapterListScreen
import org.unknownplace.manga.ui.screens.DataMigration
import org.unknownplace.manga.ui.screens.DataMigrationScreen
import org.unknownplace.manga.ui.screens.MangaList
import org.unknownplace.manga.ui.screens.MangaListScreen
import org.unknownplace.manga.ui.screens.Reader
import org.unknownplace.manga.ui.screens.ReaderScreen
import org.unknownplace.manga.ui.screens.SearchManga
import org.unknownplace.manga.ui.screens.SearchMangaScreen

@Composable
fun MangaGraph(activity: Activity) {
    val navController = rememberNavController()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(navController, startDestination = DataMigration) {
            composable<DataMigration> {
                DataMigrationScreen(
                    onFinishMigration = {
                        navController.navigate(MangaList)
                    }
                )
            }

            composable<MangaList> {
                MangaListScreen(
                    onSearchManga = {
                        navController.navigate(SearchManga)
                    },
                    onSelectManga = { id ->
                        navController.navigate(ChapterList(id))
                    }
                )
            }

            composable<SearchManga> {
                SearchMangaScreen(
                    onSelectManga = { id ->
                        navController.navigate(ChapterList(id))
                    }
                )
            }

            composable<ChapterList> { backStackEntry ->
                val chapterList: ChapterList = backStackEntry.toRoute()
                ChapterListScreen(
                    id = chapterList.id,
                    onSelectChapter = { chapterId ->
                        navController.navigate(Reader(chapterId))
                    }
                )
            }

            composable<Reader> { backStackEntry ->
                val reader: Reader = backStackEntry.toRoute()
                ReaderScreen(
                    chapterId = reader.chapterId,
                    activity = activity,
                )
            }
        }
    }
}
