package com.mysticagit.memoryhouse.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * 전체화면 뷰어의 확대/이동 상태.
 *
 * 제스처 감지는 Compose 가 해주고, 여기서는 배율 제한과 이동 범위 계산만 담당한다.
 * 화면에서 떼어내 테스트할 수 있도록 별도 클래스로 두었다.
 */
class ZoomState(
    private val maxScale: Float = 5f,
    private val doubleTapScale: Float = 2.5f,
) {
    var scale by mutableFloatStateOf(1f)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    var containerSize by mutableStateOf(IntSize.Zero)

    val isZoomed: Boolean get() = scale > 1f

    /** 핀치 제스처 한 번 분량을 반영한다. */
    fun onTransform(pan: Offset, zoom: Float) {
        val next = (scale * zoom).coerceIn(1f, maxScale)
        // 원래 크기로 돌아오면 위치도 가운데로 되돌린다.
        offset = if (next <= 1f) Offset.Zero else clamp(offset + pan, next)
        scale = next
    }

    /** 더블탭: 확대 상태면 원래대로, 아니면 [doubleTapScale] 배로. */
    fun onDoubleTap() {
        if (isZoomed) {
            reset()
        } else {
            scale = doubleTapScale
        }
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    /** 확대 배율에 맞춰 사진이 화면 밖으로 완전히 빠져나가지 않도록 이동 범위를 제한한다. */
    private fun clamp(candidate: Offset, currentScale: Float): Offset {
        val maxX = (containerSize.width * (currentScale - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (containerSize.height * (currentScale - 1f) / 2f).coerceAtLeast(0f)
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY),
        )
    }
}
