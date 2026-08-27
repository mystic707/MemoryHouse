package com.mysticagit.memoryhouse

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.mysticagit.memoryhouse.data.DriveRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MemoryHouseApp : Application(), ImageLoaderFactory {

    /** 목록 조회와 이미지 다운로드가 커넥션 풀을 함께 쓴다. */
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** 앨범마다 API 키가 다를 수 있으므로 선택된 앨범의 키로 만들어 쓴다. */
    fun driveRepository(apiKey: String): DriveRepository = DriveRepository(httpClient, apiKey)

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { httpClient }
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            // 한 번 받은 썸네일은 다시 받지 않는다. 스크롤을 오르내릴 때 데이터를 아끼는 핵심.
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        // Drive 썸네일 응답의 캐시 헤더를 그대로 따르면 디스크 캐시가 거의 동작하지 않는다.
        .respectCacheHeaders(false)
        .crossfade(true)
        .build()
}
