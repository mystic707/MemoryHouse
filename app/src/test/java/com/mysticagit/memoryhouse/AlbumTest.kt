package com.mysticagit.memoryhouse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 앨범이 "쓸 수 있는 상태"인지 판단하는 규칙.
 * 반쯤 채워진 설정이 앨범 목록에 끼어들면 빈 폴더나 403 으로 이어지므로 여기서 걸러야 한다.
 */
class AlbumTest {

    private fun album(
        folderId: String = "FOLDER",
        pin: String = "1234",
        apiKey: String = "AIzaKEY",
    ) = Album(folderId = folderId, pin = pin, apiKey = apiKey)

    @Test
    fun 다_채워지면_사용_가능하다() {
        assertTrue(album().isConfigured)
    }

    @Test
    fun 번호가_없거나_자릿수가_다르면_사용_불가다() {
        assertFalse(album(pin = "").isConfigured)
        assertFalse(album(pin = "123").isConfigured)
        assertFalse(album(pin = "12345").isConfigured)
    }

    @Test
    fun 폴더나_키가_비면_사용_불가다() {
        assertFalse(album(folderId = "").isConfigured)
        assertFalse(album(apiKey = "").isConfigured)
    }

    @Test
    fun 번호로_앨범을_고른다() {
        val albums = listOf(
            album(folderId = "F1", pin = "1111"),
            album(folderId = "F2", pin = "2222"),
        )
        assertEquals("F1", albums.firstOrNull { it.pin == "1111" }?.folderId)
        assertEquals("F2", albums.firstOrNull { it.pin == "2222" }?.folderId)
        assertNull(albums.firstOrNull { it.pin == "9999" })
    }

    @Test
    fun 같은_번호를_두_앨범에_넣으면_앞의_것이_이긴다() {
        val albums = listOf(
            album(folderId = "먼저", pin = "1111"),
            album(folderId = "나중", pin = "1111"),
        )
        assertEquals("먼저", albums.firstOrNull { it.pin == "1111" }?.folderId)
    }

    @Test
    fun 실제_설정에는_최소_한_개의_앨범이_있고_번호가_겹치지_않는다() {
        assertTrue("local.properties 에 album1 을 채워야 한다", Config.isConfigured)

        val pins = Config.albums.map { it.pin }
        assertEquals("앨범끼리 잠금 번호가 겹친다", pins.size, pins.distinct().size)

        val folders = Config.albums.map { it.folderId }
        assertEquals("앨범끼리 폴더가 겹친다", folders.size, folders.distinct().size)
    }
}
