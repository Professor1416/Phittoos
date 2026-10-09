# Phittoos Agent Engineering Guide

## 1. Purpose

This document is the operating contract and engineering standard for AI coding agents working on the Phittoos codebase.

Phittoos is a private personal money ledger where correctness is paramount. Accidental accounting drift, corrupted ledger history, or phantom balances destroy user trust.

### Operating Mandates
- **Inspection Precedes Implementation**: Every task must begin by inspecting actual repository code, models, DAOs, and existing tests. Never assume architecture or features based on documentation alone.
- **Financial Correctness Over UI Appearance**: A visually appealing screen is meaningless if accounting invariants, state transitions, or persistence rules are violated.
- **Doer / Checker Verification Model**: Every code modification must satisfy both execution precision and rigorous adversarial checking before completion.
- **Definition of Done**: A task is complete only when:
  1. Business and financial logic are mathematically correct and idempotent.
  2. Persistence and schema safety are preserved (with migrations if schema changes).
  3. Targeted unit/Robolectric tests verify the change and edge cases.
  4. The full test suite (`gradle :app:testDebugUnitTest`) passes with zero regressions.
  5. The application builds cleanly (`compile_applet`).

---

## 2. Product Boundaries

### Core Identity
Phittoos is a private, person-to-person Android hisaab / informal money ledger.

**Approved Tagline**: *"Tere mere hisaab… Phittoos!"*

### Primary Use Case (Current V1)
Record and track informal money exchanges strictly between two individuals (the user and one friend):
- Money lent to a friend (friend owes user)
- Money borrowed from a friend (user owes friend)
- Open transactions and remaining amounts
- Partial repayments and cumulative balance reduction
- Full settlement and settlement celebrations
- Due dates, overdue calculations, and local reminders
- Transaction occurrence dates (including backdated entries)
- Private transaction and activity history
- Friend-level net balance and private reliability insights

### Permanent Anti-Goals (What Phittoos Must NEVER Become)
- **NOT** a group expense splitter (do NOT turn Phittoos into Splitwise).
- **NOT** a personal budgeting or expense tracker (no categories, salary tracking, or spending analytics).
- **NOT** a bank, digital wallet, or neo-bank.
- **NOT** a credit lender or interest calculator (zero interest, zero loan terms).
- **NOT** a payment processor, payment gateway, or UPI transaction app.

### Scope & Future Capability Boundary Rule
- **Cloud / Shared Functionality**: Cloud/shared functionality is outside current V1 unless explicitly authorized by a scoped future task. When authorized, it must pass authentication, server authorization, sync/idempotency, conflict-handling, security, privacy and DPDP gates.
- **Payment / UPI Handoff**: External payment/UPI handoff may only be considered in a specifically authorized future task and must never imply Phittoos processes or verifies the payment itself.
- **Core Boundary Rule**: Every proposed change must improve one-to-one hisaab recording, repayment, settlement, or trustworthy history. If a feature drifts into group splitting, budgeting, banking, or credit lending, backlog it immediately.

---

## 3. Repository Source of Truth

Agents must evaluate evidence strictly in this order of precedence:
1. **Actual current source code** (`app/src/main/java/...`)
2. **Current automated tests** (`app/src/test/java/...`)
3. **Current Room database schema and migrations** (`data/db/AppDatabase.kt`)
4. **Existing repository documentation** (`docs/...`)
5. **Task instructions**

### Conflict Resolution Protocol
- If task documentation claims a feature exists or works in a certain way, but the source code proves otherwise, **repository truth wins**.
- Report the mismatch explicitly rather than silently writing conflicting code or inventing non-existent abstractions.
- Never claim a feature exists solely because documentation references it.

### Tripartite State Classification
When documenting or evaluating features, strictly distinguish between:
1. **Current Implementation**: Confirmed in actual source code, DAOs, and automated tests.
2. **Planned Feature Work**: Scoped and authorized tasks targeted for future milestones.
3. **Hypothetical Concepts**: Exploratory ideas or architectural options not yet approved.

*Rule*: Never present planned future work or hypothetical proposals as current application behavior.

---

## 4. Required Engineering Loop & Doer/Checker Model

Every task on this repository must follow this structured engineering loop:

