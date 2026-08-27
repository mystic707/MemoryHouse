package com.mysticagit.memoryhouse.data

/**
 * Drive 이미지 URL 생성기.
 *
 * 링크 공개(anyone with the link) 폴더의 파일은 이 썸네일 엔드포인트로 인증 없이 받을 수 있고,
 * sz 파라미터로 서버가 리사이즈해준 결과만 내려받게 된다. **원본은 절대 받지 않는다.**
 *
 * 실측(3,257KB 사진 1장):
 *
 *   w200 ->  23KB (0.7%)   w320 ->  48KB (1.5%)   w1080 -> 361KB (11.4%)
 *   w240 ->  30KB (1.0%)   w400 ->  73KB (2.3%)   w1600 -> 678KB (21.3%)
 *                                                 w2048 -> 988KB (31.0%)
 *
 * 요청 폭은 화면에서 실제로 차지하는 픽셀에 맞춰 정하되, [WIDTH_LADDER] 값으로 눌러서
 * 기기마다 URL 이 미세하게 달라져 캐시가 갈리는 일을 막는다.
 */
object DriveUrls {

    /** 요청에 쓰는 폭 후보. 캐시 적중을 위해 이 값들만 사용한다. */
    private val WIDTH_LADDER = intArrayOf(160, 200, 240, 320, 400, 512, 720, 1080, 1440, 2048)

    /** 썸네일 그리드가 요청할 수 있는 최대 폭. 태블릿에서 과하게 커지는 것을 막는다. */
    private const val MAX_GRID_WIDTH = 512

    /** 전체화면 기본 요청의 최대 폭. 확대하면 [zoomedWidthFor] 로 올라간다. */
    private const val MAX_FULL_WIDTH = 1440

    /** 확대 상태에서 요청할 수 있는 최대 폭. */
    private const val MAX_ZOOM_WIDTH = 2048

    private fun thumbnail(fileId: String, width: Int): String =
        "https://drive.google.com/thumbnail?id=$fileId&sz=w$width"

    /** [targetPx] 를 담을 수 있는 가장 작은 사다리 값. */
    private fun snap(targetPx: Int, max: Int): Int {
        // 5% 는 눈에 띄지 않으므로, 살짝 모자란 값은 한 단계 낮춰서 데이터를 아낀다.
        val needed = (targetPx * 0.95f).toInt()
        return WIDTH_LADDER.firstOrNull { it >= needed }?.coerceAtMost(max) ?: max
    }

    fun gridWidthFor(cellWidthPx: Int): Int = snap(cellWidthPx, MAX_GRID_WIDTH)

    fun fullWidthFor(screenWidthPx: Int): Int = snap(screenWidthPx, MAX_FULL_WIDTH)

    /** 핀치로 확대했을 때 쓸 폭. 기본의 2배를 노린다(확대 안 하면 이건 아예 받지 않는다). */
    fun zoomedWidthFor(baseWidth: Int): Int =
        snap(baseWidth * 2, MAX_ZOOM_WIDTH).coerceAtLeast(baseWidth)

    fun gridThumbnail(fileId: String, width: Int): String = thumbnail(fileId, width)

    fun fullScreen(fileId: String, width: Int): String = thumbnail(fileId, width)

    /**
     * Coil 메모리 캐시 키.
     *
     * 기본 키는 요청 크기까지 섞여서 만들어지기 때문에, 전체화면에서 그리드가 이미 받아둔
     * 썸네일을 자리끼우개(placeholder)로 재사용하려면 이렇게 직접 지정해야 한다.
     */
    fun cacheKey(fileId: String, width: Int): String = "$fileId@w$width"
}
