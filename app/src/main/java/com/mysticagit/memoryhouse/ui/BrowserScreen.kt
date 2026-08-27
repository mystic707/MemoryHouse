package com.mysticagit.memoryhouse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mysticagit.memoryhouse.data.DriveItem
import com.mysticagit.memoryhouse.data.DriveUrls

// 격자 상수와 요청 폭 계산은 ImageSizes.kt 에 모여 있다.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    state: BrowserUiState,
    onFolderClick: (DriveItem.Folder) -> Unit,
    onImageClick: (DriveItem.Image) -> Unit,
    onUpClick: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onErrorShown: () -> Unit,
    onLock: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    // 저해상도 기기에서 필요 이상으로 큰 이미지를 받지 않도록 기기별로 폭을 정한다.
    val thumbWidth = rememberImageSizes().grid

    // 폴더가 바뀌면 스크롤을 맨 위로 되돌린다.
    LaunchedEffect(state.breadcrumb.lastOrNull()?.id) {
        gridState.scrollToItem(0)
    }

    // 목록 끝이 보이기 시작하면 다음 페이지를 미리 받아둔다.
    val shouldLoadMore by remember(state.items.size, state.canLoadMore) {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            state.canLoadMore && last >= state.items.size - 8
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    // 이미 목록이 떠 있는 상태에서의 오류는 화면을 갈아엎지 않고 스낵바로만 알린다.
    LaunchedEffect(state.error) {
        val message = state.error
        if (message != null && state.items.isNotEmpty()) {
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    if (!state.isAtRoot) {
                        IconButton(onClick = onUpClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "상위 폴더")
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = state.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (!state.isAtRoot) {
                            Text(
                                text = state.breadcrumb.joinToString(" / ") { it.name },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                    // 잠금 화면으로 돌아가면 다른 번호로 다른 사진첩을 열 수 있다.
                    IconButton(onClick = onLock) {
                        Icon(Icons.Default.Lock, contentDescription = "잠그기")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                state.error != null && state.items.isEmpty() -> {
                    MessageBlock(
                        title = "사진을 불러오지 못했어요",
                        detail = state.error,
                        actionLabel = "다시 시도",
                        onAction = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                state.isEmpty -> {
                    MessageBlock(
                        title = "이 폴더에는 사진이 없어요",
                        detail = "하위 폴더나 이미지 파일이 보이지 않습니다.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(GRID_COLUMNS),
                        contentPadding = PaddingValues(GRID_PADDING),
                        horizontalArrangement = Arrangement.spacedBy(GRID_SPACING),
                        verticalArrangement = Arrangement.spacedBy(GRID_SPACING),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = state.items,
                            key = { it.id },
                            span = {
                                GridItemSpan(if (it is DriveItem.Folder) FOLDER_SPAN else IMAGE_SPAN)
                            },
                        ) { item ->
                            when (item) {
                                is DriveItem.Folder -> FolderCard(
                                    folder = item,
                                    coverFileId = state.covers[item.id],
                                    thumbWidth = thumbWidth,
                                    onClick = { onFolderClick(item) },
                                )

                                is DriveItem.Image -> PhotoCell(
                                    image = item,
                                    thumbWidth = thumbWidth,
                                    onClick = { onImageClick(item) },
                                )
                            }
                        }

                        if (state.isLoadingMore) {
                            item(span = { GridItemSpan(GRID_COLUMNS) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderCard(
    folder: DriveItem.Folder,
    coverFileId: String?,
    thumbWidth: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.35f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        // 표지를 받는 동안에도 카드가 비어 보이지 않도록 아이콘을 항상 깔고 그 위에 사진을 얹는다.
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Center)
                .size(40.dp),
        )
        if (coverFileId != null) {
            AsyncImage(
                model = thumbnailRequest(coverFileId, thumbWidth),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 이름이 사진 위에서도 읽히도록 아래쪽에 어둡게 깔아준다.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PhotoCell(
    image: DriveItem.Image,
    thumbWidth: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = thumbnailRequest(image.id, thumbWidth),
            contentDescription = image.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * 그리드 썸네일 요청.
 *
 * 메모리 캐시 키를 직접 지정해서, 전체화면 뷰어가 여기서 받아둔 썸네일을 그대로
 * 자리끼우개로 재사용할 수 있게 한다(추가 통신 없이 바로 사진이 보인다).
 */
@Composable
private fun thumbnailRequest(fileId: String, width: Int): ImageRequest {
    val context = LocalContext.current
    return remember(fileId, width) {
        ImageRequest.Builder(context)
            .data(DriveUrls.gridThumbnail(fileId, width))
            .memoryCacheKey(DriveUrls.cacheKey(fileId, width))
            .crossfade(true)
            .build()
    }
}

@Composable
private fun MessageBlock(
    title: String,
    detail: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.ImageNotSupported,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (detail != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