```
INSPECT
  → ROOT CAUSE / REQUIREMENTS
  → PLAN
  → IMPLEMENT MINIMALLY (DOER)
  → TARGETED TEST
  → FULL REGRESSION (gradle :app:testDebugUnitTest)
  → CHECKER REVIEW (CHECKER)
  → CORRECT
  → RE-TEST
  → STOP
```

### Pre-Implementation Checklist
Before editing or creating code, identify and document:
1. **Affected files**: Exact files to create or modify.
2. **Affected accounting rules**: Impact on balance, remaining amount, status, or direction.
3. **State transitions**: Entities entering or leaving `OPEN`, `CONFIRMED`, etc.
4. **Persistence impact**: Room schema changes, table alterations, indices, or type converters.
5. **Migration impact**: Is a new database version and migration required?
6. **Backup impact**: Does the change affect JSON export/import or CSV export?
7. **Privacy & permission impact**: Does it touch storage, notifications, or require permissions?
8. **Test coverage**: Which existing tests protect this behaviour, and what new tests are needed?

### Doer / Checker Roles
- **DOER**: Implements the smallest safe, focused change that satisfies requirements without collateral edits or scope creep.
- **CHECKER**: Independently reviews:
  - Original requirement vs implementation
  - Final git diff
  - Accounting invariants and arithmetic safety
  - Entity state transitions
  - Persistence integrity and migrations
  - Test validity and assertion depth
  - Privacy, permission, and security boundaries
  - Absence of unrelated refactorings or cosmetic cleanups
  *Mandate*: The Checker must actively simulate financial edge cases (e.g., zero amounts, boundary overpayments, double settlements, mixed directions) and reject changes with insufficient verification evidence.

### Change Discipline & Test Integrity
- **No Unrelated Refactoring**: Do not reformat unrelated files, clean up unrelated methods, or change architectural patterns outside the scope of the assigned task.
- **Never Weaken Tests**: Never weaken test assertions, remove tests, or widen floating-point tolerances to make a failing test pass. If a test fails, find the root cause in the code or update the test only if the underlying business specification has legitimately changed.

---

## 5. Financial Integrity Release Blockers

Any reproducible defect in the following categories constitutes a **P0 / NO RELEASE** blocker:
- Incorrectly changing a friend's net balance
- Incorrectly altering a transaction's remaining amount
- Settling the wrong transaction ID, wrong friend ID, or wrong direction
- Settling an already-settled transaction (double settlement)
- Corrupting partial repayment history or cumulative `paidAmount`
- Closing, modifying, or settling unrelated open transactions
- Losing, deleting, or hiding historical financial records
- Corrupting or failing a Room database migration
- Partially or improperly restoring a backup file

**Absolute Rule**: A successful build (`compile_applet`) or a passing general test suite cannot override a financial-integrity defect. Any financial inaccuracy halts release immediately.

---

## 6. Architecture Boundaries

The repository follows a clean, pragmatic MVVM pattern without unnecessary boilerplate:

```
Compose UI (Screens, Components, Theme)
  ↓ (User Intent / Events)
ViewModel (UI State, StateFlow, Coroutines)
  ↓ (Operations / Queries)
Domain Helpers (DueDateHelper, ReliabilityEngine)
  ↓ (Orchestration & Transactions)
PhittoosRepository (Centralized Coordinator, runInTransaction)
  ↓ (Data Access)
DAOs (FriendDao, TransactionDao, ActivityDao)
  ↓ (SQLite Storage)
Room Database (AppDatabase v4)
```

### Supporting Systems
- **Preferences**: `UserPreferences` backed by `SharedPreferences` (`phittoos_prefs`) for theme mode, onboarding state, user name, and reminder preferences.
- **Export & Backup**:
  - `PhittoosBackupManager`: Versioned JSON backup (v1) with strict semantic validation and transactional restore.
  - `PhittoosCsvExporter`: Formatted CSV export for user viewing or external sharing (non-restorable).
- **Local Reminders**:
  - `SmartReminderEngine`: Deterministic evaluation of overdue lent transactions.
  - `SmartReminderScheduler`: Local `WorkManager` scheduling (periodic daily & immediate one-time).
  - `ReminderNotificationHelper`: Local notification channel (`overdue_reminders`).

