# 배포 전 할 일

배포 전에 반드시 끝내야 하는 작업을 적는다. 작업을 끝내면 항목을 지우지 말고 체크한다.

## iOS

### [ ] Apple 로그인 서명을 되돌린다

**배경 (2026-11-10)**

- 지금 서명 팀 `Q4977ZVTKH`는 무료 Personal Team이다.
- Personal Team은 Sign in with Apple 기능(`com.apple.developer.applesignin`)을 쓸 수 없다.
- 그래서 Xcode가 기기용 프로비저닝 프로파일을 만들지 못했다.
- 기기 테스트를 하려고, 기기용 Debug 빌드에서만 `Magic.entitlements` 연결을 뺐다.

**지금 상태**

| 빌드 | entitlements | Apple 로그인 |
|---|---|---|
| Debug, 실제 기기 | 없음 | 동작하지 않는다 |
| Debug, 시뮬레이터 | 있음 | 이전과 같다 |
| Release | 있음 | 유료 팀으로 서명해야 빌드된다 |

바꾼 곳은 `iosApp/iosApp.xcodeproj/project.pbxproj`의 iosApp 타깃 Debug 구성이다.

```
"CODE_SIGN_ENTITLEMENTS[sdk=iphonesimulator*]" = Configuration/Magic.entitlements;
```

**할 일**

1. Apple Developer Program에 가입한다.
2. 팀 ID가 바뀌면 `project.pbxproj`의 `DEVELOPMENT_TEAM` 4곳을 새 팀 ID로 바꾼다.
3. iosApp 타깃 Debug 구성의 위 줄을 아래처럼 되돌린다.

   ```
   CODE_SIGN_ENTITLEMENTS = Configuration/Magic.entitlements;
   ```

4. Xcode의 Signing & Capabilities 탭에 서명 오류가 없는지 확인한다.
5. 실제 기기에서 Debug 빌드와 Release 빌드로 Apple 로그인을 확인한다.

**되돌려야 하는 이유**

- 카카오·Google 로그인을 제공하는 앱은 App Store 심사 지침 4.8에 따라 Apple 로그인도 제공해야 한다.
- 3번을 하지 않으면 기기용 Debug 빌드에서 Apple 로그인을 테스트할 수 없다.
