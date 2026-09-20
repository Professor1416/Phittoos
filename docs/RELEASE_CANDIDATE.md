# Phittoos — V1 Release Candidate Report

This document records the specifications, verification details, and artifacts for the Phittoos V1 release candidate.

---

## 1. Release Specifications & Meta

*   **Release Candidate ID**: `Phittoos-V1.0.0-RC1`
*   **Build Date**: September 19, 2026
*   **Git Commit SHA**: `N/A` (Determinable only on your local workstation git repository where the project was cloned; sandbox workspace build ran outside a local `.git` repository tree).
*   **Application ID**: `com.professor1416.phittoos`
*   **Version Code**: `1`
*   **Version Name**: `1.0.0`
*   **Minimum SDK**: `24`
*   **Target SDK**: `36`
*   **Compile SDK**: `36` (API 36, minor API level 1)

*Note: The package/application identity `com.professor1416.phittoos` is now locked for production.*

---

## 2. Release Artifacts & Checksums

| Artifact Type | Build Status | Artifact File Path | File Size | SHA-256 Checksum |
| :--- | :--- | :--- | :--- | :--- |
| **Release APK** | Successfully Compiled | `app/build/outputs/apk/release/app-release-unsigned.apk` | 11.6 MB (11,640,939 bytes) | `4516b1edcd90b5043316a82508e1908c5feeedb46b979f931b61338bcea20c9d` |
| **Release AAB** | Successfully Compiled | `app/build/outputs/bundle/release/app-release.aab` | 11.3 MB (11,281,367 bytes) | `7baf6833b89fa088ff78deabba4096e603c97b42789e66a2aec172631b32fa76` |

---

## 3. Dependency & Network Audit Results

*   **Unused SDKs Stripped**:
    *   Firebase AI (`firebase.ai`)
    *   Firebase App Check (`firebase.appcheck.recaptcha`, `firebase.appcheck.debug`)
    *   Firebase BOM & Google Services Plugin
    *   Retrofit & OkHttp (`retrofit`, `okhttp`, `logging.interceptor`, `converter.moshi`)
    *   Moshi Kotlin / Codegen (`moshi.kotlin`, `moshi.kotlin.codegen` — JSON backup relies purely on Android's built-in `org.json`)
*   **Network & Permission Status**:
    *   `android.permission.INTERNET`: **NOT PRESENT** in merged release manifest.
    *   `android.permission.ACCESS_NETWORK_STATE`: Present via AndroidX WorkManager runtime constraints (normal install-time permission, no network calls made).
    *   `android.permission.POST_NOTIFICATIONS`: Declared for local overdue reminders only.
    *   No third-party analytics, ads, or telemetry libraries are present in the release binary.

---

## 4. Production Keystore & Signing Instructions

### Generating a Secure Local Upload Keystore
Run the following command on your secure local workstation (do not commit keystores or passwords to version control):

```bash
keytool -genkeypair \
  -v \
  -keystore my-upload-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias upload
```

### Environment Variable Setup for Automated Local Builds
Set these environment variables on your local machine before executing `./gradlew assembleRelease` or `bundleRelease`:

```bash
export KEYSTORE_PATH="/path/to/your/my-upload-key.jks"
export STORE_PASSWORD="your_keystore_password"
export KEY_PASSWORD="your_key_password"
```

### Signing and Verifying an Unsigned APK
To manually sign and verify the unsigned release APK:

```bash
# 1. Align and Sign the APK
apksigner sign \
  --ks my-upload-key.jks \
  --ks-key-alias upload \
  --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk

# 2. Verify Signing Certificate and Integrity
apksigner verify --verbose --print-certs app-release-signed.apk
```

---

## 5. Verification & Upgrade Safety

*   **Compilation & Linter**: Completed with zero syntax errors or dependency conflicts.
*   **Unit & Local JVM Tests**: All 215 tests passed cleanly.
*   **Upgrade Consequence Note**: Do NOT claim an existing installation using `com.aistudio.phittoos.mhzrtp` can be updated in-place to `com.professor1416.phittoos`. Because the app has not been publicly released yet, treat the new `applicationId` as the permanent pre-release identity. Existing test-device installs using the old `applicationId` may need uninstall/reinstall. Existing JSON Phittoos backups remain logically compatible because backup format/data IDs do not depend on the Android `applicationId`.

---

## 6. Manual Owner Checklist (Before Play Store Submission)

The following steps must be completed manually by the app owner outside the automated sandbox:

- [ ] Create Play Console account
- [ ] Create/store upload keystore securely
- [ ] Build/sign release using production upload key
- [ ] Verify signed APK certificate
- [ ] Install signed APK on real phone
- [ ] Confirm launcher/splash
- [ ] Confirm notification permission + reminder
- [ ] Create real ledger data
- [ ] Backup JSON
- [ ] Clear/reinstall as appropriate
- [ ] Restore backup
- [ ] Verify balances/history
- [ ] Verify upgrade using same locked applicationId
- [ ] Perform TalkBack/large-font smoke test
- [ ] Activate GitHub Pages in repository settings to host the Privacy Policy and Support pages
- [ ] Enter final Play declarations and submit the app

---

## 7. Status Summary

*   **Task 18C Status**: **COMPLETE IN REPOSITORY / PUBLICATION VERIFICATION PENDING**
    *   **Support Email**: `phittoos.app@gmail.com`
    *   **Target Root Support URL**: `https://professor1416.github.io/Phittoos/`
    *   **Target Privacy Policy URL**: `https://professor1416.github.io/Phittoos/privacy/`
    *   *Note: Source values are complete and placeholders have been fully replaced. Actual public URL accessibility depends on GitHub Pages activation in GitHub Settings.*
*   **Task 18D Status**: **BUILD PREPARED / MANUAL SIGNING AND DEVICE STEPS REMAIN** (Unsigned release APK & AAB successfully built; production keystore signing, physical device install verification, and Play Console submission remain manual owner actions).
