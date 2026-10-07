package com.company.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.company.core.domain.model.ImageItem
import com.company.ui.UiStatus

@Composable
fun HomeSearchResultSection(
    uiStatus: UiStatus,
    imageItems: LazyPagingItems<ImageItem>,
    onClickItem: (String, Boolean) -> Unit,
    onToggleFavorite: (item: ImageItem, isFavorited: Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (uiStatus) {
            UiStatus.Loading -> {
                CircularProgressIndicator()
            }

            UiStatus.Idle -> {
                InitialSearchScreen()
            }

            else -> {
                HomeSearchResult(
                    imageItems = imageItems,
                    onClickItem = onClickItem,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}

@Composable
fun HomeSearchResult(
    imageItems: LazyPagingItems<ImageItem>,
    onClickItem: (id: String, isVideo: Boolean) -> Unit,
    onToggleFavorite: (item: ImageItem, isFavorited: Boolean) -> Unit
) {
    val refreshState = imageItems.loadState.refresh
    val appendState = imageItems.loadState.append

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Search Results",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        handlePagingRefreshState(
            refreshState = refreshState,
            imageItems = imageItems,
            onClickItem = onClickItem,
            onToggleFavorite = onToggleFavorite
        )

        handlePagingAppendState(appendState = appendState, onRetry = imageItems::retry)
    }
}

@Composable
fun InitialSearchScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            modifier = Modifier.size(100.dp),
            tint = Color.Gray
        )
        Text(
            text = "Search for videos and images",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
    }
}

/**
 * 첫 페이지 로드(refresh) 상태를 처리하는 Composable
 */
private fun LazyGridScope.handlePagingRefreshState(
    refreshState: LoadState,
    imageItems: LazyPagingItems<ImageItem>,
    onClickItem: (id: String, isVideo: Boolean) -> Unit,
    onToggleFavorite: (item: ImageItem, isFavorited: Boolean) -> Unit
) {
    when (refreshState) {
        is LoadState.Loading -> {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        is LoadState.Error -> {
            // 첫 페이지 로드 실패
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "이미지를 불러오는데 실패했습니다.\n${refreshState.error.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        is LoadState.NotLoading -> {
            // 성공: 이미지 아이템들을 표시
            items(
                count = imageItems.itemCount,
                key = imageItems.itemKey { it.hashCode() } // API 중복 ID 회피
            ) { index ->
                val image = imageItems[index]
                if (image != null) {
                    ImageGridItem(
                        imageItem = image,
                        onClick = { onClickItem(image.id, false) },
                        onToggleFavorite = { isFavorited ->
                            onToggleFavorite(image, isFavorited)
                        }
                    )
                }
            }

            // "결과 없음" 표시
            if (imageItems.itemCount == 0) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
//                        Text(text = stringResource(resource = Res.string.no_results_found))
                    }
                }
            }
        }
    }
}

/**
 * 다음 페이지 로드(append) 상태를 처리하는 Composable
 */
private fun LazyGridScope.handlePagingAppendState(
    appendState: LoadState,
    onRetry: () -> Unit
) {
    when (appendState) {
        is LoadState.Loading -> {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        is LoadState.Error -> {
            item(span = { GridItemSpan(2) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "추가 로딩 실패: ${appendState.error.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(onClick = onRetry) {
                        Text("재시도")
                    }
                }
            }
        }

        is LoadState.NotLoading -> {
            // 로드 완료
        }
    }
}