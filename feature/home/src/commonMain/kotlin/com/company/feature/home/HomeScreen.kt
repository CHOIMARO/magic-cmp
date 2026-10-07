package com.company.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.paging.compose.LazyPagingItems
import com.company.core.domain.model.ImageItem
import com.company.feature.home.component.HomeSearchBarSection
import com.company.feature.home.component.HomeSearchResultSection

@Composable
fun HomeScreen(
    homeState: HomeState,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    imagePagingItems: LazyPagingItems<ImageItem>
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // 화면 빈 곳 클릭 시 키보드 내림
                focusManager.clearFocus()
            },
        topBar = {
            HomeSearchBarSection(
                searchQuery = homeState.searchQuery,
                clearButtonVisible = homeState.searchQuery.isNotBlank(),
                onValueChange = onQueryChange,
                onSearch = {
                    focusManager.clearFocus()
                    onSearch()
                }
            )
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            HomeSearchResultSection(
                uiStatus = homeState.uiStatus,
                imageItems = imagePagingItems,
                onClickItem = { id, isVideo ->
                    // TODO: 상세 화면 이동 로직 구현
                    println("Clicked item: $id, isVideo: $isVideo")
                },
                onToggleFavorite = { item, isFavorited ->
                    // TODO: 좋아요 토글 로직 구현 (ViewModel Intent 호출 등)
                    println("Toggle favorite: ${item.id}, state: $isFavorited")
                }
            )
        }
    }
}