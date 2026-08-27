package com.mysticagit.memoryhouse.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mysticagit.memoryhouse.Album
import com.mysticagit.memoryhouse.AppInfo
import com.mysticagit.memoryhouse.Config
import com.mysticagit.memoryhouse.R
import kotlinx.coroutines.delay

/**
 * 4자리 번호를 받는 잠금 화면.
 *
 * 번호가 어느 사진첩을 열지까지 결정한다. 화면에는 앨범이 몇 개인지 드러내지 않고,
 * 맞는 번호를 넣으면 그 앨범으로 바로 들어간다.
 *
 * 이 잠금은 앱 화면을 가리는 용도다. 드라이브 폴더 자체는 링크 공개 상태이므로
 * 번호가 사진의 접근 권한을 통제하지는 않는다.
 */
@Composable
fun PinScreen(onUnlocked: (Album) -> Unit) {
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    // 틀렸을 때 좌우로 흔들어서 알려준다.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(wrong) {
        if (!wrong) return@LaunchedEffect
        repeat(2) {
            shake.animateTo(14f, tween(durationMillis = 50))
            shake.animateTo(-14f, tween(durationMillis = 50))
        }
        shake.animateTo(0f, tween(durationMillis = 50))
    }

    LaunchedEffect(entered) {
        if (entered.length < Config.PIN_LENGTH) return@LaunchedEffect
        val album = Config.albumForPin(entered)
        if (album != null) {
            onUnlocked(album)
        } else {
            wrong = true
            delay(400)
            entered = ""
            wrong = false
        }
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(MaterialTheme.shapes.large),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "MemoryHouse",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (wrong) "번호가 맞지 않아요" else "앨범 pin ${Config.PIN_LENGTH}자리를 입력해주세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (wrong) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.graphicsLayer { translationX = shake.value },
                ) {
                    repeat(Config.PIN_LENGTH) { index ->
                        val filled = index < entered.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        wrong -> MaterialTheme.colorScheme.error
                                        filled -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                Keypad(
                    enabled = !wrong,
                    onDigit = { digit ->
                        if (entered.length < Config.PIN_LENGTH) entered += digit
                    },
                    onBackspace = { entered = entered.dropLast(1) },
                )
            }

            Text(
                text = AppInfo.displayVersion,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun Keypad(
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫"),
    )

    Column(
        modifier = Modifier.widthIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (label in row) {
                    Box(modifier = Modifier.weight(1f)) {
                        when (label) {
                            "" -> Spacer(Modifier.fillMaxWidth().aspectRatio(1.4f))
                            "⌫" -> KeypadKey(enabled = enabled, onClick = onBackspace) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "지우기",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            else -> KeypadKey(enabled = enabled, onClick = { onDigit(label[0]) }) {
                                Text(
                                    text = label,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** API 키가 비어 있을 때 보여주는 안내 화면. */
@Composable
fun SetupRequiredScreen() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "설정이 필요합니다",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "쓸 수 있는 사진첩이 없습니다.\n" +
                    "local.properties 에 아래 세 줄을 채운 뒤 다시 빌드해주세요.\n" +
                    "(화면에 뜨는 앨범 이름은 Config.kt 에 있습니다)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = "album1.pin=1234\nalbum1.apiKey=AIza...\nalbum1.folderId=1wxZ...",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE8C39E),
                )
            }
        }
    }
}
