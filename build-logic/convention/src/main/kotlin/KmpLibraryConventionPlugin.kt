import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.company.buildlogic.library
import com.company.buildlogic.libs
import com.company.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * KMP 라이브러리 모듈(Android + iOS)의 공통 설정.
 *
 * 모듈 파일에는 namespace와 그 모듈만 쓰는 의존성·설정을 둔다.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
            apply("com.android.kotlin.multiplatform.library")
            apply("com.android.lint")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            // kotlin { android { } } 블록. AGP가 "android" 이름으로 등록한다.
            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                compileSdk = libs.version("android-compileSdk").toInt()
                minSdk = libs.version("android-minSdk").toInt()

                withHostTestBuilder {
                }
            }

            // ":core:domain" -> "core:domainKit"
            val xcfName = "${path.removePrefix(":")}Kit"

            listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
                iosTarget.binaries.framework {
                    baseName = xcfName
                }
            }

            sourceSets.commonMain.dependencies {
                implementation(libs.library("kotlin-stdlib"))
            }

            sourceSets.commonTest.dependencies {
                implementation(libs.library("kotlin-test"))
            }
        }
    }
}
