package com.mysticagit.memoryhouse

import com.mysticagit.memoryhouse.data.DriveUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 요청 폭 결정 규칙. 데이터 사용량에 바로 직결되므로 값을 고정해둔다. */
class DriveUrlsTest {

    @Test
    fun 원본을_받는_URL_은_만들지_않는다() {
        val url = DriveUrls.gridThumbnail("FILE_ID", 320)
        assertTrue("sz 파라미터가 있어야 한다: $url", url.contains("sz=w320"))
        assertTrue("원본 다운로드 경로가 섞이면 안 된다: $url", !url.contains("alt=media"))
        assertTrue("원본 다운로드 경로가 섞이면 안 된다: $url", !url.contains("export=download"))
    }

    @Test
    fun 그리드는_칸_크기에_맞춰_사다리_값으로_눌린다() {
        // 720p 기기: 칸이 약 222px -> 240
        assertEquals(240, DriveUrls.gridWidthFor(222))
        // 1080p 기기: 칸이 약 325px -> 320 (5% 이내 부족은 낮은 단계를 쓴다)
        assertEquals(320, DriveUrls.gridWidthFor(325))
        // 저해상도 기기
        assertEquals(200, DriveUrls.gridWidthFor(200))
    }

    @Test
    fun 그리드_폭에는_상한이_있다() {
        // 태블릿에서도 512 를 넘기지 않는다.
        assertEquals(512, DriveUrls.gridWidthFor(900))
        assertEquals(512, DriveUrls.gridWidthFor(4000))
    }

    @Test
    fun 전체화면은_화면_폭에_맞춘다() {
        assertEquals(1080, DriveUrls.fullWidthFor(1080))
        assertEquals(720, DriveUrls.fullWidthFor(720))
        // 상한 1440
        assertEquals(1440, DriveUrls.fullWidthFor(2400))
    }

    @Test
    fun 확대하면_더_큰_것을_받는다() {
        val base = DriveUrls.fullWidthFor(1080)
        val zoomed = DriveUrls.zoomedWidthFor(base)
        assertTrue("확대 시 기본보다 커야 한다 ($base -> $zoomed)", zoomed > base)
        assertEquals(2048, zoomed)
    }

    @Test
    fun 확대_폭은_기본보다_작아지지_않는다() {
        val base = DriveUrls.fullWidthFor(4000)   // 상한 1440
        assertTrue(DriveUrls.zoomedWidthFor(base) >= base)
    }

    @Test
    fun 캐시_키는_폭까지_구분한다() {
        assertEquals("ID@w320", DriveUrls.cacheKey("ID", 320))
        assertTrue(DriveUrls.cacheKey("ID", 320) != DriveUrls.cacheKey("ID", 1080))
    }
}
