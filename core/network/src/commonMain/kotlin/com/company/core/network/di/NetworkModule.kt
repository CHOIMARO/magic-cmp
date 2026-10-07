package com.company.core.network.di

import com.company.core.network.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module


val networkModule = module {
    // 1. JSON 설정
    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        }
    }

    // 2. HttpClient 생성 (Retrofit + OkHttp 역할을 동시에 수행)
    single {
        // 엔진을 명시하지 않으면 androidMain에선 OkHttp, iosMain에선 Darwin을 자동 선택함
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 25_000
                connectTimeoutMillis = 25_000
                socketTimeoutMillis = 20_000
            }

            // [Converter Factory 대체] Serialization 플러그인
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true // 불필요한 필드는 무시 (API 변경 대응에 필수)
                })
            }

            // [Logging Interceptor 대체]
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("HTTP Client: $message") // 또는 커스텀 로거 사용
                    }
                }
                level = LogLevel.BODY
            }

            // [Auth Interceptor & Base URL 대체]
            // defaultRequest를 쓰면 모든 요청에 기본 URL과 헤더가 적용됨
            defaultRequest {
                url(BuildConfig.BASE_URL)

                // 기존 Interceptor 로직: 헤더 추가
                // 주의: BuildConfig는 commonMain에서 바로 접근이 안될 수 있음 (BuildConfig 플러그인 필요)
//                header("Authorization", "KakaoAK ${"YOUR_API_KEY"}")

                url.parameters.append("key", BuildConfig.PIXABAY_API_KEY)
            }
        }
    }
}