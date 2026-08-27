package com.mysticagit.memoryhouse

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mysticagit.memoryhouse.data.DriveItem
import com.mysticagit.memoryhouse.ui.ViewerScreen
import com.mysticagit.memoryhouse.ui.ZoomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 전체화면 뷰어에서 실제 터치 제스처가 확대로 이어지는지 확인한다. */
@RunWith(AndroidJUnit4::class)
class ViewerGestureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val images = listOf(
        DriveItem.Image("1h8l3S3a2E-ZD0nBPIUpcQALtpYpJa5ls", "IMG_3831.jpeg", null),
        DriveItem.Image("1PIiN39QPKOgltQg9eY4P-aCv-xWKfDZF", "IMG_3825.jpeg", null),
    )

    @Composable
    private fun Viewer(
        zoom: ZoomState,
        initialIndex: Int = 0,
        onIndexChange: (Int) -> Unit = {},
    ) {
        ViewerScreen(
            images = images,
            initialIndex = initialIndex,
            onIndexChange = onIndexChange,
            onClose = {},
            zoom = zoom,
        )
    }

    @Test
    fun 확대하지_않으면_스와이프로_다음_사진으로_넘어간다() {
        var index = -1
        composeTestRule.setContent { Viewer(ZoomState(), onIndexChange = { index = it }) }

        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals(1, index)
    }

    @Test
    fun 스와이프로_이전_사진으로_돌아온다() {
        var index = -1
        composeTestRule.setContent {
            Viewer(ZoomState(), initialIndex = 1, onIndexChange = { index = it })
        }

        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        assertEquals(0, index)
    }

    @Test
    fun 첫_사진에서_이전으로_넘어가지_않는다() {
        var index = -1
        composeTestRule.setContent { Viewer(ZoomState(), onIndexChange = { index = it }) }

        composeTestRule.onRoot().performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        assertEquals("첫 사진에서 뒤로 넘어가면 안 된다 (순환 금지)", 0, index)
    }

    @Test
    fun 마지막_사진에서_다음으로_넘어가지_않는다() {
        var index = -1
        composeTestRule.setContent {
            Viewer(ZoomState(), initialIndex = images.lastIndex, onIndexChange = { index = it })
        }

        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals("마지막 사진에서 더 넘어가면 안 된다 (순환 금지)", images.lastIndex, index)
    }

    @Test
    fun 확대_중에는_스와이프로_넘어가지_않는다() {
        var index = -1
        val zoom = ZoomState()
        composeTestRule.setContent { Viewer(zoom, onIndexChange = { index = it }) }

        composeTestRule.onRoot().performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()
        assertTrue("먼저 확대되어야 한다", zoom.isZoomed)

        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals("확대 중에는 사진이 넘어가면 안 된다", 0, index)
        assertTrue("확대 상태가 풀리면 안 된다", zoom.isZoomed)
    }

    @Test
    fun 사진을_넘기면_확대가_초기화된다() {
        val zoom = ZoomState()
        composeTestRule.setContent { Viewer(zoom) }

        composeTestRule.onRoot().performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()
        assertTrue(zoom.isZoomed)

        // 확대를 풀고 다음 사진으로 넘긴다.
        composeTestRule.onRoot().performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals(1f, zoom.scale, 0.001f)
    }

    @Test
    fun 더블탭하면_확대된다() {
        val zoom = ZoomState()
        composeTestRule.setContent { Viewer(zoom) }

        composeTestRule.onRoot().performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()

        assertEquals(2.5f, zoom.scale, 0.001f)
    }

    @Test
    fun 더블탭_두_번이면_원래_크기로_돌아온다() {
        val zoom = ZoomState()
        composeTestRule.setContent { Viewer(zoom) }

        composeTestRule.onRoot().performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput { doubleClick() }
        composeTestRule.waitForIdle()

        assertEquals(1f, zoom.scale, 0.001f)
        assertEquals(Offset.Zero, zoom.offset)
    }

    @Test
    fun 핀치하면_확대된다() {
        val zoom = ZoomState()
        composeTestRule.setContent { Viewer(zoom) }

        composeTestRule.onRoot().performTouchInput {
            // 두 손가락을 가운데에서 바깥으로 벌린다.
            pinch(
                start0 = center + Offset(-40f, 0f),
                end0 = center + Offset(-240f, 0f),
                start1 = center + Offset(40f, 0f),
                end1 = center + Offset(240f, 0f),
            )
        }
        composeTestRule.waitForIdle()

        assertTrue("핀치 후 배율이 1보다 커야 한다 (실제: ${zoom.scale})", zoom.scale > 1f)
    }
}
