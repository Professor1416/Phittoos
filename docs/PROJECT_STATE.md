# Phittoos project state

## 1. Snapshot metadata

| Item | Inspected state |
| --- | --- |
| Repository | [Professor1416/Phittoos](https://github.com/Professor1416/Phittoos) |
| Inspection date | 2026-10-12 (Asia/Calcutta) |
| Source commit | `51913ed5d7ea4d586752d7abe0de5853d6c99669` |
| Checkout branch | `work`; HEAD equals local `origin/main` and remote `refs/heads/main` observed through `git ls-remote` |
| Baseline | Clean tracked/index state; no non-ignored untracked files; this document did not previously exist |
| Scope | Source, test-source, documentation, Git-history, and retained-artifact inspection; documentation-only changes |

**VERIFIED** means directly observed source or artifact evidence, within the limits stated. **REPORTED** means historical documentation or conversation claims. **PROPOSED** means future concepts requiring authorization. **UNKNOWN / UNVERIFIED** means evidence is insufficient. A feature marked **VERIFIED IMPLEMENTED** has an identifiable implementation; that label is not a runtime correctness, device-QA, or release certification.

No Android build, JVM suite, instrumentation test, emulator, or device check was executed for this task. Retained generated files are local evidence, not committed CI attestations. Remote main can advance after this observation. Refresh this snapshot after source changes, linking new verification to an exact commit and command.

## 2. Product summary and V1 scope

Phittoos is an offline-first, single-user Android money ledger for recording money lent to and borrowed from friends. Its exact tagline is **Tere mere hisaab… Phittoos!**, present in [`strings.xml`](../app/src/main/res/values/strings.xml).

V1 provides owner-entered records, owner-visible repayment history, local reminders, and private reliability summaries. A friend record is a local ledger identity, not a verified account. Marking an entry settled records the owner's assertion; it does not verify payment or a friend's agreement.

V1 has no login/cloud sync, friend verification, borrower notifications, group expense splitting, personal budgeting, banking, loan disbursement, or payment processing. Financial records live in the app's local storage, with explicit JSON/CSV exports to a user-selected provider as exceptions to that storage boundary. Platform transfer behavior remains unverified (§9).

## 3. Current technical stack

Declared versions below come from [`app/build.gradle.kts`](../app/build.gradle.kts), [`libs.versions.toml`](../gradle/libs.versions.toml), and [`settings.gradle.kts`](../settings.gradle.kts); they are not a fresh resolved-dependency audit.

| Component | Verified declaration/use |
| --- | --- |
| Android module / identity | Single `:app`; namespace/application ID `com.professor1416.phittoos`; version `1.0.0`, code `1` |
| Android levels | min SDK 24; target SDK 36; compile SDK release 36, minor API 1 |
| Build | Android Gradle Plugin 9.1.1; Gradle distribution 9.3.1 in [`gradle-wrapper.properties`](../gradle/wrapper/gradle-wrapper.properties) |
| Kotlin | Compose compiler plugin 2.2.10; KSP 2.2.10-2.0.2; Android plugin supplies built-in Kotlin support |
| UI | Compose BOM 2024.09.00; Material 3; Activity Compose 1.10.1; Navigation Compose 2.8.9 |
| Lifecycle / async | Lifecycle 2.8.7; coroutine Core/Android 1.10.2; ViewModel, Flow/StateFlow |
| Persistence | Room runtime/KTX/compiler 2.7.0, KSP generation; SQLite database schema version 4; SharedPreferences |
| Scheduling | WorkManager 2.9.1; local `CoroutineWorker` |
| Test dependencies | JUnit 4.13.2, Robolectric 4.16.1, Roborazzi 1.59.0, coroutines-test 1.10.2, AndroidX Compose test APIs |
| Java distinction | Java source/target compatibility 11 in the app build; earlier cloud setup used JDK 21. These are different settings; no new JDK compatibility test was run |

The app applies the Secrets Gradle plugin 2.0.1 with `.env`/`.env.example` defaults. Google Services 4.5.0 is declared at root with `apply false`, not applied to `:app`. Catalog entries for Firebase, Retrofit, OkHttp, credentials, camera, and other libraries are not evidence those libraries are used: the app's dependency block does not reference them. No production network client was found in inspected source.

The checkout contains wrapper distribution properties but no tracked `gradlew`, `gradlew.bat`, or wrapper JAR. Documentation using `./gradlew` therefore assumes files absent from this snapshot. Do not infer that the current checkout supports that command unchanged.

## 4. Architecture snapshot

```text
MainActivity / PhittoosNavApp
  → Compose screens → ViewModel + StateFlow
  → PhittoosRepository + domain helpers
  → Room DAOs → AppDatabase / SQLite

Settings → SAF document picker → JSON backup or CSV export
WorkManager → SmartReminderWorker → reminder engine → local notification
UserPreferences → SharedPreferences + theme StateFlow
```

All Kotlin production paths below are relative to [`app/src/main/java/com/professor1416/phittoos`](../app/src/main/java/com/professor1416/phittoos).

| Path | Responsibility |
| --- | --- |
| [`PhittoosApplication.kt`](../app/src/main/java/com/professor1416/phittoos/PhittoosApplication.kt) | Lazily creates database, repository, preferences; supplies the real database/activity DAO to repository operations; notification channel and periodic scheduling initialization |
| [`MainActivity.kt`](../app/src/main/java/com/professor1416/phittoos/MainActivity.kt), `ui/navigation/Screen.kt` | Splash/edge-to-edge, lifecycle-aware theme collection, navigation, notification friend routing |
| `ui/screens/{onboarding,home,addtransaction,frienddetail,activity,settings}` | The six navigable screens; addtransaction also handles edit mode |
| `ui/viewmodel/` | Home, add/edit, friend detail, activity, and settings state/operations; constructor-based factories, not a separate DI framework |
| [`data/repository/PhittoosRepository.kt`](../app/src/main/java/com/professor1416/phittoos/data/repository/PhittoosRepository.kt) | Derived balances, mutation guards, activity writes, settlement/repayment, clear/restore/export reads |
| `data/{dao,model,db}` | SQL reads/writes, three Room entities, enum converters and migrations |
| [`domain/DueDateHelper.kt`](../app/src/main/java/com/professor1416/phittoos/domain/DueDateHelper.kt), [`domain/ReliabilityEngine.kt`](../app/src/main/java/com/professor1416/phittoos/domain/ReliabilityEngine.kt) | Date classification/delay and owner-only LENT reliability calculation |
| `reminder/`, `export/`, `data/preferences/` | Work scheduling/notification delivery; JSON and CSV; local user settings |

Repository `runInTransaction` uses `AppDatabase.withTransaction` only when its optional database argument is supplied. The application supplies it; several tests construct a repository without it. Such test constructions do not establish production transaction behavior. Settings SAF handling and some presentation eligibility/date checks remain in UI code; the source is not an absolute separation of all logic from Compose.

## 5. Feature implementation matrix

Test names identify existing coverage, not new execution in this task. All feature rows remain subject to §10 evidence limits.

| V1 capability | Status | Evidence and limits |
| --- | --- | --- |
| LENT/BORROWED entries and multiple entries per friend | VERIFIED IMPLEMENTED | `AddTransactionViewModel.saveTransaction`, repository `addTransaction`, transaction friend foreign key; `PhittoosAccountingEngineTest`. Amount-validation completeness is PARTIAL (§11) |
| Dashboard, friend balances, net position, search | VERIFIED IMPLEMENTED | Repository `dashboardTotals`, `friendsWithBalance`, `getFriendWithBalance`; `HomeViewModel`, `HomeScreen`. Initial/loading display and intermittent assertions remain unresolved |
| Individual / same-direction bulk settlement | VERIFIED IMPLEMENTED | `markTransactionAsPaid`, `settleAllSameDirectionForFriend`; `OfflineSettlementFlowTest`; mixed directions explicitly refused |
| Partial repayment and automatic full settlement | VERIFIED IMPLEMENTED | `recordRepayment`, conditional DAO update; `PartialRepaymentEngineTest`. True simultaneous-call coverage is UNVERIFIED |
| Due dates / overdue / historical transaction dates | VERIFIED IMPLEMENTED | `DueDateHelper`, add/edit date controls, repository future-date guard; `DueDateOverdueEngineTest`, `TransactionDateBackdateTest` |
| Owner-local reminders and activity history | VERIFIED IMPLEMENTED | WorkManager scheduler, reminder engine, `ActivityDao`, `ActivityScreen`; `SmartReminderEngineTest`, `ActivityReminderHistoryTest`. Device delivery/lifecycle guarantees UNVERIFIED |
| Edit/delete eligibility and history consistency | VERIFIED IMPLEMENTED | `updateOpenUnpaidTransaction`, `deleteOpenUnpaidTransaction`; `ContextualAddAndWrongEntryRecoveryTest`. Settled/repaid entries blocked; eligible creation activities updated/deleted |
| JSON backup / validated replacement restore | PARTIAL | `PhittoosBackupManager`, settings SAF UI, `PhittoosBackupRestoreTest`; meaningful round-trip and invalid-input guards exist, but export consistency, semantic validation and cross-store atomicity have limits (§7) |
| CSV export | VERIFIED IMPLEMENTED | `PhittoosCsvExporter`, `SettingsViewModel.generateCsvExport`, SAF `CreateDocument`; `SettingsAndExportTest`. Provider failure/formula handling limits remain |
| Reliability summary | VERIFIED IMPLEMENTED | `ReliabilityEngine.calculate`, badge UI; `ReliabilityEngineTest`; LENT-only local heuristic, not external creditworthiness verification |
| Settlement celebration | VERIFIED IMPLEMENTED | `FriendDetailViewModel`, `SettlementCelebrationCard`; celebration tests. Success plus zero remaining OPEN entries and active screen are required; visual/device behavior UNVERIFIED |
| Themes / onboarding / branding | VERIFIED IMPLEMENTED | SYSTEM/LIGHT/DARK preferences, `PhittoosTheme`, onboarding, splash/launcher resources; theme and accessibility/visual tests. No formal accessibility certification |
| Profile editing / reminder toggle / delete everything | VERIFIED IMPLEMENTED | `SettingsViewModel`, `UserPreferences`; settings tests. Whole-device/external-file erasure is not provided |
| Login, sync, linking, mutual confirmation, payments | NOT IMPLEMENTED | No V1 routes/services for these; extra enum statuses/catalog declarations do not constitute V2 implementation |
| Draft reminder sharing described in privacy policy | NOT IMPLEMENTED | No draft/share-sheet flow or `ACTION_SEND` found in production source; policy text alone is not feature evidence |
| Current device QA / signed production release | UNVERIFIED | No commit-linked device report or newly verified signed-release artifact in this task |

## 6. Financial integrity and accounting

Primary evidence: [`PhittoosRepository.kt`](../app/src/main/java/com/professor1416/phittoos/data/repository/PhittoosRepository.kt), [`TransactionEntity.kt`](../app/src/main/java/com/professor1416/phittoos/data/model/TransactionEntity.kt), and [`TransactionDao.kt`](../app/src/main/java/com/professor1416/phittoos/data/dao/TransactionDao.kt).

- **Representation:** amounts, paid amounts, and totals use `Double`, not integer paise. `effectivePaidAmount` clamps nullable paid amounts into `[0, amount]`; `effectiveRemainingAmount` clamps `amount - paid` at zero. These are defensive calculations, not a general invalid-row validator.
- **Direction and totals:** only OPEN entries contribute. LENT remaining amounts add to money receivable; BORROWED remaining amounts add to money owed. Net is `sum(LENT remaining) - sum(BORROWED remaining)`, per friend or across the dashboard. Totals use direct `Double` sums, without an explicit final rounding step. Equal opposite entries produce zero net while retaining two OPEN records.
- **Repayment:** rejects nonpositive, NaN/infinite amounts, missing/non-OPEN entries, and amounts exceeding remaining plus `0.0001`. Cumulative paid amount is rounded with `round(value * 100) / 100`. Full-settlement threshold is `amount - 0.005`; then paid becomes the full amount, status becomes CONFIRMED, and `settledAt` is stored. A partial repayment stays OPEN with null settlement time. Activity amount is the submitted repayment amount; final repayment has note `final_repayment`.
- **Settlement:** DAO mutations include `WHERE ... status = 'OPEN'` and return affected-row counts. Individual settlement changes only the specified ID and logs the remaining amount. Bulk settlement refuses empty/mixed-direction sets; same-direction OPEN entries for the friend become CONFIRMED with paid amount equal to amount. Repeated settlement of a non-OPEN entry returns failure. Net zero never triggers automatic settlement.
- **Editing/deletion:** repository requires OPEN status, no positive effective/raw paid amount, and no settlement timestamp. Editing preserves identity/friend and updates its TRANSACTION_CREATED activity; deletion removes associated transaction activities and the eligible transaction. Amount validation for edits rejects nonfinite/nonpositive values; past due dates and future transaction calendar dates are rejected.
- **Retention:** ordinary settlement retains transactions and event history. Explicit deletion of eligible untouched entries, replace-restore, delete-everything, uninstall, and DAO friend-delete cascades are distinct destructive paths. History is not universally undeletable.
- **Limits:** `addTransaction` guards future transaction dates but does not itself enforce positive/finite amounts or due-date validity. No database CHECK constraints enforcing financial values were found. Whole-accounting correctness, crash behavior, and simultaneous mutation races are not newly proven here.

Coverage includes `OfflineSettlementFlowTest.testC_mixedDirectionBulkSettlementIsPrevented`, `testD_zeroNetWithOpenTransactionsIsNotSettled`, `testI_idempotentSettlement`, and partial/overpayment tests. `PartialRepaymentEngineTest.testK_concurrentRepaymentsCannotOverpay` actually makes sequential calls. Do not describe it as concurrent stress verification.

The [AGENTS.md](../AGENTS.md) financial-integrity rules remain mandatory: any reproducible wrong balance, wrong-target repayment/settlement, corrupted history, migration, or partial restore is **P0 / NO RELEASE**, irrespective of build/test totals. No new reproduction was performed in this documentation task.

## 7. Data model and persistence

[`AppDatabase`](../app/src/main/java/com/professor1416/phittoos/data/db/AppDatabase.kt) is schema **4**, named `phittoos_database`, with `exportSchema = false` and three entities:

| Entity/table | Main fields and relationships |
| --- | --- |
| `Friend` / `friends` | Auto-generated Long ID; name, optional contact info, stored reliability tier string; not a remote user |
| `TransactionEntity` / `transactions` | Long ID; indexed friend foreign key with CASCADE delete; Double amount/nullable paid amount; direction, status, note; occurrence, optional due, optional settlement timestamps |
| `ActivityEntity` / `activity_records` | Long ID; friend foreign key with CASCADE delete; optional transaction ID, amount, direction, note, reminder stage; type/timestamp; friend/time indices. Transaction ID is not a Room foreign key |

Enums are stored as names. `TransactionStatus` includes OPEN, PENDING_CONFIRMATION, CONFIRMED, DISPUTED; normal inspected V1 mutations use OPEN → CONFIRMED. The additional values do not implement mutual confirmation/disputes. `Converters` silently defaults unknown stored direction/status/type strings to LENT/OPEN/TRANSACTION_CREATED; backup validation instead rejects unknown enum names.

Registered migrations: **1→2** adds nullable `settled_at`; **2→3** creates activity records and indices; **3→4** adds nullable `reminder_stage`. No destructive fallback is configured. Test source constructs legacy version-2 and version-3 databases in `ActivityReminderHistoryTest.testO_migrationFrom2To3PreservesData` (actually opened through v4) and `SmartReminderEngineTest.testT_databaseMigration3To4PreservesData`. No complete version-1 upgrade fixture or exported schema history was found. These facts do not establish every real upgrade is safe.

[`UserPreferences`](../app/src/main/java/com/professor1416/phittoos/data/preferences/UserPreferences.kt) uses MODE_PRIVATE `phittoos_prefs` for local profile, onboarding, reminder enabled (default true), theme, and last-backup time. Theme exposes StateFlow. It is not an encrypted preference implementation.

**Backup/restore:** [`PhittoosBackupManager`](../app/src/main/java/com/professor1416/phittoos/export/PhittoosBackupManager.kt) exports JSON format version 1: metadata, three preferences (profile/onboarding/reminder), friends, transactions and activities. Theme and last-backup time are not exported. Validation checks required sections, profile/friend names, positive unique IDs, known enums, friend/transaction references, finite positive amounts and paid bounds. It rejects CSV and versions newer than 1; it does not enforce exactly version 1, comprehensive timestamp/status/paid consistency, or activity transaction-to-friend consistency.

Restore validates and parses before calling repository `restoreValidatedData`; it clears and reinserts tables inside the repository's optional Room transaction. The manager's `database` parameter does not itself establish a transaction. Production supplies a database to the repository. Preference setters use separate SharedPreferences `apply()` calls even inside the Room block, so combined database/preference rollback cannot be claimed. The failed-restore test uses invalid references rejected before replacement, rather than an injected failure during commit. Export fetches tables separately without one encompassing read transaction; concurrent writes during export are an unverified consistency risk.

**CSV:** [`PhittoosCsvExporter`](../app/src/main/java/com/professor1416/phittoos/export/PhittoosCsvExporter.kt) writes 11 columns (IDs, friend name, direction, amount/paid/remaining, status, occurrence/due/settlement dates, note), ordered by occurrence then ID, with quoting for comma/quote/newline. It is a readable export, not a restore format, and has no spreadsheet-formula neutralization. Both formats are plaintext and user-controlled after export.

## 8. Reminders, dates and lifecycle behavior

[`DueDateHelper`](../app/src/main/java/com/professor1416/phittoos/domain/DueDateHelper.kt) classifies NONE/UPCOMING/DUE_TODAY/OVERDUE. It normalizes comparisons to noon in the supplied/default device timezone and calculates day differences by rounded milliseconds divided by 86,400,000. Settled/non-open states display historical due information; dueInfo also suppresses active overdue when remaining is zero. Calendar-boundary tests exist; arbitrary timezone changes and DST behavior are not certified by comments alone.

Add/edit defaults occurrence to now, supports past dates and rejects future calendar days in ViewModel and repository. A later timestamp within today's calendar day is permitted by that helper. Occurrence time, due date, repayment/activity time and settlement time are different fields. The UI uses date pickers; due dates today/future are accepted, past due dates rejected in the add form and edit repository path. Overdue calculations are reevaluated on repository flow evaluation/worker execution; no corresponding midnight timer was found in HomeViewModel or FriendDetailViewModel.

[`ActivityViewModel`](../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/ActivityViewModel.kt) separately refreshes activity date grouping on resume and at the next local midnight while active, cancelling its job on pause/clear. Its refresh trigger regroups activity labels but does not itself recompute the repository's `needsAttentionItems` flow. Group-label freshness and overdue-item freshness are distinct concerns; device rollover/timezone-change behavior remains unverified.

[`SmartReminderScheduler`](../app/src/main/java/com/professor1416/phittoos/reminder/SmartReminderScheduler.kt) enqueues a unique **one-day periodic** worker with KEEP and a separate immediate job with REPLACE. This is Android-managed scheduling, not exact-time delivery. Cancel currently targets periodic work, not the immediate work name; the worker checks the preference before proceeding. `PhittoosApplication` attempts scheduling at startup even when preferences are disabled, and catches initialization failures without reporting them.

[`SmartReminderEngine`](../app/src/main/java/com/professor1416/phittoos/reminder/SmartReminderEngine.kt) qualifies OPEN LENT entries with a positive due date and positive remaining amount. At **7/15/30 days overdue**, `ReminderStage.determineStageToSend` selects the highest eligible unsent stage, skips lower stages after a higher stage was sent, and records REMINDER_SENT only after the sender reports success. BORROWED/settled entries do not notify. Posting and activity persistence are separate steps; the sequential duplicate guard is not a crash-proof or database-unique delivery guarantee.

[`ReminderNotificationHelper`](../app/src/main/java/com/professor1416/phittoos/reminder/ReminderNotificationHelper.kt) uses channel **`repayment_reminders`**, checks notification availability/API-33 permission, and uses immutable PendingIntents. Notifications include a friend label and pending amount. Tapping routes to friend detail if that friend exists; transaction ID is carried but the inspected navigation uses friend ID. `SmartReminderWorker` retries exceptions while `runAttemptCount < 3`; that runtime policy is distinct from the agent troubleshooting policy.

Reliability is a local LENT-only heuristic: at least two completed LENT entries before non-NEW classification; available settlement delays and currently overdue LENT entries drive GREEN/YELLOW/RED. Missing delay data can still yield GREEN with a no-delays-recorded message; it is not proof of verified on-time repayment. Celebration is tied to successful settlement/full repayment with no remaining OPEN entries and an active friend-detail screen; pause clears the event.

## 9. Security and privacy snapshot

Verified source controls: [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) declares POST_NOTIFICATIONS, `allowBackup="false"`, an exported launcher `MainActivity`, and a non-exported AndroidX startup provider. It declares no INTERNET, contacts, location, camera, SMS, or broad storage permissions. No automatic sync/authentication/network-client or sensitive logging calls were found in production source searches. This is not a binary security or telemetry certification.

A retained **debug merged manifest** also declares WAKE_LOCK, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE, and the application dynamic-receiver permission, but no INTERNET. Those dependency-added declarations mean “only one permission” is accurate only for the source manifest/runtime request context, not every merged permission. Current release merging was not verified.

SQLite and preferences use the app sandbox; no application-level database encryption or app PIN/biometric gate was found. Notification content can expose ledger context according to device lock-screen settings; the builder sets no explicit private visibility override. Exports use settings `CreateDocument`/`OpenDocument` and content resolver streams, not a FileProvider. No `file_paths.xml` exists despite generic FileProvider guidance in AGENTS.md.

[`backup_rules.xml`](../app/src/main/res/xml/backup_rules.xml) is an essentially empty template; [`data_extraction_rules.xml`](../app/src/main/res/xml/data_extraction_rules.xml) has an empty cloud-backup element and no explicit device-transfer section. `allowBackup=false` records the intent to disable automatic backup, not verified behavior across devices/OEM transfer mechanisms. SAF providers may place exports in cloud storage chosen by the user; Phittoos exports are not end-to-end encrypted by application code. Deleting local data does not delete those external copies.

[`PRIVACY.md`](PRIVACY.md), [`PLAY_DECLARATIONS.md`](PLAY_DECLARATIONS.md), and static support/privacy pages express product disclosures, not independent audit results. Privacy text mentions reminder drafts that source does not implement. No legal/DPDP compliance, formal WCAG compliance, or absence of all vulnerabilities is claimed here.

## 10. Verification and testing

**New verification in this task:** read-only source/history inspection, document evidence/link/scope checks and Git checks. Android builds/runtime tests: **NOT APPLICABLE — not run (documentation-only scope)**.

There are **25 JVM test-source files** and one instrumented test-source file. The latter's `ExampleInstrumentedTest.useAppContext` checks application identity only; it is not an end-to-end ledger/device suite. No tracked `.github` CI workflow was found. GitHub-hosted external checks/issues were not independently queried.

| Coverage area | Existing source evidence |
| --- | --- |
| Directions, netting, target isolation, settlement | `PhittoosAccountingEngineTest`, `AccountingStateCorrectnessTest`, `OfflineSettlementFlowTest` |
| Partial/full repayment, bounds and rejection | `PartialRepaymentEngineTest`; sequential calls do not establish true concurrency |
| Due states / backdating | `DueDateOverdueEngineTest`, `TransactionDateBackdateTest` |
| Reminder qualification, stages, activity/migrations | `SmartReminderEngineTest`, `ActivityReminderHistoryTest` |
| JSON/CSV/preferences and edit/delete | `PhittoosBackupRestoreTest`, `SettingsAndExportTest`, `ContextualAddAndWrongEntryRecoveryTest` |
| Reliability, themes, celebration, UI/layout | `ReliabilityEngineTest`, `ThemePreferenceTest`, celebration/accessibility/responsive/visual test classes |

**Retained evidence inspected:** `app/build/test-results/testDebugUnitTest/TEST-*.xml` contains 25 suites, **269 tests, 1 failure, 0 errors, 0 skipped**. The failing `ExampleRobolectricTest` case is `zero net with open transactions displays Overall balance 0 and NOT All settled`; its XML timestamp is `2026-10-12T08:01:26.556Z`. Thus 268 tests passed in that retained run. Artifacts do not embed a reliable source-SHA attestation; prior session context associates them with onboarding at `67c777a`. `git diff 67c777a HEAD --name-only` shows only AGENTS.md, corroborating unchanged tracked app/test source since then, but not a fresh execution at this HEAD.

The failure observed initial “All settled up” text instead of the expected zero-net OPEN-entry summary. `HomeViewModel` starts with loading/zero state; `HomeScreen` renders its summary without a loading gate, while the test asserts after Compose idleness rather than explicitly awaiting the Room-backed state. Timing is a plausible explanation, not a proven root cause or proof that production is unaffected. Earlier class-only 12/12 passing and debug APK/signature verification are **REPORTED prior-session results**, not rerun here. A retained debug APK exists; its current integrity/signature was not reverified.

[`RELEASE_CANDIDATE.md`](RELEASE_CANDIDATE.md) reports 215 passing tests and unsigned APK/AAB outputs dated 2026-09-19, with source SHA `N/A`. Treat these as older reported evidence, not the current 269-test result or current release approval. Historical physical-device testing has no commit-linked report verified here and cannot be extrapolated.

Expected code-task checks in AGENTS.md are `gradle :app:testDebugUnitTest` and `compile_applet`; the latter is an external helper absent from tracked files. Earlier cloud builds used `:app:assembleDebug`. `GreetingScreenshotTest` captures to tracked `app/src/test/screenshots/greeting.png`, so a future test run must preserve intentional files and inspect generated changes.

## 11. Known issues, risks and technical debt

Priorities below are review triage, not invented issue IDs. Source-confirmed gaps are distinguished from unexecuted failure scenarios. No new runtime financial-corruption reproduction was attempted; any such reproduction must be classified P0 / NO RELEASE under AGENTS.md.

| Priority | Finding / impact | Evidence and status |
| --- | --- | --- |
| P1 | Incomplete amount boundary validation | `addTransaction` lacks finite/positive checks; `AddTransactionViewModel.saveTransaction` checks parse/null and `<= 0`, not infinity, and `setAmount` does not limit decimal precision/length. Extremely large digit input and fractional precision need reproduction; potential invalid balances/export failure are UNVERIFIED, not cleared for release |
| P1 | Backup/restore correctness assurance incomplete | Separate export reads; Room/SharedPreferences split; weak status/date/cross-friend semantic validation. Existing invalid-input tests do not prove mid-restore rollback. Corruption scenarios UNVERIFIED; reproducible loss would escalate to P0 |
| P1 | False success possible on document-provider null stream | Settings CSV/JSON callbacks use nullable `openOutputStream(uri)?.use`, then announce success; backup timestamp can advance without a write. Source-confirmed control-flow gap; provider reproduction/data-loss extent UNVERIFIED |
| P1 | Current full regression is not green | Retained 268/269 result and intermittent dashboard assertion; production loading-state display and test synchronization need separate diagnosis |
| P1 | Release/device/privacy verification incomplete | No new signed release, commit-linked physical QA, full upgrade-chain or transfer-policy evidence; not a proven exploit or completed release gate |
| P2 | Reminder delivery not atomic/exact | Notification posting precedes history write; duplicate guard lacks a unique constraint/transaction. Scheduling errors can be swallowed; crash/overlap delivery scenarios unverified |
| P2 | Monetary precision/converter robustness | Direct Double totals, unbounded input precision and silent enum fallbacks deserve boundary/corrupt-storage tests. Do not claim exact-decimal accounting or silently migrate representation |
| P2 | CSV spreadsheet interpretation | Quoting exists but formula prefixes in names/notes are not neutralized. Consumer-dependent risk, not a demonstrated app exploit |
| P2 | Documentation/build reproducibility drift | Release notes mention removed Google Services although root declaration remains; wrapper entry points absent; AGENTS.md uses `overdue_reminders` versus actual `repayment_reminders` and generic FileProvider guidance; privacy mentions missing draft sharing. Other docs remain unchanged |
| P2 | Evidence coverage limits | Schema export disabled; no version-1 fixture found; “concurrent” test is sequential; no repository CI workflow. Absence of coverage is not proof of incorrect migrations |

## 12. Release readiness snapshot

**Current release readiness: UNVERIFIED; no release approval.** This document establishes implementation presence and unresolved evidence, not a green financial/security QA gate.

- Build identity and signing logic exist. Debug expects a root debug keystore; release signing is configured only if the upload keystore exists and uses environment-provided passwords. No credential values were inspected or added.
- Current full-suite PASS is not evidenced. Retained debug build/report evidence and old unsigned release hashes do not establish current signed APK/AAB correctness.
- Required upgrade, backup failure/recovery, notification permission/lifecycle, lock-screen disclosure, TalkBack/large-font, icon/splash and physical-device checks remain unverified at this commit.
- Static support/privacy HTML, Play guidance and licensing/brand documentation are present. Public hosting, Play Console declarations/submission, production signing and store approval are unknown here.
- [`LICENSES.md`](LICENSES.md) contains AndroidX/Kotlin/icon attributions; no exhaustive resolved/transitive-license audit was performed. [`PHITTOOS_BRAND_V1.md`](branding/PHITTOOS_BRAND_V1.md) supplies the visual specification, not proof of multi-device mask/accessibility correctness.

## 13. V1 to V2 boundary

Current V1 is an owner-only local ledger. **PROPOSED / NOT CURRENT:** optional friend linking, shared records, mutual confirmations, secure sync, authentication and new privacy controls. Catalog placeholders and PENDING_CONFIRMATION/DISPUTED enum values are not completed V2 features.

Before any separately authorized V2 work, decide:

- Who owns a record, who may view/change it, and whether private owner notes/reliability must stay separate from shared data.
- How identity, linking consent, session revocation, invite expiry/reuse prevention and unlinking are authorized.
- Which trust boundaries enforce access, idempotency, replay protection and mutation authorization.
- What confirmation means, how offline conflicts resolve without losing financial history, and how identifiers/order/audit evidence are preserved.
- What data moves off device, how deletion/retention/export work, and what disclosures/permissions must change.
- What migration/backward-compatibility and financial-integrity verification gates are required.

No backend, vendor, identity provider, credential-storage library, invite mechanism, conflict algorithm, framework, or implementation plan is selected by this snapshot. Permanent anti-goals in AGENTS.md remain in force.

## 14. Next recommended verification activities

1. In a separately authorized verification task, reproduce amount overflow/precision, restore semantic/commit-failure and null-document-stream cases using synthetic data. Escalate reproducible financial corruption to P0; preserve existing assertions.
2. Diagnose the dashboard loading/assertion behavior, then record targeted and full-suite outcomes tied to the exact SHA. Do not infer success from isolated reruns.
3. Verify true overlapping repayments/settlements, export during mutation, reminder crash/duplicate behavior, and upgrades from actual v1/v2/v3 databases with preserved balances/history.
4. Test signed-build identity/permissions and physical-device SAF, notification, backup/restore, timezone/DST, large-text/TalkBack and transfer behavior; record OS/device and commit.
5. Reconcile privacy/Play/build documentation with verified behavior and confirm public support/privacy hosting and release checks in their own authorized scope. This is a verification list, not a V2 feature backlog.

## 15. Evidence and limitations

Use these relative paths to recheck the snapshot:

- [`AGENTS.md`](../AGENTS.md): operating scope, V1 boundaries, financial release blockers, future-choice neutrality and reporting rules.
- [`app/build.gradle.kts`](../app/build.gradle.kts), [`libs.versions.toml`](../gradle/libs.versions.toml), [`settings.gradle.kts`](../settings.gradle.kts): app identity, dependency declarations and module/build structure.
- [`PhittoosRepository.kt`](../app/src/main/java/com/professor1416/phittoos/data/repository/PhittoosRepository.kt): `dashboardTotals`, `addTransaction`, `recordRepayment`, `markTransactionAsPaid`, `settleAllSameDirectionForFriend`, edit/delete, `restoreValidatedData`.
- [`AppDatabase.kt`](../app/src/main/java/com/professor1416/phittoos/data/db/AppDatabase.kt), [`Converters.kt`](../app/src/main/java/com/professor1416/phittoos/data/db/Converters.kt), [`data/model`](../app/src/main/java/com/professor1416/phittoos/data/model), [`data/dao`](../app/src/main/java/com/professor1416/phittoos/data/dao): schema, relations, converters and SQL guards.
- [`export`](../app/src/main/java/com/professor1416/phittoos/export), [`SettingsScreen.kt`](../app/src/main/java/com/professor1416/phittoos/ui/screens/settings/SettingsScreen.kt), [`SettingsViewModel.kt`](../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/SettingsViewModel.kt): plaintext formats, SAF, replacement/deletion and stream-success handling.
- [`reminder`](../app/src/main/java/com/professor1416/phittoos/reminder), [`domain`](../app/src/main/java/com/professor1416/phittoos/domain), [`UserPreferences.kt`](../app/src/main/java/com/professor1416/phittoos/data/preferences/UserPreferences.kt): stage policy, time calculation, heuristic reliability and preferences.
- [`JVM test sources`](../app/src/test/java/com/professor1416/phittoos), [`instrumented test`](../app/src/androidTest/java/com/professor1416/phittoos/ExampleInstrumentedTest.kt): coverage and assertion limitations described in §10. Retained XML/debug artifacts under `app/build` are ignored/local, not portable evidence links.
- Recent history: `0d0b34c` transaction-date selection; `39049d1` edit/delete; `f3af526` UI loading/terminology; `657a046` due-date/repayment validation; `59cfe66` Firebase removal/security; `51913ed` AGENTS.md-only policy completion. Commit titles orient inspection; they do not independently prove behavior.

The supplied conversation previews were historical context only. A conversation reader was unavailable, so referenced chats were not independently retrieved. No secrets, user financial records or backup contents were inspected. Unknowns above are intentional; future evidence should replace them with precise outcomes rather than silently upgrading source presence into a PASS.
