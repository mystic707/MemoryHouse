package com.mysticagit.memoryhouse.ui

import android.app.Activity
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** 두 번째 백키를 이 시간 안에 눌러야 종료된다. */
private const val EXIT_CONFIRM_WINDOW_MS = 2000L

private const val EXIT_HINT = "한번더 뒤로가기 시 앱이 종료됩니다."

/**
 * 백키를 두 번 눌러야 앱이 종료되도록 하는 콜백을 만든다.
 *
 * 첫 번째 호출에서는 안내 토스트만 띄우고, [EXIT_CONFIRM_WINDOW_MS] 안에 다시 호출되면
 * 액티비티를 끝낸다. 시간이 지나면 다시 처음부터다.
 *
 * 시계를 되돌려도 영향이 없도록 벽시계 대신 [SystemClock.elapsedRealtime] 을 쓴다.
 */
@Composable
fun rememberExitOnDoubleBack(): () -> Unit {
    val context = LocalContext.current
    var firstBackAt by remember { mutableLongStateOf(0L) }
    // 연달아 누를 때 토스트가 쌓이지 않도록 이전 것을 치운다.
    val toast = remember { arrayOfNulls<Toast>(1) }

    return {
        val now = SystemClock.elapsedRealtime()
        if (now - firstBackAt <= EXIT_CONFIRM_WINDOW_MS) {
            toast[0]?.cancel()
            (context as? Activity)?.finish()
        } else {
            firstBackAt = now
            toast[0]?.cancel()
            toast[0] = Toast.makeText(context, EXIT_HINT, Toast.LENGTH_SHORT).also { it.show() }
        }
    }
}
