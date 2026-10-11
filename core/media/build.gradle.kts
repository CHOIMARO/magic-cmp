plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
}

// 미리보기 재생기(feature:editor)와 내보내기(core:data)가 함께 쓰는 영상 합성 구성.
// 두 곳이 같은 코드로 합성해야 미리보기와 저장 결과가 같다.
kotlin {

    android {
        namespace = "com.company.core.media"
        compileSdk = 37
        minSdk = 29

        withHostTestBuilder {
        }
    }

    val xcfName = "core:mediaKit"

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
                implementation(libs.kotlinx.coroutines.core)
                implementation(projects.core.domain)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // CompositionPlayer(미리보기)와 Transformer(내보내기)가 쓰는 Composition 모델과 효과
                api(libs.androidx.media3.transformer)
                api(libs.androidx.media3.effect)
            }
        }
    }
}
