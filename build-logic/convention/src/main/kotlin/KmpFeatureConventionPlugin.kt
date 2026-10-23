import com.company.buildlogic.library
import com.company.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * 화면(feature) 모듈의 공통 설정.
 *
 * magic.kmp.compose 설정 위에 Navigation 경로 직렬화, Koin, ViewModel, 공통 core 모듈을 더한다.
 * core:data는 넣지 않는다. feature는 domain의 UseCase만 쓰고, data는 composeApp의 DI에서 연결한다.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("magic.kmp.compose")
            apply("org.jetbrains.kotlin.plugin.serialization")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                implementation(project(":core:base"))
                implementation(project(":core:domain"))
                implementation(project(":core:ui"))
                implementation(project(":core:designsystem"))

                // Koin
                implementation(libs.library("koin-core"))
                implementation(libs.library("koin-compose"))
                implementation(libs.library("koin-compose-viewmodel"))

                // Navigation
                implementation(libs.library("kotlinx-serialization-json"))
                implementation(libs.library("androidx-navigation-compose"))

                // Lifecycle
                implementation(libs.library("androidx-lifecycle-viewmodelCompose"))
                implementation(libs.library("androidx-lifecycle-runtimeCompose"))

                // Icons
                implementation(libs.library("jetbrains-compose-material-icons-extended"))
            }
        }
    }
}
