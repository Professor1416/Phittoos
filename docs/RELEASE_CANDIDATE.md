# Phittoos — V1 Release Candidate Report

This document records the specifications, verification details, and artifacts for the Phittoos V1 release candidate.

---

## 1. Release Specifications & Meta

*   **Release Candidate ID**: `Phittoos-V1.0.0-RC1`
*   **Build Date**: September 15, 2026
*   **Git Commit SHA**: `N/A` (Determinable only on your local workstation git repository where the project was cloned; sandbox workspace build ran outside a local `.git` repository tree).
*   **Application ID**: `com.aistudio.phittoos.mhzrtp`
*   **Version Code**: `1`
*   **Version Name**: `1.0.0`
*   **Minimum SDK**: `24`
*   **Target SDK**: `36`
*   **Compile SDK**: `36` (API 36, minor API level 1)

---

## 2. Release Artifacts & Checksums

| Artifact Type | Build Status | Artifact File Path | File Size | SHA-256 Checksum |
| :--- | :--- | :--- | :--- | :--- |
| **Release APK** | Successfully Compiled | `app/build/outputs/apk/release/app-release-unsigned.apk` | 16 MB | `b904f83e777eca458512285e9cca118ca0f1cd41e2d62775a9819b44c3b47243` |
| **Release AAB** | Successfully Compiled | `app/build/outputs/bundle/release/app-release.aab` | 15 MB | `6ab7b3864ff9a2c9d6ade5aaa7d3384df99c7d2cdb93ef9b965105ff187a99fe` |

---

## 3. Signing Status

*   **Build Output Status**: 
    *   **Release APK Build**: **SUCCEEDED**
    *   **Release AAB Build**: **SUCCEEDED**
*   **Signing Configuration**: **Production signing has NOT been completed.** Build outputs `app-release-unsigned.apk` and `app-release.aab` are prepared unsigned to protect credentials and prevent hardcoding secrets.
*   **Signed APK/Device Verification**: Remains a **manual owner step** to sign using your private upload keystore.
*   **Manual Signing Recommendation**: Use `apksigner` and `jarsigner` with your secure private `.jks` upload key:
    ```bash
    apksigner sign --ks my-upload-key.jks --out app-release-signed.apk app-release-unsigned.apk
    ```

---

## 4. Build Commands Executed

The following exact commands were executed to produce the clean production release builds:
1.  **Release APK**:
    ```bash
    gradle :app:assembleRelease
    ```
2.  **Release AAB Bundle**:
    ```bash
    gradle :app:bundleRelease
    ```

---

## 5. Verification & Testing

*   **Compilation & Linter**: Completed with zero syntax errors, build failures, or dependency conflicts.
*   **Unit & Local Roborazzi Tests**: Passed successfully.
*   **Upgrade Safety**: Database migration/source compatibility checks passed. Physical install-over-existing-data verification remains pending on a real device or emulator.

---

## 6. Critical Release Blockers (Action Required)

As mandated by Google Play developer policies and Task 18C compliance, the following placeholders must be replaced with live production URLs before submitting the app to production:

1.  **Privacy Support Email**: `support@phittoos.example.com` inside `/docs/PRIVACY.md` must be replaced with the publisher's real public support email.
2.  **Product Website URL**: `https://ai.studio/build` inside `/docs/PRIVACY.md` must be replaced with the actual hosted page/policy website.
3.  **Privacy Policy URL**: Host the plain-text/HTML version of the final `PRIVACY.md` on your web host (e.g., GitHub Pages) and register that live URL in Google Play Console.

---

## 7. Task 18D Status

“Task 18D build preparation is complete. Production signing, signed-device installation, and final upgrade verification remain manual release steps before distribution.”
