# 초대 링크 (App Links) 설정

Firebase Dynamic Links 는 종료돼서 안 씀. 그냥 https 링크를 쓰고, 안드로이드 **App Links** 로 앱이 바로 열리게 한다.

| 링크 | 앱에서 여는 화면 |
|---|---|
| `https://goner-ssu.web.app/i/{friendCode}` | `friend/InviteLandingActivity` (친구 초대) |
| `https://goner-ssu.web.app/p/{appointmentId}` | `InviteLandingActivity` → `promise/InviteDetailActivity` (약속 초대) |

앱 쪽은 끝나 있음 (`AndroidManifest.xml` 의 `InviteLandingActivity` intent-filter, `autoVerify="true"`).
남은 건 **Firebase Hosting 에 `assetlinks.json` 올리기** (배포는 직접).

## 1. SHA-256 지문 구하기

앱을 서명한 키마다 하나씩 필요하다. 여러 개면 배열에 다 넣으면 됨.

### debug (내 컴퓨터에서 `installDebug` 한 앱)
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA256
```
지금 이 맥의 debug 키 (2026-11-10 확인):
```
D1:E2:E5:44:F8:89:01:FD:13:66:AC:7A:59:39:5B:82:F2:C6:B2:7D:0C:A3:44:58:05:E2:95:24:40:1F:78:E6
```
팀원마다 debug 키가 다르니까, 각자 위 명령으로 뽑아서 추가해야 그 사람 폰에서도 바로 열린다.

### release
- 업로드 키로 직접 서명: `keytool -list -v -keystore <release.jks> -alias <alias>`
- Play 스토어에 올리면(Play App Signing): Play Console → 앱 → 테스트 및 출시 → 설정 → **앱 서명** 에 있는 "앱 서명 키 인증서 SHA-256" 을 넣어야 함 (업로드 키 지문 말고)

## 2. assetlinks.json

Hosting 폴더(보통 `public/`) 아래 `public/.well-known/assetlinks.json` 으로 만든다.

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.goner.app",
      "sha256_cert_fingerprints": [
        "D1:E2:E5:44:F8:89:01:FD:13:66:AC:7A:59:39:5B:82:F2:C6:B2:7D:0C:A3:44:58:05:E2:95:24:40:1F:78:E6"
      ]
    }
  }
]
```

## 3. firebase.json

- `.well-known` 은 그대로 파일로 나가야 하니까 rewrite 에 걸리지 않게 한다.
- 앱이 없는 사람이 `/i/...`, `/p/...` 를 열면 웹 페이지(설치 안내 등)가 보이게 rewrite.

```json
{
  "hosting": {
    "public": "public",
    "headers": [
      {
        "source": "/.well-known/assetlinks.json",
        "headers": [{ "key": "Content-Type", "value": "application/json" }]
      }
    ],
    "rewrites": [
      { "source": "/i/**", "destination": "/index.html" },
      { "source": "/p/**", "destination": "/index.html" }
    ]
  }
}
```

배포: `firebase deploy --only hosting`
확인: 브라우저로 `https://goner-ssu.web.app/.well-known/assetlinks.json` 이 위 JSON 그대로 나오면 됨.
구글 확인 도구: `https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://goner-ssu.web.app&relation=delegate_permission/common.handle_all_urls`

## 4. 폰에서 확인

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
# 검증 다시 시키기 (앱 설치 후)
$ADB shell pm verify-app-links --re-verify com.goner.app
# 상태 보기 (goner-ssu.web.app: verified 면 성공)
$ADB shell pm get-app-links com.goner.app
# 링크 열어 보기
$ADB shell am start -a android.intent.action.VIEW -d "https://goner-ssu.web.app/i/han1234"
```

`assetlinks.json` 올리기 전에 테스트만 하고 싶으면 (그 폰에서만 임시로 허용):
```bash
$ADB shell pm set-app-links-user-selection --user 0 --package com.goner.app true goner-ssu.web.app
```

## 5. 앱 동작 요약

- 친구 링크
  - 로그인 O: 코드로 사람 찾고 → 친구 되기(요청 없이 바로 양쪽 friends 생성) / 이미 친구 / 내 코드면 홈 / 없는 코드면 홈 + 토스트
  - 로그인 X: 코드를 SharedPreferences 에 저장 → 로그인 화면으로. 로그인·온보딩 끝나고 `MainActivity` 가 뜰 때 저장된 코드가 있으면 초대 화면을 다시 엶 (`InviteLink.takePendingFriendCode`)
- 약속 링크: 로그인 O 면 초대 상세. 내가 `participantUids` 에 없으면 "초대받은 사람만 볼 수 있어요". 로그인 X 면 로그인 화면
- 약속 생성 완료 화면의 "초대 링크 공유" = 약속 링크 공유
