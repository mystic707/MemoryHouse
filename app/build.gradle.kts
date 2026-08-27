import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// 비밀 값은 git 에 올라가지 않는 local.properties 에서 읽는다.
// 값이 없으면 빈 문자열이 들어가고, 앱 실행 시 설정 안내 화면이 뜬다.
// Properties.load(InputStream) 은 ISO-8859-1 로 읽으므로 UTF-8 리더를 쓴다.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.reader(Charsets.UTF_8).use { load(it) }
}

fun secret(key: String, fallback: String = ""): String =
    (localProperties.getProperty(key) ?: fallback).trim()

// 버전은 version.properties 한 곳에서만 관리한다.
val versionProperties = Properties().apply {
    rootProject.file("version.properties").reader(Charsets.UTF_8).use { load(it) }
}
val appVersionName: String = versionProperties.getProperty("versionName").trim()
val appVersionCode: Int = versionProperties.getProperty("versionCode").trim().toInt()

android {
    namespace = "com.mysticagit.memoryhouse"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mysticagit.memoryhouse"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // local.properties 에는 ASCII 값만 둔다. 한글 앨범 이름은 Config.kt 에 있다.
        buildConfigField("String", "ALBUM1_PIN", "\"${secret("album1.pin")}\"")
        buildConfigField("String", "ALBUM1_API_KEY", "\"${secret("album1.apiKey")}\"")
        buildConfigField("String", "ALBUM1_FOLDER_ID", "\"${secret("album1.folderId")}\"")
        buildConfigField("String", "ALBUM2_PIN", "\"${secret("album2.pin")}\"")
        buildConfigField("String", "ALBUM2_API_KEY", "\"${secret("album2.apiKey")}\"")
        buildConfigField("String", "ALBUM2_FOLDER_ID", "\"${secret("album2.folderId")}\"")
        // 목록 API 주소. 로컬 모의 서버로 UI 를 확인할 때만 바꾼다.
        buildConfigField(
            "String",
            "DRIVE_API_BASE_URL",
            "\"${secret("drive.apiBaseUrl", "https://www.googleapis.com")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
