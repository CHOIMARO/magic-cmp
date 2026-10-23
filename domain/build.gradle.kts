plugins {
    alias(libs.plugins.magic.kmp.library)

    // Serialization
    alias(libs.plugins.kotlinSerialization)
}

kotlin {

    android {
        namespace = "com.company.domain"

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

                // Paging
                implementation(libs.androidx.paging.common)

                // Coroutines (StateFlow)
                implementation(libs.kotlinx.coroutines.core)
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
