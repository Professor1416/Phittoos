# Phittoos V1 financial integrity and engineering rules

## 1. Purpose, scope and source baseline

This rulebook is the engineering contract for maintaining Phittoos V1 without changing the meaning, ownership, or history of the owner's financial records. **MUST / MUST NOT** are acceptance obligations. **SHOULD** requires a documented reason when not followed. Rules do not authorize implementation changes or broaden a task's file scope.

| Baseline | Inspected evidence |
| --- | --- |
| Repository | [Professor1416/Phittoos](https://github.com/Professor1416/Phittoos) |
| Source commit | `b4550fc841a3aaed8ed2c082e09c233554d11f00` |
| Checkout | Branch `work`; HEAD and local `origin/main` match remote `refs/heads/main` observed with `git ls-remote` |
| Inspection date | 2026-10-12, Asia/Calcutta |
| Initial state | Clean tracked/index state; no non-ignored untracked files; RULES.md absent |
| Task scope | Documentation only; no application build/runtime tests executed |

[AGENTS.md](../AGENTS.md) governs agent workflow, financial release blockers and task scope. [ARCHITECTURE.md](ARCHITECTURE.md) describes implementation; [PROJECT_STATE.md](PROJECT_STATE.md) records evidence and risks. This document adds stable acceptance references, not a competing architecture or permission to repair gaps. Do not renumber an existing rule ID when wording changes; record a replacement explicitly if its meaning changes.

Evidence labels:

- **REQUIRED POLICY:** an obligation derived from safety/product requirements, not proof of implementation.
- **CURRENT ENFORCEMENT:** directly observed guard/calculation/transaction path; runtime and coverage limits still apply.
- **REQUIRED / GAP:** the obligation is incomplete, absent, or lacks necessary verification at this baseline.
- **REPORTED / UNVERIFIED:** historical results or a runtime claim not independently established here. **PROPOSED:** future, unimplemented work.

Source and actual assertions govern enforcement claims. Test existence is not a PASS certificate. Historical chat previews are context only; no conversation reader was available. Later commits MUST trigger reinspection of relevant claims. No secrets, real ledger records or user backup contents were accessed.

## 2. Product boundary rules

**PROD-001 — V1 identity.** Phittoos MUST remain a private, single-user, offline-first Android ledger for money lent to/borrowed from local friends. Exact tagline: **Tere mere hisaab… Phittoos!** Owner-entered records and settlement assertions MUST NOT be presented as verified payments, friend consent or externally verified debt.

**PROD-002 — Anti-goals.** V1 MUST NOT add login, linking, cloud sync, mutual confirmation, group splitting, budgeting/expense tracking, loan services, interest, banking, wallets, payment processing or UPI payment execution. Local reminders notify the owner, never the friend. Reliability MUST remain an owner-only LENT-history heuristic, not a public score or creditworthiness guarantee.

**PROD-003 — Future boundary.** Shared V2 functionality remains PROPOSED ONLY and requires separate explicit authorization plus financial/security/privacy gates. Enum values and unused catalog aliases do not authorize a feature. This rulebook MUST NOT select a backend, vendor, identity provider, token-storage technology, conflict algorithm or synchronization design. User-selected SAF exports are a disclosed V1 exception to local storage, not app-controlled sync.

## 3. Financial invariants

Each row is required even where enforcement is partial. Function names refer to the linked repository/entities/DAOs. Test class names are existing coverage, not a claim that all required cases are already tested. Validation means cases required for a relevant future code change.

| ID | Required rule | Current enforcement / gap | Relevant code/tests | Required validation |
| --- | --- | --- | --- | --- |
| FIN-001 | LENT MUST mean receivable from the friend. | CURRENT: OPEN LENT remaining adds to receivable/net. | [Repository][repo] `dashboardTotals`; [Accounting tests][accounting] | Single LENT, multiple friends, partial/full payment signs. |
| FIN-002 | BORROWED MUST mean owner owes the friend. | CURRENT: OPEN BORROWED remaining adds to owed and subtracts from net. | [Repository][repo]; [Accounting tests][accounting] | Single BORROWED and mixed-direction signs. |
| FIN-003 | Only OPEN records MUST contribute outstanding remaining amounts. | CURRENT: balance queries/derived totals filter OPEN, not all statuses. | [Repository][repo]; [FriendDetailViewModel][detailvm]; [Settlement tests][settlement] | OPEN/CONFIRMED/other enum states separately. |
| FIN-004 | Raw paid MUST be null/zero or finite within `[0, amount]`; effective paid/remaining MUST preserve valid amounts. | CURRENT: helpers clamp paid and remaining. REQUIRED / GAP: clamping is not raw-row validation. | [TransactionEntity][transaction]; [Partial tests][partial] | Null/zero/partial/full; corrupt negative, excessive and nonfinite paid values. |
| FIN-005 | Net MUST equal OPEN LENT remaining minus OPEN BORROWED remaining. | CURRENT: repository and detail ViewModel derive sums; no stored net column. | [Repository][repo]; [Accounting tests][accounting] | Per-friend/global net after add/edit/delete/repay/restore. |
| FIN-006 | Zero net MUST NOT imply settled or mutate records. | CURRENT: separate OPEN count, bulk direction guard; no net-triggered settlement. Loading UI remains a GAP. | [Settlement tests][settlement] `testD_zeroNetWithOpenTransactionsIsNotSettled`; [HomeScreen][home] | Equal opposing entries stay OPEN; loading and final summary differ. |
| FIN-007 | Repayments MUST accumulate on one intended transaction only. | CURRENT: ID-based update and rounded cumulative paid. True overlap evidence is a GAP. | [Repository][repo] `recordRepayment`; [Partial tests][partial] | Multi-step payments; unrelated IDs/friends/directions unchanged. |
| FIN-008 | Overpayment MUST be rejected; valid remaining MUST NOT be negative. | CURRENT: rejects above remaining plus `0.0001`, clamps valid remaining. Edge/precision assurance is a GAP. | [Repository][repo]; [TransactionEntity][transaction]; [Partial tests][partial] | Exact remaining, epsilon boundaries, zero/negative/extreme input. |
| FIN-009 | Full repayment MUST make one OPEN→CONFIRMED transition, paid=amount, with settlement time. | CURRENT: rounded full threshold `amount - 0.005`, conditional DAO update. | [Repository][repo]; [TransactionDao][txdao]; [Partial tests][partial] | Threshold boundaries; timestamp, event and full amount correct. |
| FIN-010 | Repeated settlement MUST have no duplicate financial/event effects. | CURRENT: status/affected-row checks precede events; simultaneous calls UNVERIFIED. | [Settlement tests][settlement] `testI_idempotentSettlement`; [Activity tests][activitytests] | Repeats, overlapping calls, event count and unchanged unrelated rows. |
| FIN-011 | Bulk settlement MUST contain one direction among that friend's OPEN records. | CURRENT: empty/mixed sets refused in repository, eligibility reflected in ViewModel. | [Repository][repo]; [Settlement tests][settlement] `testC_mixedDirectionBulkSettlementIsPrevented` | All-LENT/all-BORROWED succeed; mixed/empty refuse without changes. |
| FIN-012 | Ordinary settlement MUST retain transactions and prior financial history. | CURRENT: updates row, appends SETTLED event; no ordinary deletion. | [Repository][repo]; [Activity tests][activitytests] | Pre/post IDs and prior events remain visible. |
| FIN-013 | Eligible deletion, replacement restore and delete-everything MUST have distinct explicit destructive semantics. | CURRENT: different APIs/UI; universal immutable-history claims would be false. | [Repository][repo]; [SettingsViewModel][settingsvm]; [Recovery tests][recovery] | Confirmation/cancel; exact deletion/replacement scope; external exports retained. |
| FIN-014 | Financial events MUST match intended transaction, friend and direction. | CURRENT: repay/settle use fetched record; creation uses submitted input/new ID. GAP: activity transaction ID is not an FK; restore lacks cross-friend check. | [ActivityEntity][activity]; [ActivityDao][actdao]; [Backup manager][backup] | Wrong friend/transaction linkage, missing target and import mismatches rejected. |
| FIN-015 | Invalid numbers MUST NOT silently enter the ledger. | CURRENT: edit/repay finite checks; REQUIRED / GAP: repository creation lacks them, add form lacks explicit infinity check. | [AddTransactionViewModel][addvm]; [Repository][repo] | UI and direct-repository malformed/NaN/infinite/overflow/high-precision cases. |
| FIN-016 | Invalid/duplicate identifiers or foreign references MUST NOT overwrite or orphan history. | CURRENT: friend FKs, unique primary IDs and backup duplicate/reference checks. GAP: REPLACE inserts and logical activity link need scrutiny. | [DAOs][daos]; [Backup tests][backuptests] | Duplicate IDs, absent parents, replace/cascade effects and event association. |
| FIN-017 | Backup records MUST satisfy supported format and financial state consistency before replacement. | CURRENT: structural/enum/amount checks. REQUIRED / GAP: comprehensive status/time/cross-friend/version policy absent. | [Backup manager][backup]; [Backup tests][backuptests] | Corrupt combinations; no mutation on rejection; legacy compatibility explicitly tested. |
| FIN-018 | Multi-row changes, migrations and restore MUST preserve ledger/history consistency or fail safely. | CURRENT: production Room transaction/migration registration. GAP: optional dependency, preference split and incomplete upgrade/failure coverage. | [AppDatabase][db]; [Repository][repo]; [Migration/activity tests][activitytests] | Real transaction dependency, injected failures, upgrades and recovery checks. |

## 4. Monetary input and arithmetic rules

**MON-001 — Validate before mutation.** Monetary transaction/repayment inputs MUST parse unambiguously, be finite and strictly positive. Raw paid totals may be zero/null, but MUST otherwise be finite, nonnegative and within transaction amount. Reject NaN, positive/negative infinity, malformed/empty values and overflow; do not rely on `value <= 0` alone. Validate direct repository and restore paths, not only a disabled UI button. Clamps MUST NOT silently legitimize invalid financial rows. **REQUIRED / GAP:** creation currently lacks these repository checks.

**MON-002 — Precision and range.** New rupee inputs MUST have a defined paise/two-decimal policy and a tested safe range for storage, cumulative arithmetic, formatting and serialization. More than two decimals MUST be rejected unless an explicitly approved normalization policy explains the resulting amount to the owner. Tiny payments MUST NOT be recorded as successful history without the expected paid/remaining change. **REQUIRED / GAP:** `setAmount` allows unbounded digit/dot strings, no length/decimal limit; totals and scaling may exceed safe arithmetic ranges. No maximum amount is invented by this document. Legacy higher-precision records require approved compatibility handling, not silent rounding/deletion.

**MON-003 — Preserve observed arithmetic deliberately.** Current representation is `Double`, not integer paise or BigDecimal. For valid values the observed helpers and repayment arithmetic are:

```text
effectivePaid = (paidAmount or 0).coerceIn(0, amount)
remaining = (amount - effectivePaid).coerceAtLeast(0)
newPaid = round((effectivePaid + repayment) * 100) / 100
reject overpayment if repayment > remaining + 0.0001
fullySettled if newPaid >= amount - 0.005
finalPaid = amount when fullySettled, otherwise min(newPaid, amount)
```

Code changes MUST preserve or explicitly authorize changes to these numerical boundaries, with cases immediately below/at/above them. MUST NOT widen tolerances, float assertions or clamp ranges just to pass tests. The epsilon is compatibility behavior, not permission to manufacture extra repayment. Whole totals currently use direct Double sums; formatted two decimals MUST NOT be described as stored two-decimal precision.

**MON-004 — Serialization and representation.** JSON/CSV outputs MUST represent valid finite amounts honestly; failures MUST be visible rather than producing unusable backups. Round-trip validation MUST compare amounts, statuses, IDs, timestamps and histories, not just formatted text. A move to integer paise/BigDecimal requires separate authorization, migration design, backup compatibility and finance-focused regression. This documentation task changes neither representation nor rounding.

## 5. Transaction creation, editing and deletion rules

**TX-001 — Creation boundary.** Require an existing local friend, explicit LENT/BORROWED direction, valid monetary input, occurrence date not in a future calendar day, and absent or valid due date under the current add policy. Initial state MUST be OPEN, with no repayment/settlement history. Creation and its TRANSACTION_CREATED event MUST agree on amount, direction, target, note and occurrence time. Friend creation is a separate operation; failed transaction save MUST NOT be reported as saved merely because friend insertion succeeded.

**Current enforcement:** repository `addTransaction` trims note and rejects future occurrence dates; it writes row/event in the supplied Room transaction. AddTransactionViewModel checks friend/form dates and positive parse. **REQUIRED / GAP:** repository does not repeat positive/finite amount or due-date checks; friend validity primarily relies on FK enforcement. New workflow callers MUST NOT bypass the required guards.

**TX-002 — Untouched eligibility.** Direct edit/delete MUST require OPEN, raw paid null/zero, zero effective paid, null settledAt, and no existing repayment history. Re-fetch and revalidate at mutation time. For eligible edits identity/friend MUST remain unchanged; owner may correct amount/direction/note/dates, with matching creation-event updates. Financially historical rows MUST NOT be rewritten as a correction shortcut.

| Operation | Required acceptance | Required rejection / scope |
| --- | --- | --- |
| Add | Valid existing friend, number/direction/date; new OPEN record | Invalid input/reference/future occurrence; no partial transaction/event write |
| Edit | Untouched OPEN plus valid replacement values; stable transaction/friend ID | Missing/non-OPEN/repaid/settled/corrupt values; no historical reassignment |
| Delete | Explicit owner confirmation, untouched OPEN | Missing/non-OPEN/repaid/settled; only target and its associated activities removed |
| Ordinary settlement | Valid intended OPEN target and guarded update | No editing/deleting settled history; separate settlement rules apply |

**Current enforcement:** `updateOpenUnpaidTransaction` and `deleteOpenUnpaidTransaction` reject non-OPEN, positive raw/effective paid or non-null settledAt; edit validates finite/positive amount and dates. **REQUIRED / GAP:** these guards do not query repayment events or prove raw paid is exactly null/zero for corrupted data. Tests MUST cover zero-rounded payment-history and corrupt-paid eligibility rather than assuming an aggregate proves untouched history.

**TX-003 — Event consistency.** Edit MUST update its TRANSACTION_CREATED amount/direction/note/createdAt in the same DB transaction. Delete MUST remove target-associated events with the target, without deleting another transaction/friend. Current delete removes all activities for that transaction ID; restore/clear/cascade are separate destructive paths. Do not treat broad DAO deletion/REPLACE as a safe alternative to guarded repository APIs.

## 6. Partial repayment and full settlement rules

**PAY-001 — Apply to one record.** Validate submitted amount before mutation; inside the transaction fetch intended ID, require OPEN, calculate remaining, enforce overpayment rule and use the OPEN-conditional DAO update. Derive friend/direction from the target. Other records MUST be unchanged. UI busy flags help prevent double taps but MUST NOT substitute for repository/database consistency or true concurrent tests.

**PAY-002 — State and history.** Partial payment MUST accumulate rounded paid, stay OPEN and have null settledAt. Full threshold MUST set paid=amount, CONFIRMED and event settlement time exactly once. Full/partial behavior MUST be tested for both directions, multiple payments, exact final amount and rounding boundaries. With synthetic ₹500 and existing paid ₹300, repayment ₹150 leaves paid ₹450/remaining ₹50; another ₹150 is rejected. This is not concurrent evidence.

| Path | Observed event semantics to preserve/test |
| --- | --- |
| Partial `recordRepayment` | PARTIAL_REPAYMENT; submitted repayment amount; target friend/direction; event timestamp |
| Final `recordRepayment` | SETTLED; submitted repayment amount; note `final_repayment`; paid becomes full amount |
| Individual `markTransactionAsPaid` | SETTLED; pre-update remaining amount; paid becomes full amount |

**PAY-003 — Failure/idempotency.** Missing/non-OPEN targets and zero affected rows MUST NOT insert new payment/settlement events. Repeated settlement MUST leave balances, paid, settledAt and event counts unchanged. Do not call an already-settled error a newly successful payment. Atomic DB row/event behavior requires the supplied database and ActivityDao; omitted dependencies in tests do not establish it. Actual simultaneous repayment/settlement/restore races remain **REQUIRED / GAP** verification.

## 7. Bulk settlement rules

**BULK-001 — Selection.** Bulk MUST select only intended friend's OPEN entries and require all LENT or all BORROWED. Empty sets and mixed directions MUST be refused without mutation/events. Net amount, including zero, MUST NOT determine eligibility. Current `settleAllSameDirectionForFriend` enforces the direction guard; ViewModel computes matching eligibility.

**BULK-002 — Commit/event accounting.** All eligible rows MUST become CONFIRMED with paid=amount and the operation settlement time, with one correct remaining-amount SETTLED event per affected entry. Already-confirmed/unrelated rows and previous history MUST remain unchanged. DB update and events MUST commit together with rollback on event/write failure. Current production repository supplies Room; affected-count/preselected-event matching under real overlapping mutations remains UNVERIFIED. Verify rows, amounts and event counts, not just the boolean result.

**BULK-003 — All-settled outcome.** Successful completion MUST be based on no remaining OPEN entries for that friend. Celebration additionally requires active friend-detail screen and a successful settle/full-repayment result; no celebration on zero net, deletion, cancellation or rejection.

## 8. Database and migration rules

**DB-001 — Preserve constraints and identity.** Baseline Room schema is **4**, database `phittoos_database`, entities Friend/friends, TransactionEntity/transactions and ActivityEntity/activity_records. Both child friend IDs are indexed foreign keys with CASCADE delete. Activity transaction ID is a logical reference only. MUST validate event target consistency without pretending an absent FK exists. Inserts currently use REPLACE; any use with existing IDs MUST account for overwrite/cascade/history consequences, not silently replace records.

**DB-002 — Non-destructive upgrades.** Every schema change MUST have an explicit versioned migration, registration, compatible backup handling and retention tests for IDs, amounts, directions, paid/status/timestamps, friend links and events. MUST NOT use `fallbackToDestructiveMigration` for financial data or drop/recreate tables as an implicit “repair”. A failed upgrade MUST be reported, not papered over by losing history.

| Registered baseline migration | Operation | Current coverage / gap |
| --- | --- | --- |
| 1→2 | Add nullable `settled_at` | Complete real v1 fixture/chain not found |
| 2→3 | Create activities and indices with friend FK | ActivityReminderHistoryTest v2 fixture opens through v4 |
| 3→4 | Add nullable `reminder_stage` | SmartReminderEngineTest v3 fixture checks retained data and stage field |

Schema export is disabled; no exported historical schemas were found. Existing fixtures are limited evidence, not universal upgrade safety. Full historical upgrade verification is **REQUIRED / GAP**; this rule does not enable schema export or introduce files in this task.

**DB-003 — Enum integrity.** Serialized enum names MUST remain compatible across updates or receive a reviewed explicit conversion. Unknown values MUST NOT silently reinterpret LENT/BORROWED or OPEN/CONFIRMED/history. **REQUIRED / GAP:** Converters currently defaults unknown non-null direction/status/type to LENT/OPEN/TRANSACTION_CREATED; nullable direction/stage falls back to null. Backup enum validation is stricter. Verify corrupt/legacy strings and migration behavior; do not count fallback as proven financial recovery.

**DB-004 — Actual transaction boundary.** Multi-row financial/event mutations MUST use the production Room dependency. `runInTransaction` falls back to direct execution without `database`; ActivityDao is optional. Tests for atomicity MUST supply both and inject a failure after work begins. SharedPreferences, provider streams, notifications and reactive UI emissions are not part of SQLite commit. Derived totals MUST converge to consistent ledger values; an atomic UI-frame claim requires its own evidence.

## 9. Backup, restore and export rules

**BACK-001 — Validate untrusted input before destruction.** Owner file selection does not make JSON trusted. Check supported version/schema and all required types/sections; nonblank required names; positive unique friend/transaction/activity IDs; existing parent references; activity transaction/friend association; known and supported status/direction/event/stage semantics; finite amount/paid bounds; and occurrence/due/settlement/event timestamp consistency. Reject invalid records without replacing any existing ledger data. Historical due dates may be overdue and MUST NOT be rejected merely for being past today. Legacy missing fields require documented compatibility behavior, not invented settlement times or discarded history.

**Current enforcement:** generated backupVersion=1, required sections, duplicate/reference/name/enum checks, finite positive transaction/activity amounts and paid bounds; CSV/newer-version rejection. **REQUIRED / GAP:** validator does not restrict exactly to documented supported versions, comprehensively validate timestamp/status/payment relationships, or check matching activity/transaction friend. Accepting PENDING_CONFIRMATION/DISPUTED as enum names does not implement their V1 semantics.

**BACK-002 — Financial-state consistency.** New V1 OPEN records MUST have valid unpaid/partial state and no full-settlement timestamp; completed V1 records MUST match full paid amount and settlement history. Where legacy confirmed records lack paid/timestamp fields, an explicit compatibility policy and test fixture MUST justify interpretation; this document does not retroactively delete them. Event IDs, directions and amounts MUST match their intended operation, with explicit handling of the observed rounding tolerances. REQUIRED / GAP: current validation is not this full semantic validator.

**BACK-003 — Explicit replacement and recovery.** Restore MUST display that current local records will be replaced and require owner confirmation. Validate/parse before destructive DB work; use a real Room transaction for clearing/reinserting IDs/rows/events. Rejection or failure before successful commit MUST preserve the old ledger/history and compatible preferences. Interruption after commit MUST have documented recovery to a consistent completed state; MUST NOT report complete restoration while stores disagree. Current repository writes three SharedPreferences values via separate `apply()` inside the DB block: cross-store atomicity is **REQUIRED / GAP**. The manager's database argument alone does not establish the transaction. Invalid-input rejection tests do not prove mid-commit rollback.

**BACK-004 — Consistent export.** Backup output MUST be a coherent snapshot across friends, transactions, events and included preferences. Current export performs separate reads; consistency during writes/restore/clear is **REQUIRED / GAP**. Round trips MUST retain IDs, values, directions, dates, status and prior events. Theme/lastBackupTime are currently not included; documentation MUST disclose omissions, not imply all settings restore.

**BACK-005 — Honest SAF outcome.** A null/failed output stream, write/flush/close exception, cancelled picker or unreadable input MUST NOT produce a success message or advance successful-backup time. Clear pending UI data and report failure/cancellation honestly. **REQUIRED / GAP:** CSV/JSON callbacks currently use nullable output streams followed by success, and JSON timestamp may advance without a write. Required provider tests cover null and throwing streams, partial writes, cancellation and retry; output durability MUST NOT be overstated beyond verified provider behavior.

**BACK-006 — CSV and external plaintext.** CSV MUST be described as a readable non-restorable export, with stable columns, ordering, correct effective paid/remaining and quoting. Quoting does not neutralize spreadsheet formulas; leading formula/control prefixes in user text SHOULD have an explicitly reviewed, tested consumer-safety policy. Current exporter has no formula neutralization. JSON/CSV are plaintext; owners MUST be told selected providers may store copies externally/in cloud. Local erase MUST NOT imply external copies are deleted.

## 10. Dates, reminders and activity rules

**DATE-001 — Distinct timestamps.** Preserve occurrence (`createdDate`), optional due date, payment/event time and `settledAt` separately. Backdated occurrence is allowed; future occurrence calendar days are rejected. Current helper permits later timestamps within today. Normal add/edit due policy rejects past days, but imported historical overdue dates need compatibility validation rather than blanket rejection. Do not overwrite occurrence dates when repaying/settling.

**DATE-002 — Calendar evidence.** Preserve or explicitly review noon normalization in supplied/default timezone and rounded millisecond/86,400,000 day calculation. Test same-day boundaries, timezone changes, DST forward/backward, month/year transitions and backdated records. MUST NOT claim comments prove DST correctness. Settled/zero-remaining entries MUST NOT be actively overdue; date classification MUST NOT alter balances. Activity resume/midnight regrouping does not itself refresh upstream needsAttentionItems; Home/FriendDetail lack a matching midnight timer. Freshness assurance remains **REQUIRED / GAP**.

**REM-001 — Owner-local qualification.** Notify only for OPEN LENT with positive remaining, valid positive due date and reached overdue stage. BORROWED, settled, no-due and zero-remaining cases MUST NOT notify. Stage thresholds are DAY_7/DAY_15/DAY_30; choose highest eligible unsent stage and skip lower stages after a higher stage was sent. A reminder MUST NOT modify paid/status/direction or send to the friend.

**REM-002 — Permission and scheduling honesty.** Check notification availability and API-33 POST_NOTIFICATIONS; denial/disable MUST be a safe non-posting outcome without claiming successful reminder history. A user-facing permission acquisition path MUST be verified before calling fresh-install reminders operational. **REQUIRED / GAP:** permission is declared/checked, but no production runtime request call was found. AGENTS.md's “requested” wording is not implementation proof.

Current periodic work is one day/KEEP; immediate helper is one-time/REPLACE with no production caller found. Worker skips disabled preference and retries exceptions while `runAttemptCount < 3`. Cancellation only targets periodic work; startup scheduling errors are swallowed. MUST NOT promise exact daily delivery; test restart/disable/permission/boot/background behavior on actual supported devices when affected. This runtime retry limit is separate from agent troubleshooting limits.

**REM-003 — Honest delivery history.** Record REMINDER_SENT only after sender success, with target IDs, direction, stage, remaining amount and time. Sequential duplicate checks MUST be preserved; exactly-once claims require crash/overlap evidence. **REQUIRED / GAP:** posting then history persistence are separate; no unique stage constraint/spanning transaction exists. New reliability/anti-spam changes MUST test failure between those operations without fabricating a posted event.

**ACT-001 — Historical and reliability meaning.** Preserve prior activity records except the explicitly authorized destructive paths. Label inferred due information separately from persisted events; merely mapping BECAME_OVERDUE does not establish event generation. Reliability MUST remain LENT-only and private, with conservative missing-data wording; missing delay data MUST NOT be presented as verified on-time payment. Verify changes against ReliabilityEngineTest and activity/reminder suites.

## 11. Security and privacy rules

**SEC-001 — Least privilege and exposure.** Preserve no app-controlled network/cloud/auth in V1 and no INTERNET permission. Review source and merged permissions/components for affected variants; do not infer merged inventory from the source manifest alone. New non-system-facing components MUST be non-exported. Exported launcher MainActivity is intentional; exported inputs MUST NOT create a new unvalidated mutation path. No broad storage/contact/SMS/camera/location permission should be added without a separately authorized product need.

**SEC-002 — Sensitive information.** MUST NOT log/export automatically real friend labels, amounts, balances, notes, raw backup contents, credentials or signing material. Use synthetic test data. No secrets in source/docs/logs; inspect binding names only when necessary. Build/signing credentials belong in secure external configuration. Catalog entries MUST NOT be described as installed services or silently activated as dependencies.

**SEC-003 — Local versus provider control.** App sandbox/private preferences are observed, not application-level encryption or authentication guarantees. Explicit SAF copies can leave the device under provider control; disclosures MUST describe plaintext and retained external copies. Treat import JSON and provider streams as untrusted boundaries. No actual FileProvider/file_paths.xml exists; generic AGENTS.md guidance MUST NOT be cited as proof of such protection.

**SEC-004 — Notifications and platform backup.** Review financial content/visibility on locked devices; no explicit private-visibility override was found. `allowBackup=false` and template extraction rules MUST be reviewed against cloud/device-transfer behavior on supported OS/OEMs before guaranteeing no transfer. No explicit device-transfer section currently exists. Permission/lifecycle or privacy-policy changes require evidence, not blanket assurances of DPDP/WCAG compliance, audited encryption or vulnerability-free software.

## 12. UI, state and accessibility rules

**UI-001 — Financial meaning.** Preserve emerald/green LENT, orange/coral BORROWED, red overdue/errors and amber partial/caution semantics in light/dark/system themes. Pair colors with labels/icons. A cosmetic refactor MUST NOT change money math, eligibility, dates, status transitions or persistence. Keep premium financial minimalism rather than decorative effects that obscure ledger meaning.

**UI-002 — Loading and synchronization.** Loading/unknown state MUST NOT falsely assert all-settled or conceal outstanding entries. Distinguish zero net with OPEN count from no OPEN entries. Await relevant state in UI tests rather than treating Compose idle as proof all Room flows arrived. **REQUIRED / GAP:** Home loading zero state is rendered without a loading gate; earlier assertion failed. Show success only after the intended operation succeeds; reset busy/error state safely after failures, without relying solely on button disablement.

**UI-003 — Accessibility and lifecycle.** Require readable contrast, at least practical 48dp touch targets, text scaling without clipped amounts/actions, meaningful semantics, system insets, both themes and reduced-animation behavior. Retain lifecycle-aware collection and verified resume/pause cleanup. Completion/celebration MUST not imply money transfer or verified friend acknowledgment. Formal compliance claims require measurement; device/TalkBack checks MUST be labelled unrun when unavailable.

## 13. Test and verification requirements

**VER-001 — Risk-based checks.** For a code change, select affected IDs and exercise failure as well as success paths. Assertions MUST inspect target/unrelated rows, paid/remaining/net, IDs/timestamps/directions, event counts and old-state retention on rejection. Supply database/activity dependencies for transaction tests; add real overlap or failure injection when needed instead of mirroring implementation.

| Rule / risk | Required targeted checks | Existing evidence / missing gate |
| --- | --- | --- |
| FIN-001–006 | Unit/repository/Room signed totals, mixed entries, zero-net OPEN, unrelated friends; UI summary after load | Accounting/Settlement tests; Home loading failure remains |
| FIN-007–010, PAY rules | Boundary/cumulative payments, both directions, duplicate/no-op, real overlapping operations, transaction/event rollback | Partial/Activity tests; “concurrent” test is sequential |
| FIN-011, BULK rules | Same direction, mixed/empty refusal, correct affected IDs/event counts; failure during bulk write | OfflineSettlementFlowTest; overlap/commit failure needs evidence |
| FIN-012–014, TX rules | Eligible correction, settled/repaid rejection, corrupt paid/event mismatch, wrong-target isolation | ContextualAddAndWrongEntryRecoveryTest; event-based untouched edge remains |
| FIN-015–017, MON/BACK rules | UI/direct API overflow/nonfinite/high precision; malformed/duplicate/cross-friend/status/date backup; no destruction on rejection | Backup/Accounting tests; full semantic and extreme-input coverage incomplete |
| FIN-018, DB rules | Actual v1/v2/v3 upgrade chains, legacy enums, failure rollback, balances/history preserved | v2/v3 fixtures; no exported/full v1 history; no universal migration PASS |
| BACK-003–006 | Mid-restore/commit/process-death recovery; export during writes; null/throwing SAF provider; CSV formula policy | Backup/Settings tests; invalid-input rejection does not prove mid-commit rollback |
| DATE/REM/ACT rules | Calendar/DST/timezone, stages, disable/denial/restart, post-before-history failure, resume/midnight freshness | DueDate/TransactionDate/Reminder/Activity/Reliability tests; device delivery unverified |
| SEC/UI rules | Merged release permission/component review; locked notification, transfer/export, TalkBack/large text, loading and repeated taps | UI test sources and identity-only instrumentation; physical checks require separate evidence |

**VER-002 — Evidence honesty.** Record exact commit, command, dependency/service context and counts/outcomes. Retained XML from the earlier onboarding run shows **269 tests: 268 passed, 1 failed, 0 errors, 0 skips**; the failure is ExampleRobolectricTest's zero-net OPEN summary assertion. It is historical, lacks source-SHA attestation and is not a current green run. Isolated class PASS cannot replace full regression. No tracked CI workflow was found; external hosted checks and current physical-device QA are unverified. Earlier release-candidate 215-test claims are older reports.

**VER-003 — Code-task completion.** Required targeted checks and full `gradle :app:testDebugUnitTest` regression MUST pass, with the build check required by AGENTS.md (`compile_applet`) actually executed or its unavailable capability reported precisely. Do not silently substitute an unrun/zero-test check. The helper is external to tracked files; native Android `:app:assembleDebug` proves only what its actual result establishes. Device verification cannot be inferred from JVM tests. Preserve intentional tracked screenshots that tests can write. MUST NOT weaken assertions, remove failing tests, inflate tolerances, bypass integrity checks or falsify outcomes to obtain PASS.

**Documentation-only exception:** builds/runtime tests are NOT APPLICABLE under explicit scope. Review all content, IDs, source claims, links/tables, whitespace and authorized-file Git status; this is documentation validation only.

## 14. Severity, release blockers and escalation

| Severity | Required response and evidence |
| --- | --- |
| P0 / NO RELEASE | Reproducible wrong balance/remaining, wrong-target mutation, double financial effect, corrupted/missing history, unsafe migration or partial restore. Stop release even if builds/general tests pass; preserve evidence and require scoped fix/reverification. |
| P1 | Serious unresolved financial validation/consistency or essential-function verification gap, failing required regression, false-success backup path. Do not claim verified safe release; reproduce/triage without inventing corruption or an exploit. Escalate demonstrated financial corruption to P0. |
| P2 | Less critical robustness, evidence/documentation drift or consumer-dependent risk with impact bounded by evidence. Examples include stale channel wording or CSV interpretation requiring review. Escalate if actual impact warrants. |

**ENG-001 — Stop unsafe work.** Report BLOCKED for prohibited product/scope change, destructive loss without safe migration/recovery, irreconcilable invariant conflict or a required check that cannot be completed/diagnosed safely. A discovered gap MUST be recorded; it does not authorize a documentation agent to fix code. Distinguish confirmed failure, unverified risk and missing evidence; identify precise unmet acceptance gates rather than mark all theoretical scenarios P0.

Follow the single Failure-Loop Policy in AGENTS.md: at most eight meaningful attempts; stop after two consecutive attempts with no measurable progress; no unchanged-hypothesis rerun. Do not invent a second environment/build retry policy. Code-task PASS is blocked by unresolved required verification, while documentation can pass if it honestly records the unresolved application state.

## 15. Coding-agent checklist and definition of done

**ENG-002 — Before change.**

- [ ] Verify remote/base SHA, branch, staged/tracked/untracked baseline; preserve unrelated work.
- [ ] Read AGENTS.md and actual affected code/tests; identify applicable rule IDs, allowed files and rejected states.
- [ ] Declare accounting, migration, backup, permissions/privacy and test impact; obtain separate authorization for out-of-scope work.

**ENG-003 — During change.**

- [ ] Follow the AGENTS.md engineering loop; make the smallest coherent authorized diff.
- [ ] Keep financial guards authoritative outside UI; preserve target IDs, event/history consistency and real transaction dependencies.
- [ ] Diagnose failures with observable evidence; never dilute tests/validations or invent a passing build.
- [ ] Record encountered REQUIRED / GAP findings without silently broadening the task.

**ENG-004 — Before completion.**

- [ ] Independently recheck requirements, final diff, applicable invariants, failure cases and actual evidence (Doer/Checker).
- [ ] For code: targeted/full finance regression and required build/device gates have evidenced outcomes; unresolved required checks prevent code PASS.
- [ ] For docs only: content/source/ID/link/table/whitespace checks pass; builds/runtime are explicitly NOT APPLICABLE.
- [ ] Verify final status, staged/untracked files and authorized scope; preserve pre-existing changes separately.
- [ ] No commit/push/merge/publish or subsequent task without current authorization.

The final report MUST use the exact AGENTS.md headings: **STATUS; TASK; ROOT CAUSE; ACCOUNTING IMPACT; CHANGES; VALIDATION; TESTS; DATABASE IMPACT; SECURITY/PRIVACY IMPACT; CHECKER VERDICT; REMAINING RISKS; STOP REASON**. State whether verdict is documentation-only, code verification or release approval; do not conflate them.

## 16. Evidence references and unresolved gaps

Source functions and test classes named above exist at the baseline; test names are evidence pointers, not invented new test coverage. References:

- [PhittoosRepository][repo]: add/edit/delete, repayment/settlement, totals, transaction wrapper, clear/restore/event handling; [PhittoosApplication](../app/src/main/java/com/professor1416/phittoos/PhittoosApplication.kt) supplies production DB/DAOs.
- [Friend](../app/src/main/java/com/professor1416/phittoos/data/model/Friend.kt), [TransactionEntity][transaction], [ActivityEntity][activity], [TransactionDao][txdao], [FriendDao](../app/src/main/java/com/professor1416/phittoos/data/dao/FriendDao.kt), [ActivityDao][actdao], [AppDatabase][db], [Converters](../app/src/main/java/com/professor1416/phittoos/data/db/Converters.kt).
- [AddTransactionViewModel][addvm], [FriendDetailViewModel][detailvm], [HomeViewModel](../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/HomeViewModel.kt), [HomeScreen][home], [SettingsViewModel][settingsvm], [SettingsScreen](../app/src/main/java/com/professor1416/phittoos/ui/screens/settings/SettingsScreen.kt), [UserPreferences](../app/src/main/java/com/professor1416/phittoos/data/preferences/UserPreferences.kt).
- [DueDateHelper](../app/src/main/java/com/professor1416/phittoos/domain/DueDateHelper.kt), [ReliabilityEngine](../app/src/main/java/com/professor1416/phittoos/domain/ReliabilityEngine.kt), [SmartReminderEngine](../app/src/main/java/com/professor1416/phittoos/reminder/SmartReminderEngine.kt), [Worker](../app/src/main/java/com/professor1416/phittoos/reminder/SmartReminderWorker.kt), [Scheduler](../app/src/main/java/com/professor1416/phittoos/reminder/SmartReminderScheduler.kt), [NotificationHelper](../app/src/main/java/com/professor1416/phittoos/reminder/ReminderNotificationHelper.kt).
- [Backup manager][backup], [CSV exporter](../app/src/main/java/com/professor1416/phittoos/export/PhittoosCsvExporter.kt), [AndroidManifest](../app/src/main/AndroidManifest.xml), [backup rules](../app/src/main/res/xml/backup_rules.xml), [extraction rules](../app/src/main/res/xml/data_extraction_rules.xml), [app build](../app/build.gradle.kts), [version catalog](../gradle/libs.versions.toml).
- [Accounting tests][accounting], [Partial tests][partial], [Settlement tests][settlement], [Recovery tests][recovery], [Backup tests][backuptests], [Activity/migration tests][activitytests], [Reminder/migration tests](../app/src/test/java/com/professor1416/phittoos/SmartReminderEngineTest.kt), [Date tests](../app/src/test/java/com/professor1416/phittoos/DueDateOverdueEngineTest.kt), [Transaction-date tests](../app/src/test/java/com/professor1416/phittoos/TransactionDateBackdateTest.kt), [Settings/export tests](../app/src/test/java/com/professor1416/phittoos/SettingsAndExportTest.kt), [Reliability tests](../app/src/test/java/com/professor1416/phittoos/ReliabilityEngineTest.kt), [instrumented identity test](../app/src/androidTest/java/com/professor1416/phittoos/ExampleInstrumentedTest.kt).

Unresolved baseline gaps: creation finite/range/precision guards; clamped raw values and aggregate-only untouched checks; true concurrency; full backup semantic/version validation; Room/preferences recovery; separate export reads; nullable SAF success; reminder delivery/history atomicity; runtime permission acquisition; dashboard loading; converter fallbacks and complete historical upgrades. These remain REQUIRED / GAP or UNVERIFIED, not a roadmap or implementation authorization.

Document discrepancies retained: actual channel is `repayment_reminders`, not AGENTS.md's `overdue_reminders`; no FileProvider/file_paths.xml exists; declaration/check is not a runtime permission request; strict backup/immutable-history wording must be interpreted as policy with the destructive/validation limits described here. Existing docs remain untouched. No new app test/build run or release certification accompanies this rulebook.

[repo]: ../app/src/main/java/com/professor1416/phittoos/data/repository/PhittoosRepository.kt
[transaction]: ../app/src/main/java/com/professor1416/phittoos/data/model/TransactionEntity.kt
[activity]: ../app/src/main/java/com/professor1416/phittoos/data/model/ActivityEntity.kt
[txdao]: ../app/src/main/java/com/professor1416/phittoos/data/dao/TransactionDao.kt
[actdao]: ../app/src/main/java/com/professor1416/phittoos/data/dao/ActivityDao.kt
[daos]: ../app/src/main/java/com/professor1416/phittoos/data/dao
[db]: ../app/src/main/java/com/professor1416/phittoos/data/db/AppDatabase.kt
[addvm]: ../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/AddTransactionViewModel.kt
[detailvm]: ../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/FriendDetailViewModel.kt
[settingsvm]: ../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/SettingsViewModel.kt
[home]: ../app/src/main/java/com/professor1416/phittoos/ui/screens/home/HomeScreen.kt
[backup]: ../app/src/main/java/com/professor1416/phittoos/export/PhittoosBackupManager.kt
[accounting]: ../app/src/test/java/com/professor1416/phittoos/PhittoosAccountingEngineTest.kt
[partial]: ../app/src/test/java/com/professor1416/phittoos/PartialRepaymentEngineTest.kt
[settlement]: ../app/src/test/java/com/professor1416/phittoos/OfflineSettlementFlowTest.kt
[recovery]: ../app/src/test/java/com/professor1416/phittoos/ContextualAddAndWrongEntryRecoveryTest.kt
[backuptests]: ../app/src/test/java/com/professor1416/phittoos/PhittoosBackupRestoreTest.kt
[activitytests]: ../app/src/test/java/com/professor1416/phittoos/ActivityReminderHistoryTest.kt
