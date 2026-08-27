package com.mysticagit.memoryhouse.data

import com.mysticagit.memoryhouse.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** 사용자에게 그대로 보여줄 수 있는 메시지를 담은 예외. */
class DriveException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Drive REST API 중 files.list 하나만 쓰는 얇은 클라이언트.
 *
 * 공식 google-api-services-drive SDK 는 이 용도에 비해 지나치게 무거워서 직접 호출한다.
 * 폴더가 링크 공개 상태이므로 OAuth 없이 API 키만으로 조회된다.
 */
class DriveRepository(
    private val client: OkHttpClient,
    private val apiKey: String,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val filesEndpoint = "${Config.apiBaseUrl.trimEnd('/')}/drive/v3/files"

    /**
     * [folderId] 바로 아래의 하위 폴더와 이미지 파일만 한 페이지 조회한다.
     * 문서 등 이미지가 아닌 파일은 쿼리 단계에서 걸러지므로 앱으로 넘어오지 않는다.
     */
    suspend fun listChildren(folderId: String, pageToken: String? = null): DrivePage =
        withContext(Dispatchers.IO) {
            val query = buildString {
                append("'").append(folderId).append("' in parents")
                append(" and trashed = false")
                append(" and (mimeType = '").append(MIME_FOLDER).append("'")
                append(" or mimeType contains 'image/')")
            }

            val url = filesEndpoint.toHttpUrl().newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("fields", "nextPageToken,files(id,name,mimeType,modifiedTime)")
                // 폴더를 먼저, 그 안에서는 이름순. Drive 웹에서 보던 순서와 같아진다.
                .addQueryParameter("orderBy", "folder,name")
                .addQueryParameter("pageSize", "100")
                .addQueryParameter("key", apiKey)
                .apply { if (pageToken != null) addQueryParameter("pageToken", pageToken) }
                .build()

            val body = execute(Request.Builder().url(url).get().build())
            val parsed = runCatching { json.decodeFromString<DriveFileListResponse>(body) }
                .getOrElse { throw DriveException("드라이브 응답을 읽지 못했습니다.", it) }

            DrivePage(
                items = parsed.files.map { it.toDriveItem() },
                nextPageToken = parsed.nextPageToken,
            )
        }

    /**
     * 폴더 자체의 이름. 최상단 폴더 이름을 앱에 적어두지 않고 드라이브에서 그대로 가져온다.
     * 드라이브에서 폴더 이름을 바꾸면 앱에도 그대로 반영된다.
     */
    suspend fun folderName(folderId: String): String? = withContext(Dispatchers.IO) {
        val url = "$filesEndpoint/$folderId".toHttpUrl().newBuilder()
            .addQueryParameter("fields", "id,name,mimeType")
            .addQueryParameter("key", apiKey)
            .build()

        val body = execute(Request.Builder().url(url).get().build())
        runCatching { json.decodeFromString<DriveFileDto>(body) }.getOrNull()
            ?.takeIf { it.mimeType == MIME_FOLDER }
            ?.name
    }

    /**
     * 폴더 표지에 쓸 이미지 한 장을 찾는다.
     * 하위에 폴더만 있고 이미지가 없는 폴더도 있으므로 null 을 돌려줄 수 있다.
     */
    suspend fun findCoverImage(folderId: String): DriveItem.Image? = withContext(Dispatchers.IO) {
        val url = filesEndpoint.toHttpUrl().newBuilder()
            .addQueryParameter(
                "q",
                "'$folderId' in parents and trashed = false and mimeType contains 'image/'"
            )
            .addQueryParameter("fields", "files(id,name,mimeType,modifiedTime)")
            .addQueryParameter("orderBy", "name")
            .addQueryParameter("pageSize", "1")
            .addQueryParameter("key", apiKey)
            .build()

        val body = execute(Request.Builder().url(url).get().build())
        val parsed = runCatching { json.decodeFromString<DriveFileListResponse>(body) }.getOrNull()
        parsed?.files?.firstOrNull()?.toDriveItem() as? DriveItem.Image
    }

    private fun execute(request: Request): String {
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw DriveException("네트워크에 연결할 수 없습니다.", e)
        }

        response.use {
            val body = it.body?.string().orEmpty()
            if (it.isSuccessful) return body

            throw DriveException(
                when (it.code) {
                    400 -> "요청이 올바르지 않습니다. API 키를 확인해주세요."
                    403 -> "드라이브 접근이 거부되었습니다. API 키 제한 설정이나 폴더 공유 설정을 확인해주세요."
                    // 익명 호출에는 비공개 파일의 존재 자체를 숨기려고 403 대신 404 를 준다.
                    // 즉 404 는 "없는 폴더"보다 "공개되지 않은 폴더"인 경우가 훨씬 많다.
                    404 -> "폴더에 접근할 수 없습니다. 폴더 ID 가 맞는지, " +
                        "드라이브 공유 설정이 '링크가 있는 모든 사용자'인지 확인해주세요."
                    in 500..599 -> "드라이브 서버가 응답하지 않습니다. 잠시 후 다시 시도해주세요."
                    else -> "드라이브 요청이 실패했습니다. (HTTP ${it.code})"
                }
            )
        }
    }
}
