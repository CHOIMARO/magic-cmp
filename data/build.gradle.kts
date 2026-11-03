import java.util.Properties

plugins {
    alias(libs.plugins.magic.kmp.library)

    // Serialization
    alias(libs.plugins.kotlinSerialization)
}

// 서버 주소와 로그인 키는 local.properties에서 읽는다. 키는 저장소에 올리지 않는다.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localValue(key: String, default: String = ""): String = localProperties.getProperty(key) ?: default

// 서버 주소는 이 한 곳에서 정한다(app-api-guide 4-3). 도메인과 HTTPS가 생기면 기본값만 바꾼다.
val generateApiConfig = tasks.register<Sync>("generateApiConfig") {
    val apiBaseUrl = localValue("MAGIC_API_BASE_URL", "http://54.117.33.15")
    val googleWebClientId = localValue("GOOGLE_WEB_CLIENT_ID")
    val kakaoNativeAppKey = localValue("KAKAO_NATIVE_APP_KEY")
    from(
        resources.text.fromString(
            """
            |package com.company.data.remote
            |
            |/** Settings of the server and the login providers. Gradle makes this file from local.properties. */
            |internal object ApiConfig {
            |    /** Address of the API server. */
            |    const val BASE_URL = "$apiBaseUrl"
            |
            |    /** Web client ID of Google. Android asks Google for an ID token for this client. */
            |    const val GOOGLE_WEB_CLIENT_ID = "$googleWebClientId"
            |
            |    /** Native app key of Kakao. */
            |    const val KAKAO_NATIVE_APP_KEY = "$kakaoNativeAppKey"
            |}
            |
            """.trimMargin()
        )
    ) {
        rename { "ApiConfig.kt" }
    }
    into(layout.buildDirectory.dir("generated/apiConfig"))
}

kotlin {

    android {
        namespace = "com.company.data"

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateApiConfig.map { it.destinationDir })
            dependencies {
                implementation(projects.domain)
                implementation(projects.core.media)
                implementation(libs.kotlinx.coroutines.core)

                // Koin 추가
                implementation(libs.koin.core)

                // Ktor
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.auth)

                // Paging
                implementation(libs.androidx.paging.common)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        androidMain {
            dependencies {
                // 로그인 SDK (F17)
                implementation(libs.androidx.credentials)
                implementation(libs.androidx.credentials.play.services.auth)
                implementation(libs.googleid)
                implementation(libs.kakao.sdk.user)
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
