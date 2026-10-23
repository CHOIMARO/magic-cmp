package com.company.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// 플러그인 코드에서는 libs.xxx 접근자가 생성되지 않으므로 이름으로 찾는다.
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

internal fun VersionCatalog.library(alias: String) =
    findLibrary(alias).get()

// kotlin { android { } } 블록. AGP가 "android" 이름으로 등록한다.
internal fun KotlinMultiplatformExtension.configureAndroidTarget(
    action: KotlinMultiplatformAndroidLibraryTarget.() -> Unit,
) {
    (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
        action()
    }
}
