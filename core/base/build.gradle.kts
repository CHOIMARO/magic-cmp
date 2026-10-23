plugins {
    alias(libs.plugins.magic.kmp.library)
}

kotlin {

    android {
        namespace = "com.company.core.base"

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.androidx.lifecycle.viewmodelCompose)
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