### Layer Responsibilities
- **Compose UI**: Pure presentation and user interaction. Renders state provided by ViewModels. Must never perform arithmetic, financial validation, or direct database calls.
- **ViewModel**: Manages UI state, handles user interactions, invokes repository methods, and formats presentation strings.
- **Domain**: Pure Kotlin objects/functions for complex deterministic calculations:
  - `DueDateHelper`: Calendar-day due date state calculations (`UPCOMING`, `DUE_TODAY`, `OVERDUE`).
  - `ReliabilityEngine`: Private, deterministic friend repayment reliability tier calculation (`NEW`, `GREEN`, `YELLOW`, `RED`).
- **PhittoosRepository**: Single source of truth for business rules, transaction boundaries (`runInTransaction` / `withTransaction`), cross-DAO synchronization, and financial validations.
- **DAO & Room**: Low-level SQL queries, entity persistence, and foreign key cascades.

---

## 7. Protected Accounting Invariants

Every agent must preserve all 15 core accounting invariants:

1. **LENT Direction**: Increases money the friend owes the user (+ balance).
2. **BORROWED Direction**: Increases money the user owes the friend (- balance).
3. **No Direction Merging**: LENT and BORROWED must never be combined into unsigned absolute numbers or single-signed balances without explicit direction context.
4. **Mathematical Balance**: Friend net balance strictly equals:
   $$\text{Net Balance} = \sum (\text{LENT remaining}) - \sum (\text{BORROWED remaining})$$
   computed across `OPEN` transactions.
5. **Targeted Repayment**: A partial repayment reduces only the single intended transaction.
6. **Cumulative Repayments**: Repayments accumulate into `paidAmount`:
   $$\text{newPaid} = \text{round}((\text{currentEffectivePaid} + \text{repaymentAmount}) \times 100.0) / 100.0$$
7. **Non-Negative Remaining**: `effectiveRemainingAmount` must never be negative:
   $$\text{effectiveRemainingAmount} = (\text{amount} - \text{effectivePaidAmount}).\text{coerceAtLeast}(0.0)$$
8. **Single Settlement Transition**: Reaching full payment transitions status from `OPEN` to `CONFIRMED` exactly once and sets `settledAt` timestamp.
9. **Idempotency**: Repeated settlement or repayment invocations on already-settled transactions must fail gracefully with zero state change.
10. **History Retention**: Settled transactions are permanently retained in history; they must never be deleted or hidden from historical records.
11. **Mixed-Direction Bulk Safety**: If a friend has open transactions in both directions (both LENT and BORROWED), bulk settlement (`settleAllSameDirectionForFriend`) **must refuse** to execute. Transactions must be settled individually.
12. **Net Zero Does Not Mean Settled**: If a friend has ₹500 LENT and ₹500 BORROWED, the net balance is ₹0, but both transactions remain open until settled individually. Never auto-settle on net zero.
13. **Consistent Recalculation**: Editing or deleting an open transaction must atomically recalculate dependent friend balances and dashboard totals.
14. **Immutable Financial History**: Historical transactions with repayment history or settled status must never have their amounts or directions silently rewritten.
15. **Target Isolation**: Settlement must never affect:
    - the wrong transaction ID,
    - the wrong friend ID,
    - the wrong transaction direction,
    - or an already-settled transaction.

---

## 8. Money Input & Representation

### Current Representation: Double
The repository currently represents monetary values as `Double`:
- `TransactionEntity.amount: Double`
- `TransactionEntity.paidAmount: Double?`
- `FriendWithBalance.netBalance: Double`
- `DashboardTotals`: `Double`

> **CRITICAL RULE**: Do **NOT** invent or falsely claim integer/paise storage exists. The codebase uses `Double`.

### Arithmetic Safety & Rounding Rules
- Never introduce casual unrounded floating-point calculations.
- Repayment additions must be rounded to two decimal places:
  `val newPaid = kotlin.math.round(unroundedPaid * 100.0) / 100.0`
- Settlement threshold comparison allows a `0.005` floating-point tolerance:
  `val isFullySettled = newPaid >= tx.amount - 0.005`
- Overpayment check uses an epsilon:
  `repaymentAmount > remaining + 0.0001`
- Any future architectural migration to Integer/Paise or BigDecimal must be executed as a dedicated, fully tested migration task with schema updates, migration scripts, backup compatibility checks, and zero data loss.

### Input Validation
Input validation must reject:
- Amounts $\le 0.0$
- Values that are `NaN` or `Infinite`
- Overpayments exceeding the remaining balance
- Malformed numeric strings or non-numeric input

---

## 9. Partial Repayment Rules

