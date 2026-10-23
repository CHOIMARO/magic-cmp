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
    }
}

compose.resources {
    packageOfResClass = "com.company.feature.editor.resources"
}
