plugins {
    alias(libs.plugins.magic.kmp.feature)
}

kotlin {

    android {
        namespace = "com.company.feature.capture"
    }

    sourceSets {
        androidMain {
            dependencies {
                // 시스템 피커: 임베디드 피커(Android 14 + SDK Extensions 15)와 기본 피커 실행
                implementation(libs.androidx.photopicker.compose)
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.company.feature.capture.resources"
}
