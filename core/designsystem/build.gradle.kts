plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)

    // Compose
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {

    android {
        namespace = "com.company.core.designsystem"
        compileSdk = 37
        minSdk = 29

        // 폰트 등 Compose Multiplatform 리소스를 Android에서 사용하기 위해 필요
        androidResources {
            enable = true
        }

        withHostTestBuilder {
        }
    }

    val xcfName = "core:designsystemKit"

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

                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.material3)
                implementation(libs.jetbrains.compose.ui)
                implementation(libs.jetbrains.compose.components.resources)
                implementation(libs.jetbrains.compose.material.icons.extended)
                implementation(libs.jetbrains.compose.ui.tooling.preview) // @Preview
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose) // BackHandler
            }
        }
    }
}

// Android Studio에서 commonMain의 @Preview를 그리기 위한 도구. KMP 라이브러리 플러그인은 androidRuntimeClasspath에 둔다.
dependencies {
    androidRuntimeClasspath(libs.jetbrains.compose.ui.tooling)
}

compose.resources {
    packageOfResClass = "com.company.core.designsystem.resources"
}
