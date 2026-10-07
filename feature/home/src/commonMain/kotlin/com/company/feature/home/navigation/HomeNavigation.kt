package com.company.feature.home.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.company.feature.home.HomeRoute
import com.company.feature.home.HomeViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

// 1. 그래프(Scope)의 기준이 되는 부모 Route
@Serializable
object HomeGraph

// 2. 홈 메인 화면
@Serializable
object HomeRoute

fun NavGraphBuilder.homeGraph(
    navController: NavController,
) {
    // startDestination을 HomeRoute로 지정하여 HomeGraph 생성
    navigation<HomeGraph>(startDestination = HomeRoute) {

        // --- 1. 홈 메인 화면 ---
        composable<HomeRoute> { backStackEntry ->
            // 부모 그래프(HomeGraph)의 BackStackEntry를 가져옴
            // 이 Entry가 ViewModelStoreOwner 역할을 함
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<HomeGraph>()
            }

            // parentEntry를 오너로 지정하여 ViewModel 주입 -> 그래프 범위 내 공유 가능
            // (Hilt의 hiltViewModel(parentEntry)와 동일한 원리)
            val homeViewModel = koinViewModel<HomeViewModel>(viewModelStoreOwner = parentEntry)

            HomeRoute(
                homeViewModel = homeViewModel
            )
        }
    }
}