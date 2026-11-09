# MemoryHouse

Google Drive 의 가족사진 폴더를 사진첩처럼 보는 안드로이드 앱. Play Store 배포용이 아니라 가족끼리 APK 로 나눠 쓰는 용도다.

## 동작 방식

대상 폴더가 **링크 공개(anyone with the link)** 상태라서 Google 로그인 없이 동작한다.

- **폴더/파일 목록**: Drive REST API `files.list` — **API 키 필요**
- **이미지 다운로드**: `https://drive.google.com/thumbnail?id=...&sz=w...` — 인증 불필요

앱 진입 시 4자리 번호를 받는다. 이 번호는 앱 화면을 가리는 잠금이지 Drive 폴더의 접근 권한을 통제하지 않는다. 폴더 ID 를 아는 사람은 브라우저로 사진을 볼 수 있다.

## 사진첩 여러 개 운영

**잠금 번호가 어느 사진첩을 열지 결정한다.** 잠금 화면은 앨범이 몇 개인지 드러내지 않고,
맞는 번호를 넣으면 그 앨범으로 바로 들어간다. 상단 자물쇠 버튼을 누르면 잠금 화면으로 돌아가
다른 번호로 다른 사진첩을 열 수 있다.

앨범을 가르는 것은 **폴더**다. API 키는 Google Cloud 프로젝트 자격증명일 뿐이라 같은 키로
여러 폴더를 볼 수 있다. **두 폴더가 서로 다른 구글 계정에 있을 때만** 키를 따로 발급하면 된다.

**앨범을 켜고 끄는 것은 `local.properties` 하나로 끝난다.** 앨범 번호별로 세 값을 넣는다.

```properties
# local.properties  (git 에 올라가지 않음)
album1.pin=1234
album1.apiKey=AIzaSy...
album1.folderId=1wxZd3VX_If795ps90BECODAXxcs2TYk8

album2.pin=5678
album2.apiKey=                     # 비우면 album1 의 키를 재사용한다
album2.folderId=1abcDEF...
```

- `pin` / `apiKey` / `folderId` 중 하나라도 비면 그 앨범은 목록에서 빠진다. 2번을 안 쓰면
  그냥 비워두면 되고, 1번만 동작한다.
- `pin` 은 정확히 4자리여야 한다.
- **두 앨범에 같은 번호를 넣지 말 것.** 겹치면 앞의 앨범이 이긴다 (`AlbumTest` 가 검사한다).
- 앨범을 3개 이상으로 늘리려면 [`Config.kt`](app/src/main/java/com/mysticagit/memoryhouse/Config.kt)
  의 `declared` 에 항목을 추가하고 `build.gradle.kts` 에 `ALBUM3_PIN` / `ALBUM3_API_KEY` /
  `ALBUM3_FOLDER_ID` 를 같은 방식으로 더하면 된다.

화면 상단에 뜨는 **앨범 이름은 앱에 적어두지 않는다.** 드라이브에서 그 폴더의 이름을 그대로
읽어오므로(`files.get`), 드라이브에서 폴더명을 바꾸면 앱에도 바로 반영된다.

### folderId 는 어디서 얻나

브라우저에서 드라이브를 열고 해당 폴더로 들어간 뒤, 주소창의 `/folders/` 다음 부분이 folderId 다.

```
https://drive.google.com/drive/u/0/folders/1wxZd3VX_If795ps90BECODAXxcs2TYk8
                                           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 이 부분
```

폴더 우클릭 → **공유 → 링크 복사** 로 얻은 링크에서도 같은 값을 볼 수 있다. `?usp=...` 같은
뒤쪽 쿼리는 떼고 ID 만 넣는다.

**그리고 그 폴더의 공유 설정을 `링크가 있는 모든 사용자`(뷰어)로 바꿔야 한다.** 이 앱은 OAuth
로그인을 하지 않고 API 키로만 조회하기 때문에, 비공개 폴더는 403 이 난다. 하위 폴더는 상위
설정을 상속하므로 최상단 폴더 한 번만 바꾸면 된다.

## 최초 설정

### 1. Drive API 키 발급

