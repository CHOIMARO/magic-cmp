plugins {
    alias(libs.plugins.magic.kmp.feature)
}

// 로그인 시트(S14), 계정 관리(S15), 회원 탈퇴(S16) (F17)
kotlin {

    android {
        namespace = "com.company.feature.account"
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.company.feature.account.resources"
}
