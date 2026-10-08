package com.company.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.company.feature.home.HomeRoute
import kotlinx.serialization.Serializable

// 홈 메인 화면
@Serializable
object HomeRoute

/**
 * Adds the home screen (Pixabay image search) to the graph.
 *
 * The ViewModel has the scope of this screen. Use a graph scope only when several screens share one UI state.
 */
fun NavGraphBuilder.homeScreen() {
    composable<HomeRoute> {
        HomeRoute()
    }
}
