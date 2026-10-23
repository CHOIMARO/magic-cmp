plugins {
    alias(libs.plugins.magic.kmp.compose)
}

kotlin {

    android {
        namespace = "com.company.core.designsystem"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.jetbrains.compose.material.icons.extended)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose) // BackHandler
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.company.core.designsystem.resources"
}
