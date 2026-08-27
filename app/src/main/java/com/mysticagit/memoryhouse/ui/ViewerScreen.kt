package com.mysticagit.memoryhouse.ui

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mysticagit.memoryhouse.data.DriveItem
import com.mysticagit.memoryhouse.data.DriveUrls

private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

/**
 * 사진을 전체화면으로 보여준다.
 *
 * - 핀치로 확대/축소, 확대 상태에서 드래그로 이동, 더블탭으로 확대와 원래 크기를 오간다.
 * - 확대하지 않은 상태에서 좌우로 스와이프하면 이전/다음 사진으로 넘어간다.
 *   확대 중에는 스와이프가 잠기고, 목록의 처음과 끝에서는 더 넘어가지 않는다(순환하지 않음).
 */
@Composable
fun ViewerScreen(
    images: List<DriveItem.Image>,
    initialIndex: Int,
    onIndexChange: (Int) -> Unit,
    onClose: () -> Unit,
    zoom: ZoomState = remember { ZoomState(maxScale = MAX_SCALE, doubleTapScale = DOUBLE_TAP_SCALE) },
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(images.indices),
        pageCount = { images.size },
    )

    // 사진을 넘기면 확대 상태를 초기화하고 현재 위치를 알린다.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            zoom.reset()
            onIndexChange(page)
        }
    }

    HideSystemBars()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { zoom.containerSize = it },
    ) {
        HorizontalPager(
            state = pagerState,
            // 확대 중에는 스와이프로 넘기지 않는다. 드래그는 사진을 움직이는 데 쓴다.
            userScrollEnabled = !zoom.isZoomed,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ZoomableImage(
                image = images[page],
                // 확대 변환은 지금 보고 있는 사진에만 적용한다.
                zoom = if (page == pagerState.currentPage) zoom else null,
            )
        }

        // 상단 그라디언트 위에 파일명과 닫기 버튼.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
            }
            Text(
                text = images.getOrNull(pagerState.currentPage)?.name.orEmpty(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (images.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${images.size}",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/**
 * 사진 한 장. [zoom] 이 null 이면(지금 보고 있는 사진이 아니면) 확대 제스처를 받지 않는다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomableImage(
    image: DriveItem.Image,
    zoom: ZoomState?,
) {
    val context = LocalContext.current
    val sizes = rememberImageSizes()

    // 확대하지 않으면 화면 폭에 맞는 것만 받고, 핀치로 확대할 때만 한 단계 큰 것을 더 받는다.
    val zoomed = zoom?.isZoomed == true
    val requestWidth = if (zoomed) sizes.zoomed else sizes.full
    val request = remember(image.id, requestWidth) {
        ImageRequest.Builder(context)
            .data(DriveUrls.fullScreen(image.id, requestWidth))
            .memoryCacheKey(DriveUrls.cacheKey(image.id, requestWidth))
            // 큰 이미지를 받는 동안 이미 갖고 있는 것을 먼저 보여준다.
            // 확대 전에는 그리드 썸네일, 확대 직후에는 방금 보던 기본 이미지가 자리를 메운다.
            .placeholderMemoryCacheKey(
                DriveUrls.cacheKey(image.id, if (zoomed) sizes.full else sizes.grid)
            )
            .crossfade(true)
            .build()
    }

    // 핀치는 항상 받되, 한 손가락 드래그(팬)는 확대 중일 때만 가져간다.
    // 확대하지 않은 상태에서 팬을 소비해버리면 Pager 가 스와이프를 못 받아 사진이 넘어가지 않는다.
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        zoom?.onTransform(pan = panChange, zoom = zoomChange)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (zoom == null) {
                    Modifier
                } else {
                    Modifier
                        .transformable(
                            state = transformState,
                            canPan = { zoom.isZoomed },
                        )
                        .pointerInput(image.id) {
                            detectTapGestures(onDoubleTap = { zoom.onDoubleTap() })
                        }
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = image.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom?.scale ?: 1f
                    scaleY = zoom?.scale ?: 1f
                    translationX = zoom?.offset?.x ?: 0f
                    translationY = zoom?.offset?.y ?: 0f
                },
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            },
            error = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp),
                    )
                }
            },
        )
    }
}

/** 전체화면인 동안만 상태바/네비게이션바를 숨긴다. */
@Composable
private fun HideSystemBars() {
    val view = LocalView.current
    val context = LocalContext.current

    DisposableEffect(view) {
        val window = (context as? Activity)?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previousBehavior = controller.systemBarsBehavior
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())

            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = previousBehavior
            }
        }
    }
}
