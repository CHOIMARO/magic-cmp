plugins {
    alias(libs.plugins.magic.kmp.library)

    // Serialization
    alias(libs.plugins.kotlinSerialization)
}

kotlin {

    android {
        namespace = "com.company.core.data"

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain {
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

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.testExt.junit)
            }
        }
    }

}
