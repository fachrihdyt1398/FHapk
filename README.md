# FHapk — Aplikasi Catatan

Aplikasi catatan Android (Kotlin) dengan penyimpanan permanen dan **sinkronisasi otomatis ke cloud (Firebase Firestore)**. Dibuiled otomatis oleh GitHub Actions.

## Fitur

- Menyimpan catatan **permanen** di database lokal (Room) — tetap ada walau offline
- **Auto-save**: catatan tersimpan sendiri ~0,5 detik setelah berhenti mengetik
- **Sinkron otomatis**: catatan tersinkron ke Firebase Firestore (real-time) — hapus/pasang ulang aplikasi, catatan tetap bisa dipulihkan
- Konflik sinkron diselesaikan memakai waktu edit terakhir (yang terbaru menang)

## Struktur Build CI

Setiap push ke branch `main`:
- Workflow **Android CI** menjalankan `assembleDebug` dan meng-upload APK debug sebagai artifact.

Setiap push tag `v*` (mis. `v1.0.0`):
- Job **release** menjalankan `assembleRelease` yang **ditandatangani** dengan keystore dari repository secrets, lalu membuat **GitHub Release** berisi APK.

## Setup Firebase (sekali saja, wajib untuk fitur sync)

1. Buka https://console.firebase.google.com → **Add project** (nama bebas; Google Analytics boleh dimatikan).
2. Tambahkan aplikasi **Android**: package name `com.fhapk.app` → download `google-services.json`.
3. Taruh file `google-services.json` di folder `app/` (untuk build lokal, opsional), lalu set secret untuk CI:
   ```powershell
   gh secret set GOOGLE_SERVICES_JSON < app\google-services.json
   ```
   Catatan: CI tetap bisa build tanpa ini, hanya fitur sync yang nonaktif (aplikasi jalan offline).
4. Aktifkan **Authentication → Sign-in method → Anonymous → Enable**.
5. Buat **Firestore Database** (production mode), lalu di tab **Rules** gunakan:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{uid}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == uid;
       }
     }
   }
   ```

## Setup Signing Release (untuk rilis APK signed)

1. Buat keystore:
   ```
   keytool -genkey -v -keystore app-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias fhapk
   ```
2. Set secrets:
   ```powershell
   gh secret set KEYSTORE_BASE64 --body "$([Convert]::ToBase64String([IO.File]::ReadAllBytes('app-release.jks')))"
   gh secret set KEYSTORE_PASSWORD
   gh secret set KEY_ALIAS --body "fhapk"
   gh secret set KEY_PASSWORD
   ```
   Simpan `app-release.jks` di tempat aman dan JANGAN commit ke repo.

## Rilis

1. Push tag versi: `git tag v1.0.0 && git push origin v1.0.0`
2. GitHub Actions membuat APK signed + Release otomatis.
3. Download APK debug terbaru: buka tab **Actions** → run terbaru → Artifact `app-debug`.