When working on or modifying repayment logic:

### State Transitions
- **Partial Payment** ($0 < \text{amount} < \text{remaining}$):
  - `paidAmount` increases by payment amount (rounded to 2 decimal places).
  - `status` remains `TransactionStatus.OPEN`.
  - An `ActivityEntity` of type `ActivityType.PARTIAL_REPAYMENT` is inserted.
- **Full Payment** ($\text{newPaid} \ge \text{amount} - 0.005$):
  - `paidAmount` is set to `tx.amount`.
  - `status` transitions to `TransactionStatus.CONFIRMED`.
  - `settledAt` is set to the event timestamp.
  - An `ActivityEntity` of type `ActivityType.SETTLED` is inserted with note `"final_repayment"`.
  - If no open transactions remain for the friend, `isAllSettledForFriend` is flagged as `true` (enabling UI celebration).

### Mandatory Repayment Tests
Every repayment change must be verified against:
- Value strictly below remaining amount
- Value exactly equal to remaining amount
- Value exceeding remaining amount (must return `EXCEEDS_REMAINING`)
- Zero and negative amounts
- Malformed inputs
- Cumulative multi-step repayments
- Attempted repayment on an already-settled transaction
- Mixed-direction friend handling

---

## 10. Settlement Rules

### Individual Settlement (`markTransactionAsPaid`)
- Marks a single `OPEN` transaction as `CONFIRMED`.
- Sets `settledAt = timestamp`.
- Inserts an `ActivityEntity` with `type = ActivityType.SETTLED` and amount equal to the remaining balance.
- Returns `SettlementResult(success, isAllSettledForFriend)`.

### Bulk Settlement (`settleAllSameDirectionForFriend`)
- **Safety Precondition**: All open transactions for the friend must have the **same direction** (all `LENT` or all `BORROWED`).
- If open transactions contain **both** `LENT` and `BORROWED`, the function **must immediately return `false`**.
- If valid, marks all open transactions as `CONFIRMED` and inserts `SETTLED` activity records for each.

### Settlement Celebration
- The settlement celebration ("*Tere mere hisaab… Phittoos!*") triggers **only** when all open transactions for a friend are fully settled (`openCount == 0`).
- It must **not** trigger on deleting an entry, cancelling an action, or when other open transactions remain.

---

## 11. Date & Due-Date Rules

### Due Dates (`dueDate: Long?`)
- Due dates are optional.
- Logical states evaluated by `DueDateHelper.calculateDueState`:
  - `NONE`: No due date set or transaction is settled.
  - `UPCOMING`: Due date is in the future relative to calendar day.
  - `DUE_TODAY`: Due date matches the current calendar day.
  - `OVERDUE`: Current calendar day is strictly past the due date (`isActivelyOverdue = true`).
- **Timezone & Calendar Math**: Normalizes dates to noon (12:00:00) in the device's local timezone to prevent DST and clock-time distortions.
- **Settled Transactions**: A settled (`CONFIRMED`) transaction is **never actively overdue**. Its due date is rendered as historical info ("Due `<date>`").

### Transaction Occurrence Dates (`createdDate: Long`)
- Represents when the transaction occurred in the real world.
- Defaults to today / current timestamp.
- **Backdating**: Supported for logging past transactions.
- **Future Date Prohibition**: Transaction dates cannot be set in the future (`DueDateHelper.isFutureDate` check). The Add/Edit flow and Repository strictly reject future dates.
- Occurrence date (`createdDate`), repayment dates, and settlement event dates (`settledAt`) are distinct and must never be conflated.

---

## 12. Edit / Delete Rules

### Eligibility Criteria
Editing and deletion are restricted to **open, untouched transactions only**:
- `status == TransactionStatus.OPEN`
- `paidAmount == null || paidAmount == 0.0`
- `effectivePaidAmount == 0.0`
- `settledAt == null`

### Rejection Mandates
- If any partial repayment has occurred (`hasRepayments == true`), direct edit and deletion are **strictly blocked** (`HAS_REPAYMENTS`).
- If the transaction is settled, edit and deletion are **strictly blocked** (`NOT_OPEN`).
- Deleting an eligible transaction must remove its associated `TRANSACTION_CREATED` activity record to prevent orphaned activity entries.
- Editing an eligible transaction must update its corresponding `TRANSACTION_CREATED` activity record to keep notes, amounts, and dates synchronized.

