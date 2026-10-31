# Commerce Hub 11 (Kotlin / Jetpack Compose)

1. Is zip ko repo ke root me extract karo (`images/` folder wahi rehne do, usme icon-512.png zaruri hai).
2. Push karo -> GitHub Actions "Build Android APK" chalega.
3. APK: Actions -> run -> Artifacts -> CommerceHub11-debug-apk.

Launcher icon workflow me `images/icon-512.png` se automatically lagta hai.
Calls: io.getstream:stream-webrtc-android (WebRTC). Browser: WebView (Brave / Google search switch).
