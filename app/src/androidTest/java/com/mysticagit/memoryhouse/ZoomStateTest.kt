package com.mysticagit.memoryhouse

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mysticagit.memoryhouse.ui.ZoomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 확대/이동 계산 자체에 대한 검증. 제스처 감지는 Compose 쪽이라 여기서는 다루지 않는다. */
@RunWith(AndroidJUnit4::class)
class ZoomStateTest {

    private fun state() = ZoomState(maxScale = 5f, doubleTapScale = 2.5f).apply {
        containerSize = IntSize(1000, 2000)
    }

    @Test
    fun 처음에는_원래_크기다() {
        val zoom = state()
        assertEquals(1f, zoom.scale, 0.001f)
        assertEquals(Offset.Zero, zoom.offset)
        assertFalse(zoom.isZoomed)
    }

    @Test
    fun 핀치로_확대된다() {
        val zoom = state()
        zoom.onTransform(pan = Offset.Zero, zoom = 2f)
        assertEquals(2f, zoom.scale, 0.001f)
        assertTrue(zoom.isZoomed)
    }

    @Test
    fun 최대_배율을_넘지_않는다() {
        val zoom = state()
        repeat(10) { zoom.onTransform(pan = Offset.Zero, zoom = 2f) }
        assertEquals(5f, zoom.scale, 0.001f)
    }

    @Test
    fun 원래_크기_밑으로는_줄어들지_않는다() {
        val zoom = state()
        repeat(10) { zoom.onTransform(pan = Offset.Zero, zoom = 0.5f) }
        assertEquals(1f, zoom.scale, 0.001f)
    }

    @Test
    fun 확대_상태에서만_이동이_남는다() {
        val zoom = state()
        zoom.onTransform(pan = Offset(100f, 100f), zoom = 2f)
        assertEquals(Offset(100f, 100f), zoom.offset)

        // 다시 원래 크기로 줄이면 위치도 가운데로 돌아온다.
        zoom.onTransform(pan = Offset.Zero, zoom = 0.1f)
        assertEquals(1f, zoom.scale, 0.001f)
        assertEquals(Offset.Zero, zoom.offset)
    }

    @Test
    fun 사진이_화면_밖으로_빠져나가지_않는다() {
        val zoom = state()
        // 2배 확대에서 가로 이동 한계는 1000 * (2-1) / 2 = 500.
        zoom.onTransform(pan = Offset(9999f, 9999f), zoom = 2f)
        assertEquals(500f, zoom.offset.x, 0.001f)
        assertEquals(1000f, zoom.offset.y, 0.001f)
    }

    @Test
    fun 더블탭으로_확대와_원래_크기를_오간다() {
        val zoom = state()
        zoom.onDoubleTap()
        assertEquals(2.5f, zoom.scale, 0.001f)

        zoom.onDoubleTap()
        assertEquals(1f, zoom.scale, 0.001f)
        assertEquals(Offset.Zero, zoom.offset)
    }
}
