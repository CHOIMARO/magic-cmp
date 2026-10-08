package com.company.feature.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.compose.viewmodel.koinViewModel

/**
 * Navigation에서 Screen으로 바로 이동하지 않는 이유는 Route 부분에서 ViewModel을 받아 로직을 담당하기 위해 이렇게 구현합니다.
 */
@Composable
internal fun HomeRoute(
    viewModel: HomeViewModel = koinViewModel(),
) {
    // Screen에서는 viewModel의 State만 전달하여 UI를 렌더링하도록 구성
    val homeState by viewModel.uiState.collectAsStateWithLifecycle()
    val imagePagingItems = homeState.imageItems.collectAsLazyPagingItems()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeSideEffect.ShowToast -> {
                    // [수정] Toast 대신 Snackbar 표시 (suspend 함수이므로 코루틴 안에서 실행)
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    HomeScreen(
        homeState = homeState,
        imagePagingItems = imagePagingItems,
        snackbarHostState = snackbarHostState, // 전달
        onQueryChange = { query ->
            viewModel.postIntent(HomeIntent.OnSearchQueryChanged(query))
        },
        onSearch = {
            viewModel.postIntent(HomeIntent.OnSearch)
        },
    )
}