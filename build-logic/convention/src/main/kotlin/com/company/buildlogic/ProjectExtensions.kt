package com.company.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

// 플러그인 코드에서는 libs.xxx 접근자가 생성되지 않으므로 이름으로 찾는다.
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

internal fun VersionCatalog.library(alias: String) =
    findLibrary(alias).get()
