import AuthenticationServices
import ComposeApp
import CryptoKit
import GoogleSignIn
import KakaoSDKAuth
import KakaoSDKCommon
import KakaoSDKUser
import UIKit

/// Login SDKs of the app: Kakao, Google, and Apple (F17).
///
/// The Kotlin code calls them through `IosLoginBridge`. Each completion runs one time.
/// The keys come from `Configuration/Secrets.xcconfig` through Info.plist.
final class LoginBridge: NSObject, IosLoginBridge {
    /// Keeps the Apple delegate alive until Apple calls it.
    private var appleSignIn: AppleSignIn?

    // MARK: - Setup

    /// Starts the Kakao SDK. Call it one time when the app starts.
    static func setUp() {
        if let key = infoValue("KakaoNativeAppKey") {
            KakaoSDK.initSDK(appKey: key)
            kakaoReady = true
        }
    }

    /// Gives a URL that opened the app to the login SDKs.
    ///
    /// - Parameter url: URL that opened the app.
    @MainActor
    static func handle(url: URL) {
        if kakaoReady, AuthApi.isKakaoTalkLoginUrl(url) {
            _ = AuthController.handleOpenUrl(url: url)
        } else {
            _ = GIDSignIn.sharedInstance.handle(url)
        }
    }

    private static var kakaoReady = false

    // MARK: - Kakao

    func signInWithKakao(completion: @escaping (IosLoginResult) -> Void) {
        DispatchQueue.main.async {
            guard Self.kakaoReady else {
                completion(IosLoginResult.companion.failure(message: "KAKAO_NATIVE_APP_KEY is not in Secrets.xcconfig"))
                return
            }
            let finish: (OAuthToken?, Error?) -> Void = { token, error in
                if let token {
                    completion(IosLoginResult.companion.success(token: token.accessToken, nonce: nil, authorizationCode: nil, nickname: nil))
                } else if Self.isKakaoCancel(error) {
                    completion(IosLoginResult.companion.cancelled())
                } else {
                    completion(IosLoginResult.companion.failure(message: error?.localizedDescription ?? "Kakao gave no token"))
                }
            }
            UserApi.shared.loginWithKakaoTalk { token, error in
                // 카카오톡에서 취소하면 그대로 끝낸다. 카카오톡이 없거나 다른 오류면 카카오계정으로 로그인한다.
                if token == nil, !Self.isKakaoCancel(error) {
                    UserApi.shared.loginWithKakaoAccount(completion: finish)
                } else {
                    finish(token, error)
                }
            }
        }
    }

    private static func isKakaoCancel(_ error: Error?) -> Bool {
        guard let sdkError = error as? SdkError, sdkError.isClientFailed else { return false }
        return sdkError.getClientError().reason == .Cancelled
    }

    // MARK: - Google

    func signInWithGoogle(completion: @escaping (IosLoginResult) -> Void) {
        DispatchQueue.main.async {
            // 클라이언트 ID나 URL 스킴이 없으면 Google SDK가 앱을 멈추므로 먼저 확인한다.
            guard let clientID = Self.infoValue("GoogleIOSClientID"), Self.infoValue("GoogleReversedClientID") != nil else {
                completion(IosLoginResult.companion.failure(message: "GOOGLE_IOS_CLIENT_ID is not in Secrets.xcconfig"))
                return
            }
            guard let presenter = Self.topViewController() else {
                completion(IosLoginResult.companion.failure(message: "The app has no screen"))
                return
            }
            // 서버 클라이언트 ID를 주면 ID 토큰의 대상(aud)이 Android와 같은 웹 클라이언트가 된다.
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID, serverClientID: Self.infoValue("GoogleWebClientID"))
            GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                if let idToken = result?.user.idToken?.tokenString {
                    completion(IosLoginResult.companion.success(token: idToken, nonce: nil, authorizationCode: nil, nickname: nil))
                } else if let error = error as? GIDSignInError, error.code == .canceled {
                    completion(IosLoginResult.companion.cancelled())
                } else {
                    completion(IosLoginResult.companion.failure(message: error?.localizedDescription ?? "Google gave no ID token"))
                }
            }
        }
    }

    // MARK: - Apple

    func signInWithApple(completion: @escaping (IosLoginResult) -> Void) {
        DispatchQueue.main.async {
            let signIn = AppleSignIn { result in
                self.appleSignIn = nil
                completion(result)
            }
            self.appleSignIn = signIn
            signIn.start()
        }
    }

    // MARK: - Sign-out

    func signOut() {
        DispatchQueue.main.async {
            GIDSignIn.sharedInstance.signOut()
            if Self.kakaoReady { UserApi.shared.logout { _ in } }
        }
    }

    // MARK: - Withdrawal

    func disconnect() {
        DispatchQueue.main.async {
            // 서버는 구글 토큰을 취소하지 못하므로 앱이 연결을 끊는다(app-api-guide 2-7).
            // 앱을 다시 시작한 뒤에도 Google SDK가 Keychain의 토큰을 읽어 취소한다.
            GIDSignIn.sharedInstance.disconnect { error in
                // 취소에 실패해도(예: 네트워크 없음) 기기의 구글 로그인 상태는 지운다.
                if error != nil { GIDSignIn.sharedInstance.signOut() }
            }
            // 카카오 연결 끊기는 서버가 한다. 앱은 기기의 토큰만 지운다.
            if Self.kakaoReady { UserApi.shared.logout { _ in } }
        }
    }

    // MARK: - Helpers

    /// Value of Info.plist, or nil when it is empty.
    static func infoValue(_ key: String) -> String? {
        guard let value = Bundle.main.object(forInfoDictionaryKey: key) as? String else { return nil }
        let trimmed = value.trimmingCharacters(in: .whitespaces)
        return trimmed.isEmpty ? nil : trimmed
    }

    /// Screen on top, to show the screens of the SDKs.
    static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
        var top = window?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

/// One Sign in with Apple request. The request uses the SHA-256 hash of a random nonce.
/// The server gets the original nonce and compares the hash with the token.
private final class AppleSignIn: NSObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    private let nonce = AppleSignIn.randomNonce()
    private let completion: (IosLoginResult) -> Void

    init(completion: @escaping (IosLoginResult) -> Void) {
        self.completion = completion
    }

    func start() {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(nonce)
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let token = String(data: tokenData, encoding: .utf8) else {
            completion(IosLoginResult.companion.failure(message: "Apple gave no identity token"))
            return
        }
        let code = credential.authorizationCode.flatMap { String(data: $0, encoding: .utf8) }
        // Apple은 이름을 첫 로그인 때만 준다. 없으면 서버가 기본 이름을 쓴다.
        let name = credential.fullName
            .map { PersonNameComponentsFormatter().string(from: $0).trimmingCharacters(in: .whitespaces) }
            .flatMap { $0.isEmpty ? nil : $0 }
        completion(IosLoginResult.companion.success(token: token, nonce: nonce, authorizationCode: code, nickname: name))
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        if let error = error as? ASAuthorizationError, error.code == .canceled {
            completion(IosLoginResult.companion.cancelled())
        } else {
            completion(IosLoginResult.companion.failure(message: error.localizedDescription))
        }
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        LoginBridge.topViewController()?.view.window ?? ASPresentationAnchor()
    }

    private static func randomNonce(length: Int = 32) -> String {
        var bytes = [UInt8](repeating: 0, count: length)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        return bytes.map { String(format: "%02x", $0) }.joined()
    }

    /// SHA-256 hash in lowercase hexadecimal, as the server expects.
    private static func sha256(_ text: String) -> String {
        SHA256.hash(data: Data(text.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}
