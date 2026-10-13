package com.company.magiccmp.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import com.company.feature.editor.navigation.MyVoiceRoute
import com.company.feature.editor.navigation.editorScreen
import com.company.feature.editor.navigation.exportScreen
import com.company.feature.editor.navigation.myVoiceScreen
import com.company.feature.main.navigation.MainRoute
import com.company.feature.main.navigation.mainScreen
import com.company.ui.editor.GalleryMode

/**
 * 앱 전체 내비게이션.
 *
 * 기존 Pixabay 검색(feature:home)은 모듈과 DI를 유지하고 그래프에서만 뺐다.
 * 다시 연결하려면 homeScreen()을 아래에 추가한다.
 *
 * 화면은 전환 효과 없이 바로 바뀐다(프로토타입과 같음). 기본 전환은 두 화면을 겹쳐 페이드하므로
 * 이전 화면이 비쳐 보이고, Compose 위층에 그리는 시스템 피커는 페이드되지 않고 다음 화면 위에 남는다.
 *
 * 연타로 두 번 이동하지 않도록, 사용자 입력으로 이동하는 콜백은 각 화면 그래프에서 onlyWhenResumed로 감싼다.
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
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            mainScreen(
                onOpenGallery = { template -> navController.navigateToGallery(GalleryMode.CREATE, template) },
                onOpenCamera = { navController.navigateToCamera() },
                onOpenEditor = { navController.navigate(EditorRoute) },
                onOpenMyVoice = { navController.navigate(MyVoiceRoute) },
            )
            galleryScreen(
                onClose = { navController.popBackStack() },
                onOpenEditor = { navController.navigateToEditorFromMain() },
                onReturnToEditor = { navController.popBackStack() },
            )
            cameraScreen(
                onClose = { navController.popBackStack() },
                onOpenEditor = { navController.navigateToEditorFromMain() },
            )
            editorScreen(
                onBack = { navController.popBackStack() },
                onAddClips = { navController.navigateToGallery(GalleryMode.APPEND) },
                onExport = { navController.navigate(ExportRoute) },
                onOpenMyVoice = { navController.navigate(MyVoiceRoute) },
            )
            myVoiceScreen(onClose = { navController.popBackStack() })
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

/** 새 프로젝트를 만든 뒤 편집 화면을 연다. 뒤로 가면 갤러리·촬영이 아니라 탭 화면으로 돌아간다. */
private fun NavController.navigateToEditorFromMain() {
    navigate(EditorRoute) {
        popUpTo<MainRoute>()
    }
}
