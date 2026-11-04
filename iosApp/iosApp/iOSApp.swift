import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // 로그인 SDK를 준비하고, Kotlin이 쓸 수 있게 Koin에 넘긴다(F17).
        LoginBridge.setUp()
        AppModuleKt.doInitKoin(loginBridge: LoginBridge())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // 카카오계정·Google 로그인이 끝나면 이 URL로 앱이 다시 열린다.
                .onOpenURL { url in LoginBridge.handle(url: url) }
        }
    }
}
