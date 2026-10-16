plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)

    // Serialization
    alias(libs.plugins.kotlinSerialization)

    // Compose
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {

    android {
        namespace = "com.company.feature.editor"
        compileSdk = 37
        minSdk = 29

        // strings.xml 등 Compose Multiplatform 리소스를 Android에서 사용하기 위해 필요
        androidResources {
            enable = true
        }

        withHostTestBuilder {
        }
    }

    val xcfName = "feature:editorKit"

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(projects.core.base)
                implementation(projects.core.domain)
                implementation(projects.core.ui)
                implementation(projects.core.designsystem)
                implementation(projects.core.media)

                // Koin
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                // Navigation
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.androidx.navigation.compose)

                // Lifecycle
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                // Compose
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.material3)
                implementation(libs.jetbrains.compose.ui)
                implementation(libs.jetbrains.compose.material.icons.extended)
                implementation(libs.jetbrains.compose.ui.tooling.preview) // @Preview
                implementation(libs.jetbrains.compose.components.resources)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

// Android Studio에서 commonMain의 @Preview를 그리기 위한 도구. KMP 라이브러리 플러그인은 androidRuntimeClasspath에 둔다.
dependencies {
    androidRuntimeClasspath(libs.jetbrains.compose.ui.tooling)
}

compose.resources {
    packageOfResClass = "com.company.feature.editor.resources"
}
