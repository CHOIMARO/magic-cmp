plugins {
    alias(libs.plugins.magic.kmp.feature)
}

kotlin {

    android {
        namespace = "com.company.feature.editor"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.media)
            }
        }

        androidMain {
            dependencies {
                // 더빙 미리 듣기: 서버가 준 주소를 바로 재생한다.
                implementation(libs.androidx.media3.exoplayer)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.company.feature.editor.resources"
}
