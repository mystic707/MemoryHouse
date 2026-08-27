package com.mysticagit.memoryhouse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mysticagit.memoryhouse.Album
import com.mysticagit.memoryhouse.data.DriveException
import com.mysticagit.memoryhouse.data.DriveItem
import com.mysticagit.memoryhouse.data.DriveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 탐색 스택에 쌓이는 폴더 한 칸. */
data class FolderRef(val id: String, val name: String)

/** 최상단 폴더 이름을 드라이브에서 받아오기 전까지 쓰는 임시 문구. */
private const val PENDING_ROOT_NAME = "사진첩"

/** 한 폴더에서 지금까지 읽어온 내용. 뒤로 갈 때 다시 받지 않으려고 들고 있는다. */
private data class FolderContent(
    val items: List<DriveItem> = emptyList(),
    val nextPageToken: String? = null,
    val loadedFirstPage: Boolean = false,
)

data class BrowserUiState(
    val breadcrumb: List<FolderRef> = emptyList(),
    val items: List<DriveItem> = emptyList(),
    /** [items] 중 사진만. 전체화면에서 좌우로 넘길 때의 순서가 된다. */
    val images: List<DriveItem.Image> = emptyList(),
    /** 폴더 id -> 표지로 쓸 이미지 파일 id. */
    val covers: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    /** 전체화면으로 보고 있는 사진의 [images] 상 위치. null 이면 그리드 화면. */
    val viewingIndex: Int? = null,
) {
    val title: String get() = breadcrumb.lastOrNull()?.name.orEmpty()
    val isAtRoot: Boolean get() = breadcrumb.size <= 1
    val isEmpty: Boolean get() = items.isEmpty() && !isLoading && error == null
    val viewing: DriveItem.Image? get() = viewingIndex?.let { images.getOrNull(it) }
}

