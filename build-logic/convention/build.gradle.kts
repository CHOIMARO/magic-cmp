plugins {
    `kotlin-dsl`
}

dependencies {
    // 플러그인은 앱 빌드의 classpath에 이미 있으므로 compileOnly로 둔다.
    compileOnly(libs.android.gradleApiPlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "magic.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "magic.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "magic.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
    }
}