---

## 13. Room / Persistence Safety

### Database Identity
- Class: `AppDatabase`
- Name: `phittoos_database`
- Current Version: **4**
- Entities: `Friend`, `TransactionEntity`, `ActivityEntity`

### Migration Rules
- **Zero Destructive Migrations**: Never call `fallbackToDestructiveMigration()`. Data loss is a P0 regression.
- Every schema change requires:
  1. Incremented `version` in `@Database`.
  2. A dedicated `Migration(M, N)` object adding SQL changes cleanly.
  3. Registration in `.addMigrations(...)` in `AppDatabase.getDatabase`.
  4. Preservation of all existing tables, foreign keys, timestamps, amounts, and statuses.
- **Foreign Keys**: Cascade delete is configured from `Friend` to `TransactionEntity` and `ActivityEntity`.
- **Atomic Operations**: Multi-table updates must run inside `runInTransaction` (`db.withTransaction`).

---

## 14. Backup & Export Safety

### CSV Export (`PhittoosCsvExporter`)
- Format: `transaction_id,friend_name,direction,amount,paid_amount,remaining_amount,status,created_date,due_date,settled_date,note`
- Intended for user viewing and external spreadsheet analysis.
- **Explicit Rule**: CSV exports are **not** restorable backup files. The restore validator explicitly rejects CSV files with an informative error message.

### Versioned JSON Backup (`PhittoosBackupManager`)
- Current Version: `backupVersion: 1`
- File naming: `phittoos-backup-YYYY-MM-DD.json`
- Contains:
  - `metadata`: `backupVersion`, `appVersion`, `createdAt`
  - `preferences`: `userName`, `hasCompletedOnboarding`, `remindersEnabled`
  - `friends`: full list of friends
  - `transactions`: full list of transactions
  - `activities`: full list of activity events
- **Validation Before Restore**: `PhittoosBackupManager.validateBackup` must validate JSON structure, version compatibility, non-blank names, positive amounts, valid directions/statuses, foreign key integrity, and absence of NaN/Infinity.
- **Transactional Restore**: Restoring wipes existing records and inserts backup data atomically inside a transaction.

---

## 15. UI Engineering Rules

### Design Direction: PREMIUM FINANCIAL MINIMALISM
Phittoos UI prioritizes clarity, calm confidence, and high data legibility over flashy ornamentation.

### Semantic Color System
Preserve current financial semantics strictly across all themes:
- **Emerald / Green**: `LENT` direction / money to get back / positive balance
- **Orange / Coral**: `BORROWED` direction / money owed / negative balance
- **Red**: Real overdue states and error validations
- **Amber / Yellow**: Partial repayments and cautionary alerts

### Non-Negotiable UI Protection
UI changes must **NEVER** alter or introduce calculations that affect:
- Friend or dashboard balances
- Repayment calculations
- Settlement calculations
- Mixed-direction bulk settlement safety
- Transaction status transitions
- Due-date normalization and state calculations
- Historical data records
- Room database persistence

### Visual Restraint Guidelines
- Avoid gratuitous glassmorphism, neumorphism, claymorphism, maximalism, heavy blurred surfaces, or ornamental gradient overlays.
- Do not introduce decorative colors that compete with or dilute the financial semantics of Emerald and Orange.

---

## 16. Accessibility & Inclusivity Rules

Whenever modifying or creating UI components, verify:
- **Readable Contrast**: Text and icons must maintain strong, readable contrast against backgrounds across both Light and Dark modes.
- **Touch Target Sizes**: All interactive elements (buttons, chips, list items, icons) must meet a minimum practical touch target size of 48dp x 48dp.
- **Dynamic Text Scaling**: Layouts must adapt gracefully to user-configured font scaling (SP) without clipping, overlap, or truncation.
- **Theme Support**: Seamless readability in both Light and Dark modes without unstyled elements.
- **System Bar Insets**: Respect edge-to-edge window insets (`contentWindowInsets`, status bar, navigation bar).
- **Screen Reader Semantics**: Provide descriptive `contentDescription` on non-decorative images and icon buttons.
- **Reduced Animation**: Gracefully respect system-level reduced or disabled animation settings.
- **Color Independence**: Color must never be the sole indicator of financial state. Always pair color with text labels, status badges, or direction icons.
- **Honest Verification**: Do not claim formal WCAG compliance levels unless explicitly measured using testing tools.

