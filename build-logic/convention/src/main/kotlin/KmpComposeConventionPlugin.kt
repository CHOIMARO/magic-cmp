import com.company.buildlogic.configureAndroidTarget
import com.company.buildlogic.library
import com.company.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Compose UI를 가진 KMP 라이브러리 모듈의 공통 설정.
 *
 * magic.kmp.library 설정 위에 Compose 플러그인, Compose 리소스, @Preview 도구를 더한다.
 */
class KmpComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("magic.kmp.library")
            apply("org.jetbrains.compose")
            apply("org.jetbrains.kotlin.plugin.compose")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            configureAndroidTarget {
                // strings.xml, 폰트 등 Compose Multiplatform 리소스를 Android에서 사용하기 위해 필요
                androidResources.enable = true
            }

            sourceSets.commonMain.dependencies {
                implementation(libs.library("jetbrains-compose-runtime"))
                implementation(libs.library("jetbrains-compose-foundation"))
                implementation(libs.library("jetbrains-compose-material3"))
                implementation(libs.library("jetbrains-compose-ui"))
                implementation(libs.library("jetbrains-compose-components-resources"))
                implementation(libs.library("jetbrains-compose-ui-tooling-preview")) // @Preview
            }
        }

        // Android Studio에서 commonMain의 @Preview를 그리기 위한 도구. KMP 라이브러리 플러그인은 androidRuntimeClasspath에 둔다.
        dependencies.add("androidRuntimeClasspath", libs.library("jetbrains-compose-ui-tooling"))
    }
}
