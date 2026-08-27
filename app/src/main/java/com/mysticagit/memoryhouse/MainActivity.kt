package com.mysticagit.memoryhouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mysticagit.memoryhouse.ui.BrowserScreen
import com.mysticagit.memoryhouse.ui.BrowserViewModel
import com.mysticagit.memoryhouse.ui.PinScreen
import com.mysticagit.memoryhouse.ui.SetupRequiredScreen
import com.mysticagit.memoryhouse.ui.ViewerScreen
import com.mysticagit.memoryhouse.ui.rememberExitOnDoubleBack
import com.mysticagit.memoryhouse.ui.theme.MemoryHouseTheme

private const val LOCKED = -1

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MemoryHouseApp

        setContent {
            MemoryHouseTheme {
                when {
                    !Config.isConfigured -> SetupRequiredScreen()
                    else -> AlbumRoot(app)
                }
            }
        }
    }
}

@Composable
private fun AlbumRoot(app: MemoryHouseApp) {
    // 어느 사진첩을 열었는지. 잠금 번호가 앨범을 고른다.
    //
    // rememberSaveable 이 아니라 remember 다. 저장해두면 OS 가 프로세스를 회수한 뒤
    // 복귀할 때 잠금을 건너뛰게 되므로, 잠금 화면으로서는 오히려 구멍이 된다.
    // 화면 회전 등은 manifest 의 configChanges 로 액티비티가 유지되어 풀리지 않는다.
    var albumIndex by remember { mutableIntStateOf(LOCKED) }
    val album = Config.albums.getOrNull(albumIndex)

    val exitOnDoubleBack = rememberExitOnDoubleBack()

    if (album == null) {
        // 잠금 화면에서 백키를 누르면 앱 종료로 이어지므로 여기서도 한 번 확인한다.
        BackHandler { exitOnDoubleBack() }
        PinScreen(onUnlocked = { unlocked -> albumIndex = Config.albums.indexOf(unlocked) })
        return
    }

    // 앨범이 바뀌면 폴더 스택과 캐시가 섞이지 않도록 ViewModel 을 새로 만든다.
    val viewModel: BrowserViewModel = viewModel(
        key = album.folderId,
        factory = BrowserViewModel.Factory(app.driveRepository(album.apiKey), album),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 백키 한 번에 한 단계씩: 전체화면 닫기 -> 상위 폴더 -> 최상단에서는 두 번 눌러 종료.
    BackHandler {
        when {
            state.viewing != null -> viewModel.closeImage()
            !state.isAtRoot -> viewModel.navigateUp()
            else -> exitOnDoubleBack()
        }
    }

    // 전체화면 뷰어는 그리드 위에 덮어씌운다.
    Box(modifier = Modifier.fillMaxSize()) {
        BrowserScreen(
            state = state,
            onFolderClick = viewModel::openFolder,
            onImageClick = viewModel::openImage,
            onUpClick = { viewModel.navigateUp() },
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            onErrorShown = viewModel::dismissError,
            onLock = { albumIndex = LOCKED },
        )

        state.viewingIndex?.let { index ->
            ViewerScreen(
                images = state.images,
                initialIndex = index,
                onIndexChange = viewModel::setViewingIndex,
                onClose = viewModel::closeImage,
            )
        }
    }
}