---

## 17. Security & Privacy Engineering Gate

Every code change must be evaluated against these local security and privacy checks:

### Current-App Security Checks
- **AndroidManifest Exported Components**: Every activity, receiver, or provider must declare `android:exported="false"` unless explicitly designed for system interaction.
- **FileProvider Configuration**: `FileProvider` paths in `res/xml/file_paths.xml` must be strictly scoped to temporary cache directories for SAF operations.
- **Permissions**: Preserve least-privilege principles. Do not request permissions that are not strictly necessary. (Currently, only runtime `POST_NOTIFICATIONS` is requested on Android 13+).
- **Exported Financial Data**: Exports are user-initiated only via native SAF file pickers; never write financial data to unsecured shared storage.
- **Sensitive Logging Prohibition**: **NEVER** log sensitive personal or financial information:
  - Friend names
  - Net balances
  - Transaction amounts or repayment amounts
  - Transaction notes
  - Raw JSON backup contents
- **Secrets Management**: Zero hardcoded secrets, tokens, or credentials in source code or resources. Never request, print, commit, or expose signing, keystore, or API secrets.
- **Dependencies**: Scrutinize third-party library additions for security, privacy, and necessity.
- **Network Invariant**: The app must not declare `android.permission.INTERNET` in V1. If networking is ever authorized in a future task, cleartext traffic must remain disabled (`usesCleartextTraffic="false"`).
- **Auto Backup Policy**: Review `fullBackupContent` and `dataExtractionRules` to ensure financial data is handled consistently with user expectations.
- **Compliance Honesty**: Do not make unsubstantiated claims of legal or DPDP compliance solely from technical source checks.

---

## 18. Future / When Applicable Security & Capabilities

> **NOTE: FUTURE / WHEN APPLICABLE ONLY**
> The items below are NOT present in current V1. They apply exclusively when a future task explicitly authorizes shared, cloud, or monetization features.

### When Cloud / Shared Functionality Is Authorized
When a scoped future task explicitly introduces cloud sync or multi-user sharing:
1. **Authentication**: Enforce robust authentication via Jetpack Credential Manager / Google Sign-In.
2. **Server-Side Authorization**: Enforce strict server-side rules (e.g., Firestore rules) validating user ownership of all read/write paths.
3. **IDOR / BOLA Prevention**: Test against Broken Object Level Authorization across friend IDs and transaction IDs.
4. **Token Security**: Store auth tokens securely in Android EncryptedSharedPreferences / Keystore; implement rotation.
5. **Invite & Share Authorization**: One-time, tamper-proof, cryptographically signed share links with explicit expiry.
6. **Replay Protection**: Protect sync payloads with server-verified timestamps and nonces.
7. **Sync Idempotency**: Deduplicate incoming operations to prevent double-charging or repeated repayments.
8. **Deterministic Conflict Strategy**: Define explicit, predictable conflict resolution (e.g., last-write-wins or client-reconciled).
9. **Account Deletion & Data Wipe**: Implement complete, verifiable data deletion complying with store policies.
10. **DPDP & Privacy Architecture**: Ensure consent management, data minimization, and audit trails.
11. **Abuse Mitigation**: Implement client and backend rate limiting to mitigate denial-of-service and credential brute-forcing.

### When Billing / Monetization Is Authorized
When a scoped future task explicitly introduces in-app billing or subscriptions:
1. **Entitlement Validation**: Verify purchase tokens cryptographically via Google Play Billing APIs.
2. **Tampering Protection**: Guard against spoofed purchase payloads and signature manipulation.
3. **Restore Purchases**: Implement a reliable "Restore Purchases" flow.
4. **Duplicate Purchase Handling**: Gracefully handle idempotent pending purchases and already-owned entitlements.
5. **Server Webhook Verification**: Validate backend webhook events if a payment server is involved.

*Rule*: Do NOT implement, reference, or imply any of these features in current V1 documentation or code unless explicitly assigned.

---

## 19. Local-First, Privacy & Permission Guardrails (V1 Current)

### Network & Privacy
- **Zero Network Permissions**: The app does **not** declare `android.permission.INTERNET`.
- **No Cloud Sync / No Analytics / No Ads**: All computation and storage remain strictly on-device in Room SQLite and SharedPreferences.
- **Reliability Scores Private**: Friend reliability ratings (`ReliabilityEngine`) are private to the device owner ("*Only you can see this*") and must never be shared or transmitted.