1. [Google Cloud Console](https://console.cloud.google.com/) 에서 프로젝트 생성 (기존 프로젝트도 무방)
2. **API 및 서비스 → 라이브러리** → `Google Drive API` 검색 → **사용 설정**
3. **API 및 서비스 → 사용자 인증 정보 → 사용자 인증 정보 만들기 → API 키**
4. 만들어진 키의 **키 수정** 에서 제한을 걸어둔다
   - 애플리케이션 제한사항: **Android 앱**
   - 패키지 이름 `com.mysticagit.memoryhouse` + 서명 인증서 지문(SHA-1) 등록
   - API 제한사항: **Google Drive API** 만 선택

> 디버그 빌드와 릴리즈 빌드는 서명 키가 다르므로 **SHA-1 을 둘 다 등록**해야 한다.
> 디버그 키 지문: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`

### 2. local.properties 채우기

`local.properties` 는 git 에 올라가지 않는다. 앨범 하나당 세 줄이다.

```properties
album1.pin=1234
album1.apiKey=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
album1.folderId=1wxZd3VX_If795ps90BECODAXxcs2TYk8
```

키는 `AIzaSy` 로 시작하는 39자 문자열이다. `folderId` 는 [위](#folderid-는-어디서-얻나) 참고.
쓸 수 있는 앨범이 하나도 없으면 앱이 설정 안내 화면을 띄운다.

> **local.properties 에는 ASCII 만 넣는다.** Android Studio 는 `.properties` 를 Java 규격대로
> ISO-8859-1 로 표시해서, UTF-8 로 저장된 한글이 편집기에서 깨져 보인다(파일 자체는 멀쩡하다).
> 앨범 이름은 드라이브에서 읽어오므로 여기에 한글을 넣을 일이 없다.

### 3. 빌드

```bash
./gradlew :app:assembleDebug
```

가족 배포는 Play Console **내부 테스트** 트랙으로 한다. 아래 "배포" 항목 참고.

## 배포 (Play Console 내부 테스트)

Play 스토어에 프로덕션으로 올리지 않고, **내부 테스트 트랙**으로만 가족에게 배포한다.
APK 를 직접 나눠주는 것과 비교해 두 가지가 해결된다.

- 설치할 때 "출처를 알 수 없는 앱" 설정을 켜지 않아도 된다 (Play 에서 설치되므로)
- **Wi-Fi 자동 업데이트**가 동작한다. 새 빌드를 올리면 가족 단말이 알아서 받는다

### 릴리즈 서명

키스토어 파일은 계정 단위로 하나만 두고, 앱마다 alias 를 따로 만든다.
파일 하나만 백업하면 되고, 앱 하나의 키를 바꿔도 다른 앱에 영향이 없다.
Play App Signing 을 켜면 이 키는 **업로드 키**로만 쓰인다.

```bash
# 최초 1회. 이후 다른 앱은 같은 파일에 -alias 만 바꿔 추가한다.
keytool -genkey -v -keystore ~/mysticagit-upload.jks -alias memoryhouse \
        -keyalg RSA -keysize 2048 -validity 10000
```

`local.properties` 에 경로와 비밀번호를 넣는다(git 에 올라가지 않는다).
`local.properties.example` 을 복사해 채우면 된다.

```properties
release.storeFile=~/mysticagit-upload.jks
release.storePassword=...
release.keyAlias=memoryhouse
release.keyPassword=...   # PKCS12 키스토어는 storePassword 와 같다
```

키스토어와 비밀번호는 프로젝트 밖에 따로 백업한다. 분실하면 업데이트를 올릴 수 없다.

키스토어가 없으면 서명 없이 빌드된다(다른 PC 에서도 빌드 자체는 깨지지 않게 하기 위함).

### 업로드할 파일 만들기

```bash
./gradlew :app:bundleRelease     # app/build/outputs/bundle/release/app-release.aab
```

Play 는 신규 앱에 APK 가 아닌 **AAB** 를 요구한다.

### 업데이트 절차

1. `version.properties` 의 `versionCode` 를 1 올린다 (안 올리면 Play 가 업로드를 거부한다)
2. `./gradlew :app:bundleRelease`
3. Play Console → 내부 테스트 → 새 버전 만들기 → AAB 업로드 → 출시

심사가 없어 몇 분 내 배포되고, 가족 단말은 Wi-Fi 환경에서 자동으로 받는다.

> **주의**: 사이드로딩으로 설치해둔 기존 앱은 서명이 달라 Play 버전으로 업데이트되지 않는다.
> 가족 단말에서 기존 앱을 **삭제한 뒤** Play 링크로 다시 설치해야 한다. 최초 1회만 겪으면 된다.

> API 키에는 Android 앱 제한을 걸지 않았으므로(이 앱은 attestation 헤더를 보내지 않는다),
> Play App Signing 으로 서명 키가 바뀌어도 영향이 없다. API 제한(Drive API 만)은 그대로 유효하다.

## 데이터 사용량

**원본은 어떤 경로로도 받지 않는다.** 이미지 URL 은 전부 `DriveUrls` 를 거치고, 거기서 만드는
주소는 서버가 리사이즈해주는 `thumbnail?id=...&sz=wN` 하나뿐이다.

요청 폭은 고정값이 아니라 **기기 화면에 맞춰** 정해진다(`ui/ImageSizes.kt`). 사다리 값
(160/200/240/320/400/512/720/1080/1440/2048)으로 눌러서 기기마다 URL 이 갈려 캐시가 낭비되는 것을 막는다.

| 상황 | 요청 폭 | 비고 |
|---|---|---|
| 썸네일 그리드 | 사진 칸 폭에 맞춤 (최대 512) | 1080p 기기 → w320, 720p 기기 → w240 |
| 전체화면 (확대 안 함) | 화면 폭에 맞춤 (최대 1440) | 1080p 기기 → w1080 |
| 전체화면 (핀치 확대) | 기본의 2배 (최대 2048) | **확대할 때만** 추가로 받는다 |

1080p 기기 실측 (`240729 을왕리`, 사진 26장):

| | 크기 |
|---|---|
| 원본 전부 | 54.7 MB |
| 폴더 훑어보기(그리드 26장) | **1.23 MB** (2.2%) |
| 사진 1장 전체화면 추가 | +0.49 MB |

한 번 받은 이미지는 Coil 디스크 캐시(최대 256MB)에 남아 다시 받지 않는다. Drive 썸네일 응답의
캐시 헤더를 그대로 따르면 디스크 캐시가 거의 동작하지 않으므로 `respectCacheHeaders(false)` 로 껐다.

> 더 줄이고 싶으면: 이동통신(LTE) 구간에서만 사다리를 한 칸 낮추는 방법이 있다.
> 그리드가 w320 → w240 이 되면 폴더당 1.23MB → 0.73MB (-41%). 다만 네트워크가 바뀔 때마다
> URL 이 달라져 캐시가 갈리는 단점이 있어 지금은 넣지 않았다.

## 전체화면 뷰어

- 확대하지 않은 상태에서 **좌우 스와이프**로 이전/다음 사진으로 넘어간다. 목록의 처음과 끝에서는
  더 넘어가지 않는다(순환하지 않음).
- **핀치**로 확대/축소, 확대 상태에서 드래그로 이동, **더블탭**으로 확대와 원래 크기를 오간다.
- 확대 중에는 스와이프가 잠긴다. 드래그는 사진을 움직이는 데 쓰인다.
- 사진을 넘기면 확대 상태가 초기화된다.
- Android 백키로 닫으면 썸네일 그리드로 돌아온다.

## 백키 동작

한 번에 한 단계씩 되짚어 나온다.

| 상태 | 백키 |
|---|---|
| 전체화면 뷰어 | 뷰어 닫기 → 썸네일 그리드 |
| 하위 폴더 | 상위 폴더로 이동 |
| 최상단 폴더 / 잠금 화면 | **한 번 더 눌러야 종료** (토스트로 안내, 2초 이내) |

종료 후 다시 실행하면 **잠금 화면부터** 시작한다. 잠금 해제 상태를 `rememberSaveable` 로
저장하지 않기 때문에(→ `remember`), OS 가 프로세스를 회수한 뒤 복귀하는 경우에도 잠금이 유지된다.
화면 회전 등은 manifest 의 `configChanges` 로 액티비티가 유지되므로 잠금이 풀리지 않는다.

> 구현 주의: 확대 제스처를 `detectTransformGestures` 로 잡으면 한 손가락 드래그까지 소비해버려
> `HorizontalPager` 가 스와이프를 못 받는다. 그래서 `transformable(canPan = { isZoomed })` 로
> **확대 중일 때만** 팬을 가져가도록 했다.

## 표시 대상

`mimeType contains 'image/'` 로 걸러서 이미지만 보여준다. 문서와 **동영상(.MOV 등)은 목록에 나오지 않는다.**

## 앱 아이콘

하늘색 배경 + 브라운 톤 앨범(겹친 사진 + 표지 안의 산·해). 도형은
[`tools/icon/geom.py`](tools/icon/geom.py) 에 108x108 좌표로 정의되어 있고,
아래 명령으로 리소스를 다시 생성한다.

```bash
python3 tools/icon/build_icons.py
```

만들어지는 것:

| 대상 | 파일 |
|---|---|
| 적응형 아이콘 (API 26+) | `drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml` |
| 테마 아이콘 (Android 13+) | `drawable/ic_launcher_monochrome.xml` |
| 레거시 비트맵 (API 24~25) | `mipmap-{mdpi..xxxhdpi}/ic_launcher{,_round}.webp` |

런처가 원형 마스크를 씌우면 가운데 지름 66dp 만 확실히 보이므로, `geom.py` 의 `SCALE` 로
도형을 그 안에 들어오도록 줄여두었다. 좌표를 손볼 때 이 값을 함께 확인할 것
(밑그림 그대로는 표지 모서리가 잘린다).

## 버전 관리

버전은 저장소 루트의 [`version.properties`](version.properties) **한 곳에서만** 관리한다.

```properties
versionName=1.0.0
versionCode=1
```

이 값이 `build.gradle.kts` 를 거쳐 APK 의 `versionName`/`versionCode` 와 `BuildConfig` 로 함께
들어가므로, 화면에 보이는 버전과 APK 에 찍힌 버전이 어긋날 일이 없다.
화면에서는 [`AppInfo`](app/src/main/java/com/mysticagit/memoryhouse/AppInfo.kt) 를 통해 읽는다
(`AppInfo.displayVersion` → `"v1.0.0"`, 잠금 화면 하단에 표시).

배포할 때는 `versionCode` 를 1 씩 올린다.

## 구조

| 파일 | 역할 |
|---|---|
| `Config.kt` | 앨범 목록(잠금번호/API 키/폴더 ID), 번호로 앨범 찾기 |
| `AppInfo.kt` | 앱 버전 (version.properties → BuildConfig 경유) |
| `MemoryHouseApp.kt` | OkHttp / Coil ImageLoader(캐시 설정) |
| `data/DriveRepository.kt` | `files.list` / `files.get` 호출 (공식 SDK 대신 직접 호출) |
| `data/DriveUrls.kt` | 썸네일 URL 생성, 해상도 사다리 |
| `ui/ImageSizes.kt` | 기기별 요청 폭 계산 (그리드/전체화면이 같은 값을 공유) |
| `ui/BrowserViewModel.kt` | 폴더 스택, 페이지네이션, 폴더 표지 |
| `ui/BrowserScreen.kt` | 폴더/사진 그리드 |
| `ui/ViewerScreen.kt` | 전체화면 뷰어 (스와이프로 넘기기 + 핀치 확대) |
| `ui/ZoomState.kt` | 확대/이동 계산 |
| `ui/ExitOnDoubleBack.kt` | 백키 두 번으로 종료 (토스트 안내) |
| `ui/PinScreen.kt` | 잠금 화면 (앱 아이콘 로고, 버전 표시) |

## 테스트

```bash
./gradlew :app:testDebugUnitTest        # 요청 폭 결정 규칙 (기기 불필요)
./gradlew :app:connectedDebugAndroidTest # 확대 계산 + 제스처 (기기 필요)
```

- `AlbumTest` — 앨범이 쓸 수 있는 상태인지 판단하는 규칙, 번호로 앨범 찾기, 번호 중복 검사.
- `DriveUrlsTest` — 기기별 요청 폭, 상한, 캐시 키. 원본 URL 을 만들지 않는지도 확인한다.
- `ZoomStateTest` — 확대 배율 제한과 이동 범위 계산.
- `ViewerGestureTest` — 더블탭/핀치 확대, 스와이프로 앞뒤 넘기기, 양쪽 끝에서 멈추기,
  확대 중 스와이프 잠금, 넘길 때 확대 초기화.

### 목록 API 없이 화면만 확인하기

`drive.apiBaseUrl` 을 로컬 모의 서버로 돌리면 API 키 없이 UI 를 볼 수 있다. 디버그 빌드에서만 평문 HTTP 가 허용된다(`app/src/debug/AndroidManifest.xml`).

```properties
drive.apiBaseUrl=http://10.0.2.2:8777
```
