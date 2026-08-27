package com.mysticagit.memoryhouse

/**
 * 사진첩 하나. 잠금 번호로 구분되고, 번호가 맞으면 그 앨범의 폴더만 보인다.
 *
 * 앨범을 가르는 것은 [folderId] 다. [apiKey] 는 Google Cloud 프로젝트 자격증명일 뿐이라
 * 같은 키로 여러 폴더를 볼 수 있다. 두 폴더가 서로 다른 구글 계정에 있을 때만 키를 갈라야 한다.
 *
 * 앨범 이름은 여기 없다. 드라이브에서 폴더 이름을 그대로 읽어온다.
 */
data class Album(
    val folderId: String,
    val pin: String,
    val apiKey: String,
) {
    val isConfigured: Boolean
        get() = pin.length == Config.PIN_LENGTH &&
            folderId.isNotBlank() &&
            apiKey.isNotBlank()
}

/**
 * 앱 전역 설정.
 *
 * 설정 값은 전부 local.properties 에 있다(잠금 번호 / API 키 / 폴더 ID).
 * 화면에 뜨는 폴더 이름은 드라이브에서 읽어오므로 앱에 적어둘 것이 없다.
 */
object Config {

    const val PIN_LENGTH = 4

    /** 목록 API 주소. 기본값은 실제 Drive 이고, UI 확인용 모의 서버로만 바꾼다. */
    val apiBaseUrl: String = BuildConfig.DRIVE_API_BASE_URL

    /**
     * 운영할 사진첩 목록. 여기 순서가 local.properties 의 album1. / album2. 와 짝을 이룬다.
     *
     * 앨범을 켜고 끄는 것은 전부 local.properties 에서 한다. pin / apiKey / folderId 중
     * 하나라도 비면 그 앨범은 목록에서 빠진다. 2번을 안 쓰면 그냥 비워두면 된다.
     */
    private val declared: List<Album> = listOf(
        Album(
            folderId = BuildConfig.ALBUM1_FOLDER_ID,
            pin = BuildConfig.ALBUM1_PIN,
            apiKey = BuildConfig.ALBUM1_API_KEY,
        ),
        Album(
            folderId = BuildConfig.ALBUM2_FOLDER_ID,
            pin = BuildConfig.ALBUM2_PIN,
            // 같은 구글 계정의 다른 폴더라면 키를 따로 발급할 필요가 없다.
            apiKey = BuildConfig.ALBUM2_API_KEY.ifBlank { BuildConfig.ALBUM1_API_KEY },
        ),
    )

    /** 실제로 쓸 수 있게 채워진 앨범만. */
    val albums: List<Album> = declared.filter { it.isConfigured }

    val isConfigured: Boolean get() = albums.isNotEmpty()

    /**
     * 입력된 번호에 해당하는 앨범. 없으면 null.
     *
     * 두 앨범에 같은 번호를 넣으면 앞의 앨범이 이긴다. 번호는 서로 다르게 둘 것.
     */
    fun albumForPin(pin: String): Album? = albums.firstOrNull { it.pin == pin }
}
