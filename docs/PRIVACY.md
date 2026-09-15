# Phittoos — Privacy Policy

Last updated: September 15, 2026

Phittoos is a simple, private, offline-first ledger application designed to help you track money between you and your friends. **"Tere mere hisaab… Phittoos!"**

Because we believe your financial history is highly personal, Phittoos is designed with a **privacy-first approach**. This Privacy Policy explains exactly how Phittoos handles your information.

---

## 1. What Information We Store and Where
Phittoos stores all data strictly **locally on your own device**. There are no user accounts, cloud servers, or remote databases in Phittoos V1.

The app stores the following data:
*   **Profile Details**: The name you choose during onboarding (used solely to sign off on reminder messages and show on your dashboard).
*   **Friend Lists**: The names of friends you add to record transactions.
*   **Transactions**: Amounts in rupees, transaction directions (Lent or Borrowed), dates, statuses (Open or Settled), and any optional notes you write.
*   **Repayments**: History of partial and full payments recorded.
*   **Application Preferences**: Settings such as notification preferences.

## 2. On-Device Storage and Isolation
All data is isolated inside your device’s private sandbox using an Android SQLite database (via Jetpack Room) and standard Android `SharedPreferences`.
*   **No Internet Transmission**: None of your ledger balances, transaction history, or friend details are ever uploaded to, shared with, or synced to any external server or cloud service.
*   **100% Offline Access**: The app functions fully without active internet or network connections.

## 3. Local Reminders and Notifications
Phittoos offers local, owner-only reminders to help you remember outstanding balances:
*   These reminders are computed, scheduled, and triggered **directly on your device** using Android system schedulers (such as `WorkManager`).
*   No external notification servers, push service payloads, or borrower communication channels are used.

## 4. User-Controlled Exports and Backups
Phittoos provides features that let you export or backup your data, which are fully controlled by you:
*   **CSV Export**: You can save your ledger history as a plain-text CSV file to view or share. 
*   **Phittoos Backup**: You can create a versioned JSON backup of your database.
*   **Storage Access Framework (SAF)**: All file exports and backups use Android's native Storage Access Framework, which lets you choose exactly where on your device (or local storage provider) to write the file. The app does not request or require broad read/write access to your device's files.
*   **Restoring Backups**: Restoring a Phittoos JSON backup is atomic and **replaces all current local data** on your device with the contents of the backup. It does not merge data.

## 5. Deleting Your Data
You have complete control over your data:
*   A **"Delete everything"** action is available under App Settings.
*   Selecting this action permanently deletes all friend lists, transaction histories, and preferences from your device's local database.
*   Uninstalling the Phittoos app also automatically triggers the Android OS to erase all of the app's local data.
*   *Note: Any CSV exports or backup JSON files you saved externally to your device's file system or shared folders are not automatically deleted and must be managed or deleted by you manually.*

## 6. What Phittoos Does NOT Collect or Access
To respect your privacy, the current version of Phittoos does **not**:
*   Request or access your device's contacts list.
*   Request or access your location or device sensors.
*   Link to your bank accounts, credit cards, or scan SMS alerts.
*   Use any third-party tracking, analytics, or behavioral advertisement SDKs.

## 7. Updates to This Policy
We may update our Privacy Policy as we introduce new capabilities. If future updates introduce optional cloud-linked or shared-record features (such as V2 paired linking), they will be strictly opt-in, and this policy will be revised to clearly describe those flows.

## 8. Contact & Support
If you have questions about this privacy policy or face any technical issues, please contact us:
*   **Support Email**: `support@phittoos.example.com` (or verify in Play Console support settings)
*   **Project URL**: https://ai.studio/build
