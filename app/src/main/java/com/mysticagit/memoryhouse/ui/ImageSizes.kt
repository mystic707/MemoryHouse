package com.mysticagit.memoryhouse.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mysticagit.memoryhouse.data.DriveUrls

// 6칸 격자 위에 폴더는 3칸(줄당 2개), 사진은 2칸(줄당 3개)을 차지한다.
internal const val GRID_COLUMNS = 6
internal const val FOLDER_SPAN = 3
internal const val IMAGE_SPAN = 2
internal const val PHOTOS_PER_ROW = GRID_COLUMNS / IMAGE_SPAN
internal val GRID_PADDING = 12.dp
internal val GRID_SPACING = 8.dp

/**
 * 이 기기에서 요청할 이미지 폭.
 *
 * 그리드와 전체화면이 **같은 값**을 봐야 한다. 전체화면이 그리드가 받아둔 썸네일을
 * 자리끼우개로 재사용하려면 캐시 키가 일치해야 하기 때문이다.
 */
data class ImageSizes(
    /** 썸네일 그리드에서 사진 한 칸에 요청할 폭. */
    val grid: Int,
    /** 전체화면 기본 요청 폭. */
    val full: Int,
) {
    /** 핀치로 확대했을 때 올려 받을 폭. */
    val zoomed: Int get() = DriveUrls.zoomedWidthFor(full)
}

@Composable
fun rememberImageSizes(): ImageSizes {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthDp = configuration.screenWidthDp

    return remember(screenWidthDp, density.density) {
        with(density) {
            // 그리드는 화면 폭을 꽉 채우므로, 여백과 간격만 빼면 칸 크기를 정확히 알 수 있다.
            val usable = screenWidthDp.dp - GRID_PADDING * 2 - GRID_SPACING * (PHOTOS_PER_ROW - 1)
            ImageSizes(
                grid = DriveUrls.gridWidthFor((usable / PHOTOS_PER_ROW).roundToPx()),
                full = DriveUrls.fullWidthFor(screenWidthDp.dp.roundToPx()),
            )
        }
    }
}
