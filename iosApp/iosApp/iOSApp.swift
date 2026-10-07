import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
            // iosMain에서 만든 함수 호출
            AppModuleKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
