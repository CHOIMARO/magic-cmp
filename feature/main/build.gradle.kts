plugins {
    alias(libs.plugins.magic.kmp.feature)
}

kotlin {

    android {
        namespace = "com.company.feature.main"
    }
}

compose.resources {
    packageOfResClass = "com.company.feature.main.resources"
}
