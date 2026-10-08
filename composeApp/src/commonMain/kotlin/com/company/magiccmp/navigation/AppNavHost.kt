package com.company.magiccmp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.company.feature.capture.navigation.cameraScreen
import com.company.feature.capture.navigation.galleryScreen
import com.company.feature.capture.navigation.navigateToCamera
import com.company.feature.capture.navigation.navigateToGallery
import com.company.feature.editor.navigation.EditorRoute
import com.company.feature.editor.navigation.ExportRoute
import com.company.feature.editor.navigation.editorScreen
import com.company.feature.editor.navigation.exportScreen
import com.company.feature.editor.navigation.magicScreen
import com.company.feature.editor.navigation.navigateToMagic
import com.company.feature.main.navigation.MainRoute
import com.company.feature.main.navigation.mainScreen
import com.company.ui.editor.GalleryMode

/**
 * 앱 전체 내비게이션.
 *
 * 기존 Pixabay 검색(feature:home)은 모듈과 DI를 유지하고 그래프에서만 뺐다.
 * 다시 연결하려면 homeGraph(navController)를 아래에 추가한다.
 */
@Composable
fun AppNavHost(
    modifier: Modifier,
    navController: NavHostController
) {
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = MainRoute,
        ) {
            mainScreen(
                onOpenGallery = { mode, template -> navController.navigateToGallery(mode, template) },
                onOpenCamera = { navController.navigateToCamera() },
                onOpenEditor = { navController.navigate(EditorRoute) },
            )
            galleryScreen(
                onClose = { navController.popBackStack() },
                onOpenMagic = { template -> navController.navigateToMagic(template) },
                onOpenEditor = { navController.navigateToEditorFromMain() },
                onReturnToEditor = { navController.popBackStack() },
            )
            cameraScreen(
                onClose = { navController.popBackStack() },
                onOpenMagic = { navController.navigateToMagic(template = null) },
                onOpenEditor = { navController.navigateToEditorFromMain() },
            )
            magicScreen(
                onOpenEditor = { navController.navigateToEditorFromMain() },
            )
            editorScreen(
                onBack = { navController.popBackStack() },
                onAddClips = { navController.navigateToGallery(GalleryMode.APPEND) },
                onExport = { navController.navigate(ExportRoute) },
            )
            exportScreen(
                onBack = { navController.popBackStack() },
                onGoHome = {
                    navController.navigate(MainRoute) {
                        popUpTo<MainRoute> { inclusive = true }
                    }
                },
            )
        }
}

/** 새 프로젝트를 만든 뒤 편집 화면을 연다. 뒤로 가면 갤러리·매직이 아니라 탭 화면으로 돌아간다. */
private fun NavController.navigateToEditorFromMain() {
    navigate(EditorRoute) {
        popUpTo<MainRoute>()
    }
}
