# Phittoos Agent Engineering Guide

## 1. Purpose

This document is the operating contract and engineering standard for AI coding agents working on the Phittoos codebase.

Phittoos is a private personal money ledger where correctness is paramount. Accidental accounting drift, corrupted ledger history, or phantom balances destroy user trust.

### Operating Mandates
- **Inspection Precedes Implementation**: Every task must begin by inspecting actual repository code, models, DAOs, and existing tests. Never assume architecture or features based on documentation alone.
- **Financial Correctness Over UI Appearance**: A visually appealing screen is meaningless if accounting invariants, state transitions, or persistence rules are violated.
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

### Primary Use Case
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

### Explicit Anti-Goals (What Phittoos Is NOT)
- **NOT** a group expense splitter (do NOT turn Phittoos into Splitwise).
- **NOT** a personal budgeting or expense tracker (no categories, salary tracking, or spending analytics).
- **NOT** a bank, digital wallet, or neo-bank.
- **NOT** a credit lender or interest calculator (zero interest, zero loan terms).
- **NOT** a payment processor, payment gateway, or UPI transaction app.
- **NOT** a cloud-synced multi-user social platform.

**Boundary Rule**: Every proposed change must improve one-to-one hisaab recording, repayment, settlement, or history clarity. If a feature drifts into group splitting, budgeting, or banking, backlog it immediately.

---

## 3. Repository Source of Truth

Agents must evaluate evidence strictly in this order of precedence:
1. **Actual current source code** (`app/src/main/java/...`)
2. **Current automated tests** (`app/src/test/java/...`)
3. **Current Room database schema and migrations** (`data/db/AppDatabase.kt`)
4. **Existing repository documentation** (`docs/...`)
5. **Prompt / task instructions**

### Conflict Resolution Protocol
- If task documentation claims a feature exists or works in a certain way, but the source code proves otherwise, **repository truth wins**.
- Report the mismatch explicitly rather than silently writing conflicting code or inventing non-existent abstractions.
- Never claim a feature exists solely because documentation references it.

---

## 4. Required Engineering Loop

Every task on this repository must follow this structured engineering loop:

```
INSPECT
  → ROOT CAUSE / REQUIREMENTS
  → PLAN
  → IMPLEMENT MINIMALLY
  → TARGETED TEST
  → FULL REGRESSION (gradle :app:testDebugUnitTest)
  → CHECKER REVIEW
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

**No Unrelated Refactoring**: Do not reformat unrelated files, clean up unrelated methods, or change architectural patterns outside the scope of the assigned task.

---

## 5. Architecture Boundaries

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

## 6. Protected Accounting Invariants

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

## 7. Money Input & Representation

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

## 8. Partial Repayment Rules

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

## 9. Settlement Rules

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

## 10. Date & Due-Date Rules

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

## 11. Edit / Delete Rules

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

## 12. Room / Persistence Safety

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

## 13. Backup & Export Safety

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

## 14. Local-First, Privacy & Permission Guardrails

### Network & Privacy
- **Zero Network Permissions**: The app does **not** declare `android.permission.INTERNET`.
- **No Cloud Sync / No Analytics / No Ads**: All computation and storage remain strictly on-device in Room SQLite and SharedPreferences.
- **Reliability Scores Private**: Friend reliability ratings (`ReliabilityEngine`) are private to the device owner ("*Only you can see this*") and must never be shared or sent to anyone.

### Permissions
- **`POST_NOTIFICATIONS`**: The only runtime permission requested (Android 13+) for local overdue reminders.
- **Storage Access Framework (SAF)**: Backup and CSV exports use native system file pickers (`CreateDocument`, `OpenDocument`). No broad storage permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`) are requested.

### Local Data Wipe
- The "Delete everything" setting wipes all tables, activities, and preferences locally, leaving an empty sandbox.

---

## 15. Verification & Testing Standards

### Test Execution Command
Run the JVM unit and Robolectric test suite:
```bash
gradle :app:testDebugUnitTest
```

### Prohibited Test Patterns
- **NO Android Emulator or ADB**: The execution environment does not have an emulator or ADB. Do not run instrumented tests in `androidTest/`.
- Use **Robolectric** for Android component and lifecycle testing.

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

## 16. Operating Modes & Review Checklist

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
- [ ] No `INTERNET` permission added to `AndroidManifest.xml`.
- [ ] `gradle :app:testDebugUnitTest` passes with zero failures.
- [ ] `compile_applet` succeeds.

---

## 17. Stop & Escalation Protocol

### When to Stop
Stop immediately and present results when:
1. The requested feature or documentation is fully implemented.
2. All planned tests pass.
3. Build compilation is verified.
4. No further action is required for the user's prompt.

### When to Report BLOCKED
Report `BLOCKED` with an explanation if:
1. The requested change directly violates core product identity (e.g., requests to add UPI payments, group splitting, or cloud database sync).
2. The requested change requires destructive data loss without a migration path.
3. There is an irreconcilable conflict between prompt specifications and repository accounting invariants.
4. A persistent build or environment error cannot be resolved within 3 iterations.
