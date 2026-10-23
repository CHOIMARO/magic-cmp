plugins {
    // Compose (편집기 공유 UI)
    alias(libs.plugins.magic.kmp.compose)
}

kotlin {

    android {
        namespace = "com.company.ui"

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.domain)
                implementation(projects.core.designsystem)

                // 화면 이동 연타 방지(onlyWhenResumed)
                implementation(libs.androidx.lifecycle.runtimeCompose)
            }
        }

        androidMain {
            dependencies {
                // 썸네일과 타임라인 프레임을 하드웨어 디코더로 읽는다.
                implementation(libs.androidx.media3.inspector.frame)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.testExt.junit)
            }
        }
    }

}

compose.resources {
    packageOfResClass = "com.company.ui.resources"
}
