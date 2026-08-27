package com.mysticagit.memoryhouse

/**
 * 앱 자신에 대한 정보. 화면에서 버전을 표시할 때 여기를 쓴다.
 *
 * 버전 숫자는 저장소 루트의 `version.properties` 한 곳에서만 관리한다.
 * 그 값이 build.gradle.kts 를 거쳐 APK 의 versionName 과 BuildConfig 로 함께 들어오므로,
 * 화면에 보이는 버전과 APK 에 찍힌 버전이 어긋날 일이 없다.
 */
object AppInfo {

    /** 예: "1.0.0" */
    val versionName: String = BuildConfig.VERSION_NAME

    /** 배포 회차를 구분하는 정수. */
    val versionCode: Int = BuildConfig.VERSION_CODE

    /** 화면에 그대로 뿌릴 수 있는 형태. 예: "v1.0.0" */
    val displayVersion: String = "v$versionName"
}
