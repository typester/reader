package org.unknownplace.manga.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.serialization.Serializable
import org.unknownplace.manga.SharedContext
import org.unknownplace.manga.lockScreenOrientationToLandscape
import org.unknownplace.manga.unlockScreenOrientation

private const val TAG = "ReaderScreen"

@Serializable
data class Reader(val chapterId: String)

@Composable
fun ReaderScreen(
    chapterId: String,
    activity: Activity,
    viewModel: ReaderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val singleFirstPageMode = remember {
        mutableStateOf(true)
    }
    var lastPage by remember {
        mutableStateOf(0)
    }

    val pagerState = remember(uiState.images.size, singleFirstPageMode.value) {
        Log.d(TAG, "pagerState updated: page=${lastPage}")
        PagerState(
            pageCount = {
                if (singleFirstPageMode.value) {
                    (uiState.images.size + 2) / 2
                } else {
                    (uiState.images.size + 1) / 2
                }
            },
            currentPage = if (singleFirstPageMode.value || lastPage == 0) lastPage else lastPage - 1,
        )
    }

    LaunchedEffect(Unit) {
        activity.lockScreenOrientationToLandscape()
        viewModel.load(chapterId)
    }

    LaunchedEffect(pagerState.currentPage) {
        Log.d(TAG, "page updated: ${pagerState.currentPage}")
        lastPage = pagerState.currentPage
    }

    DisposableEffect(Unit) {
        onDispose {
            activity.unlockScreenOrientation()
        }
    }


    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            singleFirstPageMode.value = !singleFirstPageMode.value
                        }
                    )
                }
        ) { page ->
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val firstImageIndex = if (singleFirstPageMode.value && page > 0) page * 2 - 1 else page * 2

                if (page == 0 && singleFirstPageMode.value) {
                    uiState.images.getOrNull(firstImageIndex)?.let {
                        AsyncImage(
                            model = ImageRequest.Builder(SharedContext.context())
                                .data(it)
                                .apply {
                                    uiState.headers.forEach { (k, v) ->
                                        setHeader(k, v)
                                    }
                                }
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxHeight()
                        )
                    }
                } else {
                    if (firstImageIndex < uiState.images.size) {
                        AsyncImage(
                            model = ImageRequest.Builder(SharedContext.context())
                                .data(uiState.images[firstImageIndex])
                                .apply {
                                    uiState.headers.forEach { (k, v) ->
                                        setHeader(k, v)
                                    }
                                }
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxHeight()
                        )
                    }
                    if (firstImageIndex + 1 < uiState.images.size) {
                        AsyncImage(
                            model = ImageRequest.Builder(SharedContext.context())
                                .data(uiState.images[firstImageIndex + 1])
                                .apply {
                                    uiState.headers.forEach { (k, v) ->
                                        setHeader(k, v)
                                    }
                                }
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}