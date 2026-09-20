# Phittoos — Google Play Declarations & Console Guide

This guide compiles repository evidence and specific answers required when filling out Google Play Console declarations for Phittoos V1.

---

## 1. Data Safety Declaration

| Play Console Question | Suggested Answer | Repository Evidence & Verification |
| :--- | :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **No** | Phittoos V1 is strictly offline-first. No `android.permission.INTERNET` is declared or merged in the release build. All records are stored strictly on-device in Room SQLite. |
| **Is all of the user data collected by your app encrypted in transit?** | **Not applicable** | Because no data is transmitted off-device, there is no data in transit. |
| **Does your app provide a way for users to request that their data be deleted?** | **Yes** | 1. In App Settings, users can tap "Delete everything" which runs `SettingsViewModel.clearAllData()` to wipe all SQL tables and SharedPreferences.<br>2. Uninstalling the app triggers the OS to erase all private sandbox directories. |

---

## 2. App Access (Login / Authentication)

| Play Console Question | Suggested Answer | Repository Evidence & Verification |
| :--- | :--- | :--- |
| **Is access to your app restricted?** | **No, all functionality is available without restriction** | There is no login, OTP, registration, password, or cloud authentication. The app opens directly to onboarding and then the home screen. |

---

## 3. Advertising (Ads)

| Play Console Question | Suggested Answer | Repository Evidence & Verification |
| :--- | :--- | :--- |
| **Does your app contain ads?** | **No** | No ad networks or monetization libraries (such as AdMob, Unity, AppLovin) are included. The release build contains zero advertising or tracking SDKs. |

---

## 4. Financial Features Declaration

| Play Console Question | Suggested Answer | Repository Evidence & Verification |
| :--- | :--- | :--- |
| **Does your app provide any financial features?** | **Verify in Play Console** *(Select: Personal Finance / Ledger tracking)* | Phittoos is a casual, peer-to-peer social debt notebook. It does **NOT** offer payment processing, bank linking, mobile banking, UPI transfers, or credit lending services. |
| **Statement to provide if asked**: | *"Phittoos is an offline personal ledger for tracking casual peer debts. It does not access bank accounts, process UPI transactions, scan SMS messages, or handle actual money transfers."* | Confirm that no financial SDK or payment gateway (Razorpay, Stripe, UPI Intent integration) is present in the repository. |

---

## 5. Permissions & Notifications Disclosure

| Declared Permission | Purpose to Declare in Play Console | Repository Evidence |
| :--- | :--- | :--- |
| **`android.permission.POST_NOTIFICATIONS`** | *Used to show local, user-only overdue notifications.* | Notification channel is configured inside `ReminderNotificationHelper.kt` and scheduled using Jetpack `WorkManager`. Reminders are processed fully on-device. |
| **`android.permission.ACCESS_NETWORK_STATE`** | *Install-time permission bundled with AndroidX WorkManager.* | Used internally by AndroidX WorkManager runtime constraints. No network requests are made by the application. |

---

## 6. Privacy Policy Requirement

| Play Console Field | Required Action | Verification Checklist |
| :--- | :--- | :--- |
| **Privacy Policy URL** | Provide a public URL hosting the exact terms specified in `/docs/PRIVACY.md`. | Host `/docs/PRIVACY.md` on GitHub Pages or publisher website (replace `[REQUIRES_OWNER_VALUE]` placeholders), and enter the active URL under **App Content > Privacy Policy** in Play Console. |