class BrowserViewModel(
    private val repository: DriveRepository,
    album: Album,
) : ViewModel() {

    // 최상단 이름은 드라이브에서 읽어올 때까지 임시 문구로 둔다.
    private val stack = mutableListOf(FolderRef(album.folderId, PENDING_ROOT_NAME))
    private val contents = mutableMapOf<String, FolderContent>()
    private val coverCache = mutableMapOf<String, String>()
    private val coverRequests = mutableSetOf<String>()

    private val _uiState = MutableStateFlow(BrowserUiState(breadcrumb = stack.toList()))
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    init {
        loadCurrentFolder()
        resolveRootName()
    }

    /**
     * 최상단 폴더 이름을 드라이브에서 가져온다.
     * 앱에 이름을 적어두지 않으므로, 드라이브에서 폴더명을 바꾸면 앱에도 그대로 반영된다.
     * 실패하면 임시 문구가 남지만 사진 보기에는 지장이 없다.
     */
    private fun resolveRootName() {
        viewModelScope.launch {
            try {
                val name = repository.folderName(stack.first().id)
                if (name.isNullOrBlank()) return@launch
                stack[0] = stack[0].copy(name = name)
                publish()
            } catch (e: DriveException) {
                // 최상단 폴더에 접근조차 못 하는 상태다. 목록 조회는 빈 배열을 돌려주므로
                // 이걸 놓치면 "사진이 없어요" 로 보여서 공유 설정 문제를 놓치게 된다.
                _uiState.update { it.copy(error = it.error ?: e.message) }
            }
        }
    }

    private val currentFolder: FolderRef get() = stack.last()

    fun openFolder(folder: DriveItem.Folder) {
        if (_uiState.value.viewing != null) return
        stack.add(FolderRef(folder.id, folder.name))
        publish()
        loadCurrentFolder()
    }

    /** 상위 폴더로 한 칸 이동. 최상단이라 더 올라갈 곳이 없으면 false. */
    fun navigateUp(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        publish()
        loadCurrentFolder()
        return true
    }

    fun openImage(image: DriveItem.Image) = _uiState.update { state ->
        val index = state.images.indexOfFirst { it.id == image.id }
        if (index < 0) state else state.copy(viewingIndex = index)
    }

    /** 전체화면에서 좌우로 넘겼을 때 현재 위치를 맞춘다. */
    fun setViewingIndex(index: Int) = _uiState.update { state ->
        if (state.viewingIndex == null || index !in state.images.indices) state
        else state.copy(viewingIndex = index)
    }

    fun closeImage() = _uiState.update { it.copy(viewingIndex = null) }

    fun retry() {
        loadCurrentFolder(forceReload = true)
        // 폴더 이름을 아직 못 받았다면(접근 불가였다면) 다시 시도한다.
        resolveRootName()
    }

    private fun loadCurrentFolder(forceReload: Boolean = false) {
        val folder = currentFolder
        val cached = contents[folder.id]

        if (!forceReload && cached != null && cached.loadedFirstPage) {
            // 이전 폴더에서 돌던 요청이 남긴 로딩/오류 표시를 함께 걷어낸다.
            _uiState.update { it.copy(isLoading = false, isLoadingMore = false, error = null) }
            publish()
            requestCoversFor(cached.items)
            return
        }

        if (forceReload) contents.remove(folder.id)
        _uiState.update { it.copy(isLoading = true, error = null, items = emptyList()) }

        viewModelScope.launch {
            try {
                val page = repository.listChildren(folder.id)
                // 로딩 중에 사용자가 다른 폴더로 넘어갔다면 결과를 버린다.
                if (currentFolder.id != folder.id) return@launch

                contents[folder.id] = FolderContent(
                    items = page.items,
                    nextPageToken = page.nextPageToken,
                    loadedFirstPage = true,
                )
                _uiState.update { it.copy(isLoading = false) }
                publish()
                requestCoversFor(page.items)
            } catch (e: DriveException) {
                if (currentFolder.id != folder.id) return@launch
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadMore() {
        val folder = currentFolder
        val content = contents[folder.id] ?: return
        val token = content.nextPageToken ?: return
        if (_uiState.value.isLoadingMore || _uiState.value.isLoading) return

        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            try {
                val page = repository.listChildren(folder.id, token)
                if (currentFolder.id != folder.id) return@launch

                contents[folder.id] = content.copy(
                    items = content.items + page.items,
                    nextPageToken = page.nextPageToken,
                )
                _uiState.update { it.copy(isLoadingMore = false) }
                publish()
                requestCoversFor(page.items)
            } catch (e: DriveException) {
                if (currentFolder.id != folder.id) return@launch
                // 추가 페이지 실패는 이미 보고 있는 목록을 지우지 않고 메시지만 띄운다.
                _uiState.update { it.copy(isLoadingMore = false, error = e.message) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    /** 폴더 카드에 깔아줄 표지 이미지를 폴더마다 한 번씩만 조회한다. */
    private fun requestCoversFor(items: List<DriveItem>) {
        val targets = items.filterIsInstance<DriveItem.Folder>()
            .map { it.id }
            .filter { it !in coverCache && coverRequests.add(it) }
        if (targets.isEmpty()) return

        // 폴더마다 따로 띄운다. 순차로 돌리면 폴더 수만큼 왕복을 기다려 표지가 하나씩 늦게 뜬다.
        // 실제 동시 요청 수는 OkHttp 디스패처가 호스트당 상한으로 잡아준다.
        for (folderId in targets) {
            viewModelScope.launch {
                val cover = runCatching { repository.findCoverImage(folderId) }.getOrNull()
                if (cover != null) {
                    coverCache[folderId] = cover.id
                    publish()
                }
            }
        }
    }

    private fun publish() {
        val content = contents[currentFolder.id]
        val items = content?.items.orEmpty()
        val images = items.filterIsInstance<DriveItem.Image>()
        _uiState.update {
            it.copy(
                breadcrumb = stack.toList(),
                items = items,
                images = images,
                covers = coverCache.toMap(),
                canLoadMore = content?.nextPageToken != null,
                // 목록이 줄어들어 보고 있던 사진이 사라지면 전체화면을 닫는다.
                viewingIndex = it.viewingIndex?.takeIf { i -> i in images.indices },
            )
        }
    }

    class Factory(
        private val repository: DriveRepository,
        private val album: Album,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BrowserViewModel(repository, album) as T
    }
}
