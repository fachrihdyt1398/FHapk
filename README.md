# FHapk

Aplikasi Android native (Kotlin + Android Gradle Plugin) yang dibuild otomatis oleh GitHub Actions.

## Struktur Build CI

Setiap push ke branch `main`:
- Workflow **Android CI** menjalankan `assembleDebug` dan meng-upload APK debug sebagai artifact (bisa diunduh dari tab Actions > run terakhir > "Build Debug APK" > artifact `app-debug`).

Setiap push tag `v*` (mis. `v1.0.0`):
- Job **release** menjalankan `assembleRelease` yang **ditandatangani** dengan keystore dari repository secrets, lalu membuat **GitHub Release** berisi APK.

## Setup Signing (sekali saja)

1. Buat keystore dengan `keytool` (dari JDK mana pun):
   ```
   keytool -genkey -v -keystore app-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias fhapk
   ```
2. Set secrets di GitHub (Settings > Secrets and variables > Actions, atau lewat CLI):
   ```powershell
   gh secret set KEYSTORE_BASE64 --body "$([Convert]::ToBase64String([IO.File]::ReadAllBytes('app-release.jks')))"
   gh secret set KEYSTORE_PASSWORD
   gh secret set KEY_ALIAS --body "fhapk"
   gh secret set KEY_PASSWORD
   ```
   Simpan file `app-release.jks` di tempat aman dan JANGAN commit ke repo.

## Cara Rilis

1. Push tag versi baru: `git tag v1.0.0 && git push origin v1.0.0`
2. GitHub Actions akan build APK signed dan membuat Release otomatis.
3. Build lokal: `.\gradlew.bat assembleDebug` (APK di `app/build/outputs/apk/debug/`)
