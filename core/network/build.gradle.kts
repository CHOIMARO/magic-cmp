import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.magic.kmp.library)
}

// 1. local.properties 읽어오기
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(localFile.inputStream())
    }
}

// 2. 키 값 가져오기 (없으면 빈 문자열)
val pixabayKey = localProperties.getProperty("PIXABAY_API_KEY") ?: ""
val baseUrl = localProperties.getProperty("BASE_URL") ?: "https://default.url.com/"

// 3. BuildConfig 파일 생성 태스크 정의
val generateBuildConfig = tasks.register<Sync>("generateBuildConfig") {
    val packageName = "com.company.core.network"

    from(
        resources.text.fromString(
            """
            |package $packageName
            |
            |object BuildConfig {
            |    const val PIXABAY_API_KEY = "$pixabayKey"
            |    const val BASE_URL = "$baseUrl"
            |}
            """.trimMargin()
        )
    ) {
        rename { "BuildConfig.kt" } // 파일명
    }

    into(layout.buildDirectory.dir("generated/kotlin")) // 저장 위치
}

// 4. 코틀린 소스셋에 생성된 폴더 등록
kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(generateBuildConfig.map { it.destinationDir })
        }
    }
}

kotlin {
    android {
        namespace = "com.company.core.network"

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                // Koin 추가
                implementation(libs.koin.core)

                // Ktor
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.auth)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp) // 안드로이드는 익숙한 OkHttp 엔진 사용
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.testExt.junit)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin) // iOS는 네이티브 Darwin 엔진 사용
            }
        }
    }
}
