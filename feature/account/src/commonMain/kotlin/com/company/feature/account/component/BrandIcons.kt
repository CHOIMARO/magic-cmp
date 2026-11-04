package com.company.feature.account.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.company.domain.model.account.LoginProvider

// =====================================================================
// Logos of the login providers. The paths come from the v12 design.
// =====================================================================

/** Background of the Kakao button and of the Kakao logo circle. */
internal val KakaoYellow = Color(0xFFFEE500)

private fun brandIcon(name: String, viewport: Float, vararg paths: Pair<Color, String>): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = viewport.dp,
        defaultHeight = viewport.dp,
        viewportWidth = viewport,
        viewportHeight = viewport,
    ).apply {
        paths.forEach { (color, pathData) -> addPath(pathData = addPathNodes(pathData), fill = SolidColor(color)) }
    }.build()

/** Speech bubble of Kakao. */
internal val KakaoLogo: ImageVector by lazy {
    brandIcon(
        "Kakao",
        24f,
        Color(0xFF191600) to "M12 3.6C6.8 3.6 2.6 6.9 2.6 11c0 2.6 1.8 4.9 4.4 6.3l-1.1 4c-.1.3.3.6.6.4l4.7-3.1c.3 0 .5.1.8.1 " +
            "5.2 0 9.4-3.3 9.4-7.4S17.2 3.6 12 3.6z",
    )
}

/** "G" of Google in four colors. */
internal val GoogleLogo: ImageVector by lazy {
    brandIcon(
        "Google",
        48f,
        Color(0xFFEA4335) to "M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 " +
            "13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z",
        Color(0xFF4285F4) to "M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 " +
            "6c4.51-4.18 7.09-10.36 7.09-17.65z",
        Color(0xFFFBBC05) to "M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 " +
            "24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z",
        Color(0xFF34A853) to "M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 " +
            "0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z",
    )
}

/** Apple logo. */
internal val AppleLogo: ImageVector by lazy {
    brandIcon(
        "Apple",
        24f,
        Color.Black to "M16.37 1.43c0 1.14-.49 2.27-1.18 3.08-.74.9-1.99 1.57-2.99 1.57-.12 0-.23-.02-.3-.03-.01-.06-.04-.22" +
            "-.04-.39 0-1.15.57-2.27 1.21-2.98.8-.94 2.14-1.64 3.25-1.68.03.13.05.28.05.43zm4.56 15.71c-.03.07-.46 " +
            "1.58-1.52 3.12-.94 1.34-1.94 2.71-3.43 2.71-1.52 0-1.9-.88-3.63-.88-1.7 0-2.3.91-3.67.91-1.38 " +
            "0-2.33-1.26-3.43-2.8-1.29-1.82-2.32-4.63-2.32-7.28 0-4.28 2.8-6.55 5.55-6.55 1.45 0 2.68.95 3.6.95.87 " +
            "0 2.22-1.01 3.9-1.01.61 0 2.89.06 4.37 2.19-.13.09-2.38 1.37-2.38 4.19 0 3.26 2.85 4.42 2.96 4.45z",
    )
}

/** Logo of the provider. */
internal val LoginProvider.logo: ImageVector
    get() = when (this) {
        LoginProvider.KAKAO -> KakaoLogo
        LoginProvider.GOOGLE -> GoogleLogo
        LoginProvider.APPLE -> AppleLogo
    }