### Permissions
- **`POST_NOTIFICATIONS`**: The only runtime permission requested (Android 13+) for local overdue reminders.
- **Storage Access Framework (SAF)**: Backup and CSV exports use native system file pickers (`CreateDocument`, `OpenDocument`). No broad storage permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`) are requested.

### Local Data Wipe
- The "Delete everything" setting wipes all tables, activities, and preferences locally, leaving an empty sandbox.

---

## 20. Verification & Testing Standards

### Test Execution Command
Run the JVM unit and Robolectric test suite:
```bash
gradle :app:testDebugUnitTest
```

### Durable Verification Rule
Use the verification environment actually available for the task. Never claim emulator, ADB, USB-device, or physical-device verification unless it was actually performed. If physical verification is required but unavailable, report **NOT VERIFIABLE** rather than **PASS**.

### Key Existing Test Suites
- `PhittoosAccountingEngineTest`: Core accounting invariants, directions, and net balances.
- `PartialRepaymentEngineTest`: Partial repayments, cumulative math, boundary checks.
- `OfflineSettlementFlowTest`: Single and bulk settlements, mixed-direction safety.
- `DueDateOverdueEngineTest`: Due dates, calendar normalization, overdue calculation.
- `TransactionDateBackdateTest`: Backdating, today default, future date rejection.
- `SmartReminderEngineTest`: Overdue reminder qualification, LENT-only rules, anti-spam.
- `ActivityReminderHistoryTest`: Activity logging, reminder stages, and idempotency.
- `PhittoosBackupRestoreTest`: Backup export, validation, and transactional restore.
- `SettingsAndExportTest`: CSV generation, escaping, and preference clearing.
- `ContextualAddAndWrongEntryRecoveryTest`: Edit/delete eligibility and recovery.
- `ReliabilityEngineTest`: Deterministic friend reliability levels.

### Applet Compilation
Always verify clean applet compilation:
```bash
compile_applet
```

---

## 21. Operating Modes & Review Checklist

### Pre-Commit Agent Review Checklist
Before concluding any task, verify:
- [ ] No clean architecture bloat or invented layers introduced.
- [ ] Monetary arithmetic uses safe rounding (2 decimal places) on `Double`.
- [ ] `LENT` and `BORROWED` directions remain distinct and correctly signed.
- [ ] Mixed-direction friend bulk-settlement guard is intact.
- [ ] Net-zero balance does not auto-settle open transactions.
- [ ] Partial repayments decrement only the targeted transaction and accumulate correctly.
- [ ] Overpayment beyond remaining amount is rejected.
- [ ] Due dates handle timezones via noon calendar normalization.
- [ ] Future transaction dates are strictly rejected.
- [ ] Only open, untouched transactions can be edited or deleted.
- [ ] Room migrations exist for any schema alterations (zero destructive migrations).
- [ ] JSON backup and CSV export formats remain consistent.
- [ ] No financial integrity release blockers triggered.
- [ ] UI preserves Premium Financial Minimalism and semantic colors (Emerald, Orange, Red, Amber).
- [ ] Accessibility: Readable contrast, 48dp touch targets, text scaling, Dark mode, system bar insets.
- [ ] Security & Privacy: No sensitive logging, `exported="false"` on components, no secrets, no `INTERNET` permission in V1.
- [ ] Doer / Checker model observed: Diff inspected, edge cases tested, no unrelated changes.
- [ ] `gradle :app:testDebugUnitTest` passes with zero failures.
- [ ] `compile_applet` succeeds.

---

## 22. Stop & Escalation Protocol

### When to Stop
Stop immediately and present results when:
1. The requested feature or documentation is fully implemented.
2. All planned tests pass.
3. Build compilation is verified.
4. No further action is required for the user's prompt.

### When to Report BLOCKED
Report `BLOCKED` with an explanation if:
1. The requested change directly violates permanent product anti-goals (e.g., requests to add banking, group expense splitting, loan/credit lending, or payment processing).
2. Cloud or shared functionality is requested without an explicitly authorized, scoped future task.
3. The requested change requires destructive data loss without a migration path.
4. There is an irreconcilable conflict between prompt specifications and repository accounting invariants.
5. A persistent build or environment error cannot be resolved within 3 iterations.
