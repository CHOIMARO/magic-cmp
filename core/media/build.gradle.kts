plugins {
    alias(libs.plugins.magic.kmp.library)
}

// 미리보기 재생기(feature:editor)와 내보내기(data)가 함께 쓰는 영상 합성 구성.
// 두 곳이 같은 코드로 합성해야 미리보기와 저장 결과가 같다.
kotlin {

    android {
        namespace = "com.company.core.media"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(projects.domain)
            }
        }

        androidMain {
            dependencies {
                // CompositionPlayer(미리보기)와 Transformer(내보내기)가 쓰는 Composition 모델과 효과
                api(libs.androidx.media3.transformer)
                api(libs.androidx.media3.effect)
            }
        }
    }
}
