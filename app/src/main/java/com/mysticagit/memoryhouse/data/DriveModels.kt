package com.mysticagit.memoryhouse.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val MIME_FOLDER = "application/vnd.google-apps.folder"

/** Drive files.list 응답. 필요한 필드만 선언하고 나머지는 무시한다. */
@Serializable
data class DriveFileListResponse(
    @SerialName("nextPageToken") val nextPageToken: String? = null,
    @SerialName("files") val files: List<DriveFileDto> = emptyList(),
)

@Serializable
data class DriveFileDto(
    val id: String,
    val name: String,
    val mimeType: String,
    val modifiedTime: String? = null,
)

/** 화면에서 다루는 항목. 폴더와 이미지 두 가지뿐이다. */
sealed interface DriveItem {
    val id: String
    val name: String

    data class Folder(override val id: String, override val name: String) : DriveItem

    data class Image(
        override val id: String,
        override val name: String,
        val modifiedTime: String?,
    ) : DriveItem
}

fun DriveFileDto.toDriveItem(): DriveItem =
    if (mimeType == MIME_FOLDER) {
        DriveItem.Folder(id = id, name = name)
    } else {
        DriveItem.Image(id = id, name = name, modifiedTime = modifiedTime)
    }

/** 한 폴더의 목록 한 페이지. */
data class DrivePage(
    val items: List<DriveItem>,
    val nextPageToken: String?,
)
