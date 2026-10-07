package com.company.feature.home

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.core.domain.model.ImageItem
import com.company.core.domain.usecase.GetImageDetailUseCase
import com.company.core.domain.usecase.SearchImagesUseCase
import com.company.ui.UiStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

// 1. State: 화면의 현재 상태
data class HomeState(
    val uiStatus: UiStatus = UiStatus.Idle,
    val searchQuery: String = "",
    val imageItems: Flow<PagingData<ImageItem>> = flowOf(PagingData.empty()),
) : State()

// 2. Intent: 사용자의 행동 (버튼 클릭, 화면 진입 등)
sealed class HomeIntent : Intent() {
    data class OnSearchQueryChanged(val query: String) : HomeIntent()
    data object OnSearch : HomeIntent()
}

// 3. SideEffect: 네비게이션, 토스트 등 단발성 이벤트
sealed class HomeSideEffect : SideEffect() {
    data class ShowToast(val message: String) : HomeSideEffect()
}

class HomeViewModel(
    private val searchImagesUseCase: SearchImagesUseCase,
    private val getImageDetailUseCase: GetImageDetailUseCase
) : BaseViewModel<HomeState, HomeIntent, HomeSideEffect>(
    initialState = HomeState()
) {

    // Intent 처리 로직 (MVI의 핵심)
    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OnSearchQueryChanged -> {
                reduce { copy(searchQuery = intent.query) }
            }
            is HomeIntent.OnSearch -> searchImages()
        }
    }

    private fun searchImages() {
        val query = currentState.searchQuery
        if (query.isBlank()) {
            postSideEffect { HomeSideEffect.ShowToast("검색어를 입력해주세요.") }
            return
        }

        // 로딩 상태로 변경
        reduce { copy(uiStatus = UiStatus.Loading) }

        viewModelScope.launch {
            runCatching {
                // Paging 데이터 스트림 생성 및 캐싱
                val pagingFlow = searchImagesUseCase(query)
                    .cachedIn(viewModelScope)

                reduce {
                    copy(
                        uiStatus = UiStatus.Content,
                        imageItems = pagingFlow
                    )
                }
            }.onFailure { e ->
                // 에러 처리
                reduce { copy(uiStatus = UiStatus.Error(e.message)) }
                postSideEffect { HomeSideEffect.ShowToast("검색 중 오류가 발생했습니다: ${e.message}") }
            }
        }
    }
}