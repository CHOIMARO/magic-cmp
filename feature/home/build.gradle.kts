plugins {
    alias(libs.plugins.magic.kmp.feature)
}

kotlin {

    android {
        namespace = "com.company.feature.home"

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                // Paging
                implementation(libs.androidx.paging.common)
                implementation(libs.androidx.paging.compose)

                // Coil
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
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
