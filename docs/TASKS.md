# Phittoos V1 engineering backlog and release gates

## 1. Document control and baseline

| Field | Inspected baseline |
| --- | --- |
| Repository | Professor1416/Phittoos |
| Branch / source SHA | `work` / `42c91f87b433a5b44d3cca7c70fecb8d87cc13dd` |
| Remote main | Same SHA observed with `git ls-remote origin refs/heads/main` |
| Inspection date | 2026-10-12 UTC |
| Initial state | Clean staged/unstaged/untracked state; TASKS.md absent |
| Scope | Source/test/document/history and retained-artifact inspection; only this Markdown created |

This backlog defines future independently authorized work, not implementation permission. No build/test/device/store action executed here: **NOT RUN**. Prior documents are snapshots; source governs implementation. Preserve stable IDs, record changed acceptance/dependencies/status and evidence by revision. Workstreams are roles, not assigned people; no dates or estimates are invented.

Status and priority definitions are in §4. Source-confirmed gaps, retained failures, unverified runtime scenarios and product policy are distinct evidence types. A code path/test name is not completion evidence. Historical conversations were not retrieved (no available conversation reader); supplied previews are context only. No personal ledger or signing secrets were inspected.

## 2. Product scope and release objective

**Phittoos — Tere mere hisaab… Phittoos!** Private, offline-first, single-user Android ledger of money lent to/borrowed from local friends. Target outcome is a trustworthy future Google Play candidate only after all applicable safety, QA, privacy and store gates clear.

V1 has no accounts/login, linking, mutual confirmation, friend verification, cloud sync or borrower notifications. Group splitting, personal budgeting, banks/wallets/lending/interest and payment/UPI processing are excluded. Local reliability is owner-only, not credit scoring. Settlement is an owner assertion, not verified payment. Explicit plaintext SAF exports may leave the device; this is not automatic sync. V2 discovery is isolated in §16.

## 3. Current implementation snapshot

Source contains LENT/BORROWED capture, friend balances/net/search, multiple transactions, partial/final repayments, individual/same-direction bulk settlement with mixed-direction refusal, due/overdue, owner reminders/history/reliability, celebration, profile/themes, JSON replacement restore and CSV. Six screen areas exist. This backlog preserves these implementations and targets gaps or missing acceptance; it does not request reimplementation of each feature.

Observed technical boundaries: Double amounts, Room schema 4 with registered 1→2→3→4 and no destructive fallback, optional repository DB/activity dependencies supplied in production, private preferences written separately, SAF streams and approximate WorkManager scheduling. Source presence is not a release PASS.

Retained local XML contains **269 tests, 268 passed, 1 failed, 0 errors/skips**; dashboard zero-net OPEN assertion failed. No fresh run was performed. RELEASE_CANDIDATE.md reports September 19 unsigned APK/AAB and 215 passes with SHA N/A; those reports do not verify current signatures/installability/Play readiness. No tracked CI workflow, full v1 upgrade fixture or current commit-linked physical QA was found. Only wrapper distribution properties are tracked, not gradlew scripts/JAR; do not blindly prescribe ./gradlew.

## 4. Prioritization and task status rules

| Priority | Evidence / release meaning |
| --- | --- |
| P0 | Reproduced financial corruption, wrong-target/duplicate money effect, unsafe migration/partial restore or financial-history loss: NO RELEASE |
| P1 | Serious source-backed integrity/security gap, unsafe control flow, major correctness failure or missing critical gate; not automatically a proven exploit |
| P2 | Important UX/privacy/accessibility/robustness/device gap; release impact depends on required behavior/actual effect |
| P3 | Optional non-safety polish/maintenance; no mandatory item invented to fill this category |
| V2 | Future exploration; never a V1 gate or executable implementation task |

Use exactly these statuses: **DONE** — implementation and required evidence verified at named baseline; **OPEN** — work incomplete; **BLOCKED** — named prerequisite prevents proceeding; **VERIFY** — code may exist but acceptance proof missing; **DEFERRED** — explicitly outside current release scope; **PROPOSED V2** — concept awaiting product approval. Dependencies indicate order, not that every dependent task is currently BLOCKED; read-only investigation may proceed. Only final decision is BLOCKED on missing mandatory proof.

No master task is DONE. Completion of prior documentation is not completion of application/release gates. YES means mandatory before release; CONDITIONAL requires evidence-based impact/disposition, not permission to ignore; NO means not release-blocking. Priority/status changes require evidence. Reproducing an integrity failure promotes its task to P0 and halts release; no new P0 reproduction occurred in this documentation task.

## 5. Master backlog table

Dependencies below use numeric suffixes of PHI-V1 IDs; ranges include all stated IDs. “As applicable” dependencies must be recorded precisely for the candidate. Each row has its acceptance contract in §13.

| ID | Action | Priority | Status | Owner/workstream | Source evidence | Dependency | Release blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PHI-V1-001 | Establish reproducible build baseline | P1 | VERIFY | QA | [app build][build], [wrapper properties][wrapper]; executable/JAR absent | — | YES |
| PHI-V1-002 | Diagnose retained dashboard regression | P1 | VERIFY | QA | [dashboard test][dashboardtest], [Home][home]; retained zero-net assertion failed | 001 | YES |
| PHI-V1-003 | Enforce authoritative finite positive creation amounts | P1 | OPEN | Accounting | [repository][repo] addTransaction lacks guards; [Add VM][addvm]; FIN-015, MON-001, SEC-V1-001, FR-005 | 001 | YES |
| PHI-V1-004 | Define safe monetary range and precision contract | P1 | OPEN | Accounting | [transaction][tx], [Add VM][addvm], [formatters][fmt]; unbounded precision/Double; MON-002–004, FR-005 | 003 | YES |
| PHI-V1-005 | Verify target isolation and event association | P1 | VERIFY | Accounting | [repository][repo], [ActivityEntity][act], [recovery tests][recovery]; FIN-014/016, SEC-V1-003/005, FR-008/012 | 001 | YES |
| PHI-V1-006 | Exercise actual overlapping financial mutations | P1 | VERIFY | Accounting | [partial tests][partial] testK is sequential; optional transaction dependency in [repository][repo]; FIN-007/018, SEC-V1-003 | 005,008 | YES |
| PHI-V1-007 | Verify replay and repeated settlement idempotency | P1 | VERIFY | Accounting | [settlement tests][settlement] testI; ID/OPEN DAO guards [TransactionDao][txdao]; FIN-010, FR-009 | 005 | YES |
| PHI-V1-008 | Verify repayment rounding and threshold boundaries | P1 | VERIFY | Accounting | [repository][repo] remaining+0.0001 and amount-0.005; [partial tests][partial]; FIN-008/009, MON-003, FR-008 | 004 | YES |
| PHI-V1-009 | Protect correction eligibility for legacy paid/history states | P1 | OPEN | Accounting | [repository][repo] edit/delete inspect positive aggregate, not events; [converters][conv]; TX-002, FIN-004/012, SEC-V1-005 | 005,008 | YES |
| PHI-V1-010 | Verify mixed-direction and zero-net regressions | P1 | VERIFY | Accounting | [settlement tests][settlement] testC/testD; [accounting tests][accounting]; FIN-001–006/011, FR-004/011 | 001 | YES |
| PHI-V1-011 | Validate backup cross-record financial semantics | P1 | OPEN | Persistence | [backup][backup] checks IDs/enums/bounds but not full consistency; [backup tests][backuptest]; FIN-014/017, SEC-V1-007, FR-020 | 004,005 | YES |
| PHI-V1-012 | Define supported backup-version handling | P1 | OPEN | Persistence | [backup][backup] rejects -1 or >1 rather than exact supported set; BACK-001, FIN-017 | 011 | YES |
| PHI-V1-013 | Bound selected backup input resources | P2 | OPEN | Persistence | [Settings][settings] readBytes; [backup][backup] JSON parsing; SEC-V1-008 | 011 | CONDITIONAL |
| PHI-V1-014 | Verify database replacement rollback under failure | P1 | VERIFY | Persistence | [repository][repo] restoreValidatedData; [backup tests][backuptest] invalid reference test rejects before writes; FIN-018, BACK-003 | 011,012 | YES |
| PHI-V1-015 | Establish recoverable ledger/preference restore consistency | P1 | OPEN | Persistence | [repository][repo] preference setters within Room block; [preferences][prefs] apply is separate; SEC-V1-009, FR-020 | 014 | YES |
| PHI-V1-016 | Make exports a coherent ledger snapshot | P1 | OPEN | Persistence | [backup][backup], [Settings VM][settingsvm] separate reads; BACK-004, SEC-V1-010, FR-019/021 | 005,011 | YES |
| PHI-V1-017 | Reject null or failed SAF output before success | P1 | OPEN | Android UI | [Settings][settings] nullable openOutputStream followed by success/timestamp; BACK-005, SEC-V1-010, FR-019/021 | 001 | YES |
| PHI-V1-018 | Decide and verify CSV spreadsheet safety | P2 | VERIFY | Security | [CSV][csv] quotes separators, no formula-prefix neutralization; BACK-006, SEC-V1-011, FR-021 | 016 | CONDITIONAL |
| PHI-V1-019 | Verify complete historical Room upgrades | P1 | VERIFY | Persistence | [database][db] v4 1→2→3→4 exportSchema=false; [activity tests][activitytest], [reminder tests][remindertest] v2/v3 only; FIN-018 | 001,005 | YES |
| PHI-V1-020 | Prevent silent corrupt-enum financial reinterpretation | P1 | OPEN | Persistence | [converters][conv] defaults direction/status/type; DB-003, FIN-016/018, SEC-V1-006 | 019 | YES |
| PHI-V1-021 | Gate Home summary on loaded ledger state | P1 | OPEN | Android UI | [Home][home], [Home VM][homevm] initial zero/isLoading; FR-006/024, FIN-006, DESIGN §8/14 | 002,010 | YES |
| PHI-V1-022 | Verify search and distinct empty states | P2 | VERIFY | Android UI | [Home][home] ledger/search empties; [empty visual tests][emptytest]; FR-003/024 | 021 | CONDITIONAL |
| PHI-V1-023 | Recover Add save/error/busy state honestly | P1 | OPEN | Android UI | [Add VM][addvm] save lacks universal exception/finally cleanup; FR-005/024, DESIGN §9/14 | 003,004 | YES |
| PHI-V1-024 | Verify repayment duplicate-tap and error feedback | P2 | VERIFY | Android UI | [Friend detail][detail], [Detail VM][detailvm] in-flight flags; [detail accessibility tests][detailtest]; FR-008/009 | 006,007 | CONDITIONAL |
| PHI-V1-025 | Keep due/attention state fresh across calendar changes | P2 | OPEN | Android UI | [dates][datesource], [Activity VM][activityvm] regroup only; Home/Detail no matching timer; DATE rules, FR-015 | 001 | CONDITIONAL |
| PHI-V1-026 | Provide notification permission acquisition and denial UX | P2 | OPEN | Android UI | [notification][notification] declares/checks, no production request; [Settings][settings]; REM-002, SEC-V1-013, FR-016 | 001 | YES |
| PHI-V1-027 | Verify owner notification disclosure and routing | P2 | VERIFY | Privacy | [notification][notification] friend/amount, immutable intent, no private visibility override; [MainActivity][main]; SEC-V1-012/013 | 026 | CONDITIONAL |
| PHI-V1-028 | Verify reminder posting/history crash and overlap behavior | P2 | VERIFY | QA | [engine][engine], [repository][repo] recordReminderSent post then log; no unique stage; REM-003, SEC-V1-014 | 001,026 | CONDITIONAL |
| PHI-V1-029 | Verify themes and accessible spoken interactions | P2 | VERIFY | Android UI | [theme][theme], [colors][colors], accessibility test sources; FR-022, UI-001/003, DESIGN §4/15 | 021,023,026 | CONDITIONAL |
| PHI-V1-030 | Verify responsive amounts, names and insets | P2 | VERIFY | Android UI | [responsive tests][responsive], [inset tests][insettest]; DESIGN §6/15, FR-024 | 029 | CONDITIONAL |
| PHI-V1-031 | Verify launcher, splash and monochrome on devices | P2 | VERIFY | Android UI | [brand][brand], [splash][splash], [DESIGN](DESIGN.md) static previews only | 039 | CONDITIONAL |
| PHI-V1-032 | Verify celebration eligibility and accessible motion | P2 | VERIFY | Android UI | [celebration][celebration], [celebration tests][celebrationtest]; FR-018, BULK-003, DESIGN §16 | 007,024 | CONDITIONAL |
| PHI-V1-033 | Audit merged release identity, permissions and components | P1 | VERIFY | Security | [manifest][manifest], [app build][build]; debug artifact only; SEC-V1-012, FR-004/016 | 001 | YES |
| PHI-V1-034 | Verify Android backup and device-transfer boundaries | P2 | VERIFY | Privacy | [backup rules][backuprules], [extraction rules][extraction], allowBackup=false; SEC-V1-015 | 033,039 | CONDITIONAL |
| PHI-V1-035 | Align privacy, Play and agent disclosures with behavior | P2 | OPEN | Privacy | [PRIVACY](PRIVACY.md), [PLAY_DECLARATIONS](PLAY_DECLARATIONS.md), [AGENTS](../AGENTS.md); SEC-V1-011/012/015 | 017,026,027,033,034 | YES |
| PHI-V1-036 | Verify local deletion and external-copy boundaries | P2 | VERIFY | Privacy | [Settings VM][settingsvm], [preferences][prefs], cascade DAOs; FIN-013, SEC-V1-015, FR-023 | 015,035 | CONDITIONAL |
| PHI-V1-037 | Review resolved dependencies and signing supply chain | P1 | VERIFY | Security | [app build][build], [catalog][catalog], [LICENSES](LICENSES.md); SEC-V1-016; unused aliases not services | 001,033 | YES |
| PHI-V1-038 | Record current full regression and required build evidence | P1 | VERIFY | QA | [AGENTS](../AGENTS.md), [PROJECT_STATE](PROJECT_STATE.md); retained 268/269 not green | 002–028 as applicable fixes/finance gates | YES |
| PHI-V1-039 | Run representative instrumented and offline device smoke | P1 | VERIFY | QA | [instrumented test][instrumented] checks identity only; FR core journeys | 001,021,023,026 | YES |
| PHI-V1-040 | Check current Play SDK and artifact requirements | P1 | VERIFY | Release | [app build][build] min24/target36/version1; [PLAY_DECLARATIONS](PLAY_DECLARATIONS.md) | 033 | YES |
| PHI-V1-041 | Build and verify production-signed release artifacts | P1 | VERIFY | Release | [app build][build] conditional release signing; [RELEASE_CANDIDATE](RELEASE_CANDIDATE.md) old unsigned reports | 037,038,040 | YES |
| PHI-V1-042 | Verify signed install, upgrade and backup recovery | P1 | VERIFY | Release | [RELEASE_CANDIDATE](RELEASE_CANDIDATE.md), [database][db], [backup][backup]; historical phone QA unbound | 014,015,017,019,041 | YES |
| PHI-V1-043 | Verify public support and privacy URLs | P2 | VERIFY | Release | [PRIVACY](PRIVACY.md), docs/index.html and docs/privacy/index.html; hosting unverified | 035 | YES |
| PHI-V1-044 | Review Play declarations against final candidate | P1 | VERIFY | Privacy | [PLAY_DECLARATIONS](PLAY_DECLARATIONS.md), [PRD](PRD.md), [SECURITY](SECURITY.md) | 035,040,041,043 | YES |
| PHI-V1-045 | Prepare truthful store listing and candidate screenshots | P2 | OPEN | Release | [brand][brand], [DESIGN](DESIGN.md), [PRD](PRD.md); no verified release screenshot set | 030,031,041 | YES |
| PHI-V1-046 | Verify applicable closed-testing and Console prerequisites | P1 | VERIFY | Release | [RELEASE_CANDIDATE](RELEASE_CANDIDATE.md) owner checklist; account/program applicability unknown | 040,041,044 | YES |
| PHI-V1-047 | Define privacy-respecting crash and release recovery procedure | P2 | OPEN | Release | [SECURITY](SECURITY.md), [RULES](RULES.md) no current release plan; no telemetry assumed | 015,042 | YES |
| PHI-V1-048 | Record final release go/no-go decision | P1 | BLOCKED | Release | [AGENTS](../AGENTS.md), [RULES](RULES.md), [SECURITY](SECURITY.md), this gate register | All other YES gates; assessed CONDITIONAL gates | YES |

## 6. Financial integrity workstream

003–004 establish authoritative finite/positive/range/precision contracts across create/edit/pay without mandating a Double-to-paise migration. Such representation change needs separate approval, migration/backup design and regression. 005–010 preserve existing target, direction, cumulative payment, repeated-settlement, tolerance and history behavior while filling actual-overlap/corrupt-input gaps. Financial assertions must inspect unrelated rows and events, not merely returned booleans.

Trace: RULES FIN-001–018 and MON/TX/PAY/BULK; PRD FR-004–014; SEC-V1-001–006. Verify with production-equivalent DB/activity dependencies: repository constructor omission does not prove atomicity. The test named concurrent is sequential, not stress evidence.

## 7. Backup, restore and data integrity workstream

011–020 separate semantic/version/size validation, DB failure rollback, cross-store recovery, snapshot generation, provider completion, CSV consumers and legacy upgrade/converter behavior. Invalid-input rejection before replacement is not proof of mid-commit rollback. Unknown enum defaults and cross-friend activity links are source observations; no malicious import exploit was run.

Trace: FIN-014–018, BACK/DB rules, FR-019–021, SEC-V1-006–011. Restore replaces existing data; confirmation does not make it atomic with preferences. JSON supports a subset of preferences; CSV is not restorable. Profile skip/blank-name compatibility belongs to 011. Do not erase legacy high precision or history to make tests pass.

## 8. Android UI and experience workstream

021/023/025/026 are scoped behavior fixes; 022/024/027–032 are verification objectives. Diagnose 002 before assuming a dashboard assertion is harmless. Preserve gross LENT/BORROWED and zero-net OPEN distinctions, correct target/dialog context, loaded/empty/error truth and real persistence completion.

DESIGN.md records fixed light/dark palettes (System follows OS; unused dynamicColor argument), local dimensions, actual component names and static logo previews; it defines no DS IDs. QA therefore references its sections instead of inventing design identifiers. Full-screen screenshot decoding was unavailable, and TalkBack/masks/RTL/physical motion remain unverified. No reskin or new assets authorized here.

## 9. Security and privacy workstream

SEC-V1-001–011 map to 003–020; SEC-V1-012/013 to 026/027/033; SEC-V1-014 to 028; SEC-V1-015 to 034–036; SEC-V1-016 to 037/041. SEC-V1-017 is future/not applicable in V1 and belongs only to §16.

The source manifest is not the merged release manifest. Exported launcher/system components require variant/permission review rather than blanket exploit labels. SAF external plaintext, OS transfer, lock-screen content, local versus external deletion and inaccurate policy/source wording have distinct gates. No app-controlled V1 network/auth backend is invented. Dependency/license/signing reviews require actual resolved evidence, not unused catalog declarations or old clean-audit claims.

## 10. Testing and quality gates

001/002 establish runner/build/failure baseline, 005–010/014/019 provide targeted financial/DB evidence, 011–018 adversarial import/export/recovery, 024–032 UI/device tests, 038 full regression and 039 device smoke. Final release evidence uses candidate artifacts from 041/042.

Future code-task commands include `gradle :app:testDebugUnitTest` and verified `gradle :app:assembleDebug`; selected suites may use `--tests` with actual class patterns. These commands are **NOT RUN here**. AGENTS requires compile_applet; its external capability is not tracked—record absence precisely rather than claiming PASS or silently replacing its meaning. Verify runner/JDK/SDK before execution. Do not install tools/repair wrapper files as part of this documentation task.

Every result must include source SHA, exact command/working directory, environment/tool versions, exit code, executed/pass/fail/skip counts, target test IDs and artifact/log path/hash where relevant. Manual checks add device/API/OEM/settings and before/after observations. No zero-test/isolated rerun/older report substitutes for current full regression. Preserve tracked screenshot/user changes; no weakened assertions or enlarged money tolerances. Document-only validation is not an application regression.

## 11. Google Play release readiness

033/040 review identity/version/manifest and current applicable requirements; 037/041 securely validate signing/dependencies/candidate; 042 proves signed device install/upgrade/recovery. 043 checks live public support/privacy URLs; 044/046 check actual Console declarations/account/testing prerequisites; 045 captures truthful listing assets; 047 defines operational recovery; 048 records go/no-go.

Current app declaration is applicationId `com.professor1416.phittoos`, versionName 1.0.0/versionCode 1, min24/target36. Preserve locked identity; verify current Play rules at release time, never infer eligibility from target alone. Account, production key and Console state are UNKNOWN, not asserted missing or completed. The old report's different prior applicationId is not in-place upgrade compatible merely because JSON may be.

No artifact created/signed/installed, URL fetched, Console submitted, store assets generated or release published in this task. Publication always requires separate explicit authorization, even after 048 review.

## 12. Dependencies and execution order

A. Establish reproducible baseline/build and diagnose failing checks (001–002).

B. Establish money contract, target isolation, tolerance, overlap and history proof (003–010).

C. Clear semantic import, rollback/recovery, export/provider and migration boundaries (011–020).

D. Fix/verify loaded UI, save feedback, dates, reminders and disclosures (021–037). Safe independent verification can run in parallel after shared prerequisites; overlapping edits need coordinated ownership and separate authorization.

E. Record full current QA/device evidence (038–039); complete visual/manual checks against candidate, not stale artifacts.

F. Validate signed/store/device artifacts, declarations and operations (040–047); independent final decision 048. A later code change invalidates relevant downstream proof; rebuild/retest affected gates. No dates/effort estimates or automatic task execution are assigned.

## 13. Task detail and acceptance contract

Each entry is one independently reviewable change or verification objective. It does not authorize edits. If investigation requires a separate substantive repair, create a separately approved child task while retaining this ID as evidence/gate owner. Global DoD: minimal authorized diff, independent checker, applicable targeted/current regression/build and scope checks under AGENTS; explicit unrun outcomes block claimed completion.

**Completion evidence required for every entry:** named baseline/candidate, exact verification scenario/command, before/after or rejection assertions, result/counts/log or reviewed document/artifact reference, independent checker verdict and unresolved limits. Keep synthetic data only. The entry's goal, evidence, safe acceptance, verification and dependency are mandatory, not inferred from source existence.

### PHI-V1-001 — Establish reproducible build baseline

Goal: Establish reproducible build baseline.

Source evidence: [app build][build], [wrapper properties][wrapper]; executable/JAR absent.

Expected safe outcome and acceptance: A named clean checkout builds with recorded JDK/SDK/Gradle/runner and reproducible instructions; no dependency drift or disabled integrity checks.

Verification: Inspect tool versions; use verified runner for gradle :app:assembleDebug. Report unavailable compile_applet explicitly; do not assume ./gradlew.

Dependency: — (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-002 — Diagnose retained dashboard regression

Goal: Diagnose retained dashboard regression.

Source evidence: [dashboard test][dashboardtest], [Home][home]; retained zero-net assertion failed.

Expected safe outcome and acceptance: Reproduce or explain the failed assertion with observable state synchronization; identify test versus production effects without weakening assertions.

Verification: Run ExampleRobolectricTest with exact selected-test command and capture loading/Room arrival evidence; compare isolated and full-suite outcomes.

Dependency: 001 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-003 — Enforce authoritative finite positive creation amounts

Goal: Enforce authoritative finite positive creation amounts.

Source evidence: [repository][repo] addTransaction lacks guards; [Add VM][addvm]; FIN-015, MON-001, SEC-V1-001, FR-005.

Expected safe outcome and acceptance: UI and direct creation reject zero, negative, NaN, Infinity, malformed/overflow values before financial/event writes; edit/repay contracts remain consistent.

Verification: Synthetic direct API and UI cases plus existing accounting/repayment tests; assert no rows/events on rejection.

Dependency: 001 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-004 — Define safe monetary range and precision contract

Goal: Define safe monetary range and precision contract.

Source evidence: [transaction][tx], [Add VM][addvm], [formatters][fmt]; unbounded precision/Double; MON-002–004, FR-005.

Expected safe outcome and acceptance: Approved safe rupee range/paise policy covers creation/edit/repay/serialization/display and legacy high precision; extreme sums/scaling do not silently corrupt history. No automatic storage migration.

Verification: Boundary values, >2 decimals, tiny payments, scaling overflow and round-trip tests; record decision and safe compatibility cases.

Dependency: 003 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-005 — Verify target isolation and event association

Goal: Verify target isolation and event association.

Source evidence: [repository][repo], [ActivityEntity][act], [recovery tests][recovery]; FIN-014/016, SEC-V1-003/005, FR-008/012.

Expected safe outcome and acceptance: Missing target rejected; intended ID/friend/direction remain matched across add/edit/delete/pay/settle; unrelated rows/events unchanged.

Verification: Production-equivalent Room/repository fixtures with multiple friends/IDs; assert event links and REPLACE/cascade consequences.

Dependency: 001 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-006 — Exercise actual overlapping financial mutations

Goal: Exercise actual overlapping financial mutations.

Source evidence: [partial tests][partial] testK is sequential; optional transaction dependency in [repository][repo]; FIN-007/018, SEC-V1-003.

Expected safe outcome and acceptance: Real overlapping repay/settle/bulk/edit/delete calls preserve paid/remaining and exact event counts, without lost or duplicate effects.

Verification: Barrier-controlled coroutine overlap using actual database/activity dependencies; include same and different targets, rollback/failure scenarios.

Dependency: 005,008 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-007 — Verify replay and repeated settlement idempotency

Goal: Verify replay and repeated settlement idempotency.

Source evidence: [settlement tests][settlement] testI; ID/OPEN DAO guards [TransactionDao][txdao]; FIN-010, FR-009.

Expected safe outcome and acceptance: Repeated individual/bulk/final-payment actions create no second money effect/event and preserve first settledAt; business rejection is not new success.

Verification: Repeat and retry scenarios; assert unchanged amounts/times/history; overlap result linked to 006.

Dependency: 005 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-008 — Verify repayment rounding and threshold boundaries

Goal: Verify repayment rounding and threshold boundaries.

Source evidence: [repository][repo] remaining+0.0001 and amount-0.005; [partial tests][partial]; FIN-008/009, MON-003, FR-008.

Expected safe outcome and acceptance: Below/at/above tolerance cases preserve expected cumulative paid and nonnegative remaining for both directions; tiny payments do not claim unsupported progress.

Verification: Targeted PartialRepaymentEngineTest plus synthetic numeric boundary cases; no widened tolerance/assertion to gain PASS.

Dependency: 004 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-009 — Protect correction eligibility for legacy paid/history states

Goal: Protect correction eligibility for legacy paid/history states.

Source evidence: [repository][repo] edit/delete inspect positive aggregate, not events; [converters][conv]; TX-002, FIN-004/012, SEC-V1-005.

Expected safe outcome and acceptance: Corrupt raw paid/status/settledAt and repayment events cannot be treated as untouched merely because clamping/rounding gives zero; eligible normal correction still works.

Verification: Negative/excessive paid, zero-rounded payment event, settled/status mismatch fixtures; assert rejection retains history and unrelated entries.

Dependency: 005,008 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-010 — Verify mixed-direction and zero-net regressions

Goal: Verify mixed-direction and zero-net regressions.

Source evidence: [settlement tests][settlement] testC/testD; [accounting tests][accounting]; FIN-001–006/011, FR-004/011.

Expected safe outcome and acceptance: Gross LENT/BORROWED remaining and net are correct; mixed bulk refuses without writes; equal opposite OPEN entries remain actionable; no net-triggered closure.

Verification: Both directions, multiple friends, partial/full and zero-net fixtures; loaded UI assertions linked to 021.

Dependency: 001 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-011 — Validate backup cross-record financial semantics

Goal: Validate backup cross-record financial semantics.

Source evidence: [backup][backup] checks IDs/enums/bounds but not full consistency; [backup tests][backuptest]; FIN-014/017, SEC-V1-007, FR-020.

Expected safe outcome and acceptance: Reject duplicate/invalid IDs, orphan and cross-friend/direction events, inconsistent paid/status/settledAt/time combinations before replacement, with explicit legacy compatibility.

Verification: Crafted synthetic JSON matrix; old ledger/prefs remain on rejection; review skip-onboarding blank-profile round-trip and resolve compatibility without fabricated names.

Dependency: 004,005 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-012 — Define supported backup-version handling

Goal: Define supported backup-version handling.

Source evidence: [backup][backup] rejects -1 or >1 rather than exact supported set; BACK-001, FIN-017.

Expected safe outcome and acceptance: Only explicitly supported versions/coercions enter restore; ambiguous/missing/negative/newer versions reject safely; valid historical backups have evidenced policy.

Verification: Version absent/-1/0/other negative/1/newer and type-coercion synthetic cases; rejection precedes destruction.

Dependency: 011 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-013 — Bound selected backup input resources

Goal: Bound selected backup input resources.

Source evidence: [Settings][settings] readBytes; [backup][backup] JSON parsing; SEC-V1-008.

Expected safe outcome and acceptance: Approved size/record/nesting policy safely rejects excessive input with visible error and unchanged ledger; resource limits preserve legitimate backup compatibility.

Verification: Bounded fake streams/records near limits, malformed/deep JSON, UI failure recovery; no real-owner hostile imports or uncontrolled exhaustion.

Dependency: 011 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-014 — Verify database replacement rollback under failure

Goal: Verify database replacement rollback under failure.

Source evidence: [repository][repo] restoreValidatedData; [backup tests][backuptest] invalid reference test rejects before writes; FIN-018, BACK-003.

Expected safe outcome and acceptance: With production database dependency, failed table insertion/commit preserves old DB IDs/balances/history; successful replacement is complete, never partial success.

Verification: Injected failure at each clear/insert/commit stage using disposable Room DB; capture pre/post rows and exception/result.

Dependency: 011,012 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-015 — Establish recoverable ledger/preference restore consistency

Goal: Establish recoverable ledger/preference restore consistency.

Source evidence: [repository][repo] preference setters within Room block; [preferences][prefs] apply is separate; SEC-V1-009, FR-020.

Expected safe outcome and acceptance: Approved recovery contract prevents inconsistent completed restore after SQLite failure/process death; profile/onboarding/reminder state and ledger recover coherently, with honest failure.

Verification: Failure/restart checkpoints across preference/DB commit and notification/work reconciliation; document recovery result, not SQLite-only atomicity.

Dependency: 014 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-016 — Make exports a coherent ledger snapshot

Goal: Make exports a coherent ledger snapshot.

Source evidence: [backup][backup], [Settings VM][settingsvm] separate reads; BACK-004, SEC-V1-010, FR-019/021.

Expected safe outcome and acceptance: JSON/CSV generated during mutation/restore/clear reflects one coherent supported snapshot or explicit failure; relationships and totals agree.

Verification: Controlled overlap of reads and writes; validate generated JSON and compare CSV target IDs/friend labels against snapshot.

Dependency: 005,011 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-017 — Reject null or failed SAF output before success

Goal: Reject null or failed SAF output before success.

Source evidence: [Settings][settings] nullable openOutputStream followed by success/timestamp; BACK-005, SEC-V1-010, FR-019/021.

Expected safe outcome and acceptance: Null/throwing/write/flush/close failure never reports completed backup/export or advances backup time; cancellation safe; completed stream alone qualifies.

Verification: Fake provider streams for null, exceptions and partial writes; assert feedback, timestamp and unchanged ledger; verify real provider later.

Dependency: 001 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-018 — Decide and verify CSV spreadsheet safety

Goal: Decide and verify CSV spreadsheet safety.

Source evidence: [CSV][csv] quotes separators, no formula-prefix neutralization; BACK-006, SEC-V1-011, FR-021.

Expected safe outcome and acceptance: Approved consumer-safety policy handles formula-leading names/notes without corrupting financial meaning; CSV clearly non-restorable/plaintext.

Verification: Synthetic =1+1 and +,-,@ prefixes, quoting/newlines; inspect intended spreadsheet import behavior, not claim Phittoos code execution.

Dependency: 016 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-019 — Verify complete historical Room upgrades

Goal: Verify complete historical Room upgrades.

Source evidence: [database][db] v4 1→2→3→4 exportSchema=false; [activity tests][activitytest], [reminder tests][remindertest] v2/v3 only; FIN-018.

Expected safe outcome and acceptance: Actual supported v1/v2/v3 upgrades preserve IDs/amounts/directions/paid/status/time/history and relationships; failed upgrade does not wipe data.

Verification: Disposable historical fixtures and upgrade chains including v1; data comparisons and foreign-key checks; no destructive fallback.

Dependency: 001,005 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-020 — Prevent silent corrupt-enum financial reinterpretation

Goal: Prevent silent corrupt-enum financial reinterpretation.

Source evidence: [converters][conv] defaults direction/status/type; DB-003, FIN-016/018, SEC-V1-006.

Expected safe outcome and acceptance: Unknown legacy values have explicit safe failure/compatibility handling without silently turning owed into receivable/open/history; valid serialized names remain compatible.

Verification: Corrupt and known legacy enum fixtures through actual DB reads/upgrades; assert no undocumented financial reinterpretation.

Dependency: 019 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-021 — Gate Home summary on loaded ledger state

Goal: Gate Home summary on loaded ledger state.

Source evidence: [Home][home], [Home VM][homevm] initial zero/isLoading; FR-006/024, FIN-006, DESIGN §8/14.

Expected safe outcome and acceptance: Loading never claims all settled; loaded zero-net OPEN remains distinguishable from no OPEN; final totals/source state unchanged.

Verification: Initial delayed Room emissions and populated/empty/zero-net states; current dashboard test plus loading assertions without weakening.

Dependency: 002,010 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-022 — Verify search and distinct empty states

Goal: Verify search and distinct empty states.

Source evidence: [Home][home] ledger/search empties; [empty visual tests][emptytest]; FR-003/024.

Expected safe outcome and acceptance: No records and no matches offer accurate distinct guidance; clear search and select/add friend retain correct context.

Verification: Synthetic blank/matching/no-match queries, new/existing friends and loaded-empty state; visual/manual device check.

Dependency: 021 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-023 — Recover Add save/error/busy state honestly

Goal: Recover Add save/error/busy state honestly.

Source evidence: [Add VM][addvm] save lacks universal exception/finally cleanup; FR-005/024, DESIGN §9/14.

Expected safe outcome and acceptance: Failed persistence resets busy and gives safe outcome/retry; no false success or duplicate transaction/activity; separately created friend is not claimed as transaction success.

Verification: Injected repository insert failure and repeat taps; assert form state, rows/events and feedback for add/edit.

Dependency: 003,004 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-024 — Verify repayment duplicate-tap and error feedback

Goal: Verify repayment duplicate-tap and error feedback.

Source evidence: [Friend detail][detail], [Detail VM][detailvm] in-flight flags; [detail accessibility tests][detailtest]; FR-008/009.

Expected safe outcome and acceptance: Repeated taps/cancel/failure keep intended target and truthful remaining/error; busy state returns usable; no unintended celebration.

Verification: Compose repeated-tap and failure scenarios plus device IME; DB assertions use production dependencies.

Dependency: 006,007 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-025 — Keep due/attention state fresh across calendar changes

Goal: Keep due/attention state fresh across calendar changes.

Source evidence: [dates][datesource], [Activity VM][activityvm] regroup only; Home/Detail no matching timer; DATE rules, FR-015.

Expected safe outcome and acceptance: Approved lifecycle/clock refresh keeps due/overdue/attention truthful through midnight/timezone changes without financial mutation; settled/zero remaining not overdue.

Verification: Injected clocks, DST forward/back, month/year/timezone/resume changes and long-open screen; compare labels and balances.

Dependency: 001 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-026 — Provide notification permission acquisition and denial UX

Goal: Provide notification permission acquisition and denial UX.

Source evidence: [notification][notification] declares/checks, no production request; [Settings][settings]; REM-002, SEC-V1-013, FR-016.

Expected safe outcome and acceptance: Eligible Android versions offer understandable owner permission flow; denial/revocation safe without sent-history claim; ledger works without permission.

Verification: Fresh API33+ installation, grant/deny/permanent denial/revoke and pre33; inspect request calls, device feedback/history.

Dependency: 001 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-027 — Verify owner notification disclosure and routing

Goal: Verify owner notification disclosure and routing.

Source evidence: [notification][notification] friend/amount, immutable intent, no private visibility override; [MainActivity][main]; SEC-V1-012/013.

Expected safe outcome and acceptance: Approved lock-screen disclosure is evidenced; no friend messaging; missing/existing friend routing safe on fresh/reused activity; no intent-triggered financial mutation.

Verification: Locked/unlocked device, notification tap, invalid/deleted IDs and reused-activity tests; record OS settings/exposure.

Dependency: 026 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-028 — Verify reminder posting/history crash and overlap behavior

Goal: Verify reminder posting/history crash and overlap behavior.

Source evidence: [engine][engine], [repository][repo] recordReminderSent post then log; no unique stage; REM-003, SEC-V1-014.

Expected safe outcome and acceptance: 7/15/30 qualification and disabled/BORROWED suppression remain correct; posting/history failure recovery is truthful, no unsupported exactly-once guarantee.

Verification: Injected sender/history failures, overlapping workers and process restart; assert stage/event counts and no changed paid/status/direction.

Dependency: 001,026 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-029 — Verify themes and accessible spoken interactions

Goal: Verify themes and accessible spoken interactions.

Source evidence: [theme][theme], [colors][colors], accessibility test sources; FR-022, UI-001/003, DESIGN §4/15.

Expected safe outcome and acceptance: Light/Dark/System preserve financial semantics; ≥48dp critical actions, readable measured contrast and usable TalkBack labels/focus/dialogs; no color-only meaning.

Verification: ThemePreferenceTest and accessibility suites; manual TalkBack, font scaling, measured contrasts in all critical states; no formal compliance claim from source.

Dependency: 021,023,026 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-030 — Verify responsive amounts, names and insets

Goal: Verify responsive amounts, names and insets.

Source evidence: [responsive tests][responsive], [inset tests][insettest]; DESIGN §6/15, FR-024.

Expected safe outcome and acceptance: Long names/amounts and critical actions visible at constrained widths/200% text; IME/gesture/3-button bars, portrait/landscape and RTL do not hide money context.

Verification: Existing responsive/inset tests plus representative devices; record viewport/font/API/orientation and synthetic screenshots.

Dependency: 029 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-031 — Verify launcher, splash and monochrome on devices

Goal: Verify launcher, splash and monochrome on devices.

Source evidence: [brand][brand], [splash][splash], [DESIGN](DESIGN.md) static previews only.

Expected safe outcome and acceptance: Locked tagline/₹/arrows retained, no unacceptable native clipping/tint/launch transition; representative masks/themes/device appearance evidenced.

Verification: Physical launcher/splash and monochrome masks; replace unusable visual baseline only under future asset authorization, never assume greeting.png valid.

Dependency: 039 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-032 — Verify celebration eligibility and accessible motion

Goal: Verify celebration eligibility and accessible motion.

Source evidence: [celebration][celebration], [celebration tests][celebrationtest]; FR-018, BULK-003, DESIGN §16.

Expected safe outcome and acceptance: Only successful all-OPEN-cleared active-screen operation celebrates; cancel/delete/zero-net/repeat does not; disabled motion and accessibility timeout/dismissal usable.

Verification: SettlementCelebrationTest/VisualRefinement source cases and device TalkBack/reduced animation/pause/resume; no money-transfer claim.

Dependency: 007,024 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-033 — Audit merged release identity, permissions and components

Goal: Audit merged release identity, permissions and components.

Source evidence: [manifest][manifest], [app build][build]; debug artifact only; SEC-V1-012, FR-004/016.

Expected safe outcome and acceptance: Exact release variant preserves approved applicationId/version/SDK and least privilege; no INTERNET V1; exported library/system components justified and protected.

Verification: Inspect newly merged release manifest/dependency contributions and intent reachability; source-only inventory insufficient.

Dependency: 001 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-034 — Verify Android backup and device-transfer boundaries

Goal: Verify Android backup and device-transfer boundaries.

Source evidence: [backup rules][backuprules], [extraction rules][extraction], allowBackup=false; SEC-V1-015.

Expected safe outcome and acceptance: Supported OS/OEM transfer behavior documented honestly; disclosures do not promise universal no-transfer based only on manifest.

Verification: Controlled synthetic cloud backup/device transfer/restore checks with OS/OEM recorded; inspect private DB/preferences outcomes.

Dependency: 033,039 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-035 — Align privacy, Play and agent disclosures with behavior

Goal: Align privacy, Play and agent disclosures with behavior.

Source evidence: [PRIVACY](PRIVACY.md), [PLAY_DECLARATIONS](PLAY_DECLARATIONS.md), [AGENTS](../AGENTS.md); SEC-V1-011/012/015.

Expected safe outcome and acceptance: Remove absent draft-sharing claims, correct permission/acquisition and external SAF boundaries; channel/FileProvider and policy-versus-enforcement wording reconciled without invented controls.

Verification: Line-by-line disclosure review against signed-variant/source evidence; authorized future docs scope only; no unsupported DPDP/WCAG/security certification.

Dependency: 017,026,027,033,034 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-036 — Verify local deletion and external-copy boundaries

Goal: Verify local deletion and external-copy boundaries.

Source evidence: [Settings VM][settingsvm], [preferences][prefs], cascade DAOs; FIN-013, SEC-V1-015, FR-023.

Expected safe outcome and acceptance: Cancel preserves data; confirmed clear empties local tables/prefs and reconciles notifications/work/onboarding; external files remain explicitly disclosed.

Verification: Disposable synthetic ledger, cancel/success/failure/restart and retained external provider copy; no forensic erasure promise.

Dependency: 015,035 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-037 — Review resolved dependencies and signing supply chain

Goal: Review resolved dependencies and signing supply chain.

Source evidence: [app build][build], [catalog][catalog], [LICENSES](LICENSES.md); SEC-V1-016; unused aliases not services.

Expected safe outcome and acceptance: Resolved release dependencies/licenses and build provenance reviewed; any verified vulnerabilities triaged; private production keys/passwords remain protected and absent from source, logs and published artifacts; public signing certificates are expected artifact metadata.

Verification: Record resolved graph, audit method/date/advisories/licenses and secure signing binding presence only; no credential values, no claims of clean audit without execution.

Dependency: 001,033 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-038 — Record current full regression and required build evidence

Goal: Record current full regression and required build evidence.

Source evidence: [AGENTS](../AGENTS.md), [PROJECT_STATE](PROJECT_STATE.md); retained 268/269 not green.

Expected safe outcome and acceptance: Current exact SHA has targeted acceptance evidence and full JVM/Robolectric pass, meaningful build result and no weakened tests; remaining failures explicitly block completion.

Verification: gradle :app:testDebugUnitTest and verified build/helper commands; retain XML counts, exit codes and SHA/env/artifact mapping; inspect generated screenshot changes.

Dependency: 002–028 as applicable fixes/finance gates (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-039 — Run representative instrumented and offline device smoke

Goal: Run representative instrumented and offline device smoke.

Source evidence: [instrumented test][instrumented] checks identity only; FR core journeys.

Expected safe outcome and acceptance: Synthetic onboarding/add/pay/settle/history/settings operate offline on supported devices; device checks have actual outcomes beyond identity test.

Verification: Authorized :app:connectedDebugAndroidTest where runner/device available plus manual airplane-mode journeys; unavailable device means missing gate, not PASS.

Dependency: 001,021,023,026 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-040 — Check current Play SDK and artifact requirements

Goal: Check current Play SDK and artifact requirements.

Source evidence: [app build][build] min24/target36/version1; [PLAY_DECLARATIONS](PLAY_DECLARATIONS.md).

Expected safe outcome and acceptance: At release time official applicable target SDK, artifact/device/package/version constraints checked; required changes separately approved, no assumption based on today target alone.

Verification: Date-stamped official policy/Console evidence and candidate comparison; no guessed requirements or automatic SDK/dependency update.

Dependency: 033 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-041 — Build and verify production-signed release artifacts

Goal: Build and verify production-signed release artifacts.

Source evidence: [app build][build] conditional release signing; [RELEASE_CANDIDATE](RELEASE_CANDIDATE.md) old unsigned reports.

Expected safe outcome and acceptance: Authorized production APK/AAB tied to SHA/version has correct certificate/identity, successful validation and hashes; secure key handling, no debug signing mistaken for production.

Verification: Verified runner :app:assembleRelease :app:bundleRelease; APK apksigner verify and AAB signature/bundle validation with available verified tooling; record actual outputs, no secrets.

Dependency: 037,038,040 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-042 — Verify signed install, upgrade and backup recovery

Goal: Verify signed install, upgrade and backup recovery.

Source evidence: [RELEASE_CANDIDATE](RELEASE_CANDIDATE.md), [database][db], [backup][backup]; historical phone QA unbound.

Expected safe outcome and acceptance: Signed candidate installs and same-identity/same-signing upgrade preserves synthetic balances/history; export/reinstall/restore verified. Different old applicationId not claimed in-place upgrade.

Verification: Real-device install/upgrade/recovery matrix with before/after IDs/values/events, certificate/version/device and artifact SHA; safe disposable data only.

Dependency: 014,015,017,019,041 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-043 — Verify public support and privacy URLs

Goal: Verify public support and privacy URLs.

Source evidence: [PRIVACY](PRIVACY.md), docs/index.html and docs/privacy/index.html; hosting unverified.

Expected safe outcome and acceptance: Public support/privacy pages load unauthenticated with current accurate text and usable contact/links on mobile.

Verification: Read-only HTTPS checks and external/mobile browsing for configured GitHub Pages URLs; record status/date/content, do not publish here.

Dependency: 035 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-044 — Review Play declarations against final candidate

Goal: Review Play declarations against final candidate.

Source evidence: [PLAY_DECLARATIONS](PLAY_DECLARATIONS.md), [PRD](PRD.md), [SECURITY](SECURITY.md).

Expected safe outcome and acceptance: Data safety, content rating, app access, financial category and permissions answers match candidate/disclosures; offline ledger not presented as payments or shared service.

Verification: Owner-reviewed Console checklist/evidence against artifact; no automated submission or compliance clearance from draft text.

Dependency: 035,040,041,043 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-045 — Prepare truthful store listing and candidate screenshots

Goal: Prepare truthful store listing and candidate screenshots.

Source evidence: [brand][brand], [DESIGN](DESIGN.md), [PRD](PRD.md); no verified release screenshot set.

Expected safe outcome and acceptance: Approved title/copy/icon/screenshots depict actual V1 with synthetic data, locked brand and no friend-confirmation/sync/payment claims.

Verification: Capture final candidate under separate authorization; review sizes/current store requirements, accessibility and redaction; no new assets in this task.

Dependency: 030,031,041 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-046 — Verify applicable closed-testing and Console prerequisites

Goal: Verify applicable closed-testing and Console prerequisites.

Source evidence: [RELEASE_CANDIDATE](RELEASE_CANDIDATE.md) owner checklist; account/program applicability unknown.

Expected safe outcome and acceptance: Applicable account/testing/production-access prerequisites have current Console evidence; no invented tester count/duration or assumption of exemption.

Verification: Authorized owner Console read/review and date-stamped current official requirements; lack of account/device access recorded as precise prerequisite.

Dependency: 040,041,044 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays VERIFY until that proof exists.

### PHI-V1-047 — Define privacy-respecting crash and release recovery procedure

Goal: Define privacy-respecting crash and release recovery procedure.

Source evidence: [SECURITY](SECURITY.md), [RULES](RULES.md) no current release plan; no telemetry assumed.

Expected safe outcome and acceptance: Approved failure/ANR triage, halt/staged-rollout decision and data-safe recovery procedure exist; no silent analytics SDK or unsafe schema downgrade.

Verification: Tabletop upgrade/restore failure and crash report collection/retention review; use available OS/Console diagnostics only when authorized; no universal rollback promise.

Dependency: 015,042 (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays OPEN until that proof exists.

### PHI-V1-048 — Record final release go/no-go decision

Goal: Record final release go/no-go decision.

Source evidence: [AGENTS](../AGENTS.md), [RULES](RULES.md), [SECURITY](SECURITY.md), this gate register.

Expected safe outcome and acceptance: All mandatory evidence applies to final candidate; no unresolved P0 or critical P1; conditional gates have justified disposition; owner explicitly approves separate publication.

Verification: Independent artifact/QA/privacy/store checklist review with decision, exact SHA and evidence links. Blocked now because required current regression, safety and signed/device/Console proof are missing.

Dependency: All other YES gates; assessed CONDITIONAL gates (§5 suffix notation). Completion evidence: the §13 contract plus the results/artifacts named above; status stays BLOCKED until that proof exists.

## 14. P0/P1 release blockers

No new reproducible P0 is asserted. The following is a clearing-proof checklist, not a PASS report. Rows are groups of linked task gates; each underlying task retains its own contract/status.

| Blocker tasks | Impact / evidence type | Clearing proof | Current status |
| --- | --- | --- | --- |
| 003–004,008–009 | Source validation/precision/aggregate-history gaps; extreme corruption unverified | Rejected-invalid/boundary/history tests and approved contract; no changed unrelated money | OPEN / VERIFY |
| 005–007,010 | Critical target/concurrency/replay/direction verification; sequential source tests incomplete | Actual overlap, isolation, no duplicate event and signed-total assertions | VERIFY |
| 011–012,014–017 | Source semantic/cross-store/export/null-success gaps; invalid-input test not rollback proof | Crafted rejection, commit/restart recovery, coherent snapshot and stream completion evidence | OPEN / VERIFY |
| 019–020 | Limited historical fixtures and silent enum defaults | Complete supported upgrade/legacy data preservation and safe corrupt handling | VERIFY / OPEN |
| 002,021,023,038 | Retained failed assertion plus loading/save-state gaps; current regression absent | Explained failure, truthful UI/retry and current full/build results | VERIFY / OPEN |
| 033,037,039–042,044,046 | Release identity/supply-chain/QA/store critical evidence missing | Actual reviewed release variant, signed artifacts, device and applicable policy/Console proof | VERIFY |
| 048 | Cannot authorize go/no-go from unfinished evidence | Every YES gate cleared; conditional disposition reviewed; explicit owner decision | BLOCKED |

P2 YES gates 026/035/043/045/047 are required delivery/disclosure/store/operations obligations despite lower engineering severity. CONDITIONAL items (013/018/022/024–025/027–032/034/036) require recorded impact and release disposition; they are not automatically unconditional P1 blockers or freely skippable. Elevate on demonstrated harm or mandatory acceptance failure. Any reproduced financial corruption creates P0 / NO RELEASE immediately, even if not already listed.

## 15. Documentation and traceability

Master evidence/§13 link each significant task to source/test and applicable FIN, SEC and PRD FR identifiers. Governing paths: [AGENTS](../AGENTS.md), [PROJECT_STATE](PROJECT_STATE.md), [ARCHITECTURE](ARCHITECTURE.md), [RULES](RULES.md), [SECURITY](SECURITY.md), [PRD](PRD.md), [DESIGN](DESIGN.md). DESIGN has sections but no DS IDs; none invented. Release-only tasks are justified by explicit current request/release obligations rather than manufactured FR IDs.

`docs/RELEASE.md` is a future documentation task, not authored by this task; no dependency requires silently starting it. RELEASE_CANDIDATE.md is historical. Documentation mismatches are scoped to future authorized task 035, never repaired opportunistically here. Stable IDs survive completion and remain traceable from future tests/PRs.

## 16. Proposed V2 discovery backlog

The following NON-EXECUTABLE questions all have status **PROPOSED V2**, priority **V2** and no V1 release dependency; no master implementation ID is assigned.

- Is optional friend linking wanted, and how is mutual consent/revocation understood?
- Which records/notes/reliability remain private versus shared, and who may edit/view?
- What does shared repayment confirmation mean without implying processed/verified payment?
- What identity/account recovery and server-side object authorization properties are needed?
- How do replay/idempotency/offline sync conflicts preserve history rather than overwrite it?
- How do privacy, deletion, retention/export and unlinking work?
- Which V2 UX and technologies merit evaluation after explicit product authorization?

No vendor, backend, framework, algorithm, endpoint, schedule or architecture is approved. V1 does not gain accounts/shared ledger/payment features through this list. Historical shared-confirmation proposals remain superseded for V1.

## 17. Change management and update protocol

Refresh HEAD/remote/working-tree baseline before each authorized task. Reproduce or gather evidence using synthetic data; record code gap versus confirmed failure and exact prerequisites. Preserve accepted work/unrelated files. Update status only after entry acceptance and independent review; if environment/credentials/device block work, name the prerequisite and mark BLOCKED rather than invent missing access.

Record exact commands, counts and artifact/commit evidence; keep old results historically labeled when superseded. Move to DONE only for evidenced implementation/verification at named baseline, not because docs or a test name exist. Record dependency/priority/disposition changes with reasons; do not silently erase task history. Promote reproduced integrity failures to P0 and stop unsafe release. Follow AGENTS bounded attempts/no weakened validation; no competing retry policy.

Every future task has its own explicit file scope and permissions. This document authorizes none of its fixes, dependency changes, commits, store actions or extra Markdown edits. New findings need stable IDs with independent acceptance; no broad “fix all bugs” task. After code change refresh affected QA/release gates; older green results do not attest new artifacts.

## 18. Open questions, assumptions and source references

Unresolved: safe monetary range/precision and legacy handling; consistent profile-skip backup support; explicit accepted backup versions; cross-store recovery; truly concurrent mutation/export/reminder results; corrupt enum policy; complete old upgrades; current signing/Console/account/device access; actual current Play requirements, public URLs and privacy-respecting operations evidence. These are decisions/gates, not chosen solutions.

All seven prior documents and brand/release/privacy/Play/license sources inform this baseline. App/test source remains unchanged by documentation commits since 67c777a; that does not create current runtime proof. No full regression, build, audit, URL, device or Play check was executed. Retained local XML/merged debug outputs under app/build are not portable CI or release attestations. Conversation material was not independently retrieved; source wins.

No pre-existing modifications were found; only docs/TASKS.md was created. No credentials, raw backups or personal finance data used. Recheck relative references and source identifiers as code changes.

[repo]: ../app/src/main/java/com/professor1416/phittoos/data/repository/PhittoosRepository.kt
[txdao]: ../app/src/main/java/com/professor1416/phittoos/data/dao/TransactionDao.kt
[tx]: ../app/src/main/java/com/professor1416/phittoos/data/model/TransactionEntity.kt
[act]: ../app/src/main/java/com/professor1416/phittoos/data/model/ActivityEntity.kt
[db]: ../app/src/main/java/com/professor1416/phittoos/data/db/AppDatabase.kt
[conv]: ../app/src/main/java/com/professor1416/phittoos/data/db/Converters.kt
[addvm]: ../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/AddTransactionViewModel.kt
[homevm]: ../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/HomeViewModel.kt
[detailvm]: ../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/FriendDetailViewModel.kt
[activityvm]: ../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/ActivityViewModel.kt
[settingsvm]: ../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/SettingsViewModel.kt
[home]: ../app/src/main/java/com/professor1416/phittoos/ui/screens/home/HomeScreen.kt
[detail]: ../app/src/main/java/com/professor1416/phittoos/ui/screens/frienddetail/FriendDetailScreen.kt
[settings]: ../app/src/main/java/com/professor1416/phittoos/ui/screens/settings/SettingsScreen.kt
[prefs]: ../app/src/main/java/com/professor1416/phittoos/data/preferences/UserPreferences.kt
[backup]: ../app/src/main/java/com/professor1416/phittoos/export/PhittoosBackupManager.kt
[csv]: ../app/src/main/java/com/professor1416/phittoos/export/PhittoosCsvExporter.kt
[fmt]: ../app/src/main/java/com/professor1416/phittoos/ui/util/Formatters.kt
[datesource]: ../app/src/main/java/com/professor1416/phittoos/domain/DueDateHelper.kt
[notification]: ../app/src/main/java/com/professor1416/phittoos/reminder/ReminderNotificationHelper.kt
[engine]: ../app/src/main/java/com/professor1416/phittoos/reminder/SmartReminderEngine.kt
[main]: ../app/src/main/java/com/professor1416/phittoos/MainActivity.kt
[manifest]: ../app/src/main/AndroidManifest.xml
[build]: ../app/build.gradle.kts
[catalog]: ../gradle/libs.versions.toml
[wrapper]: ../gradle/wrapper/gradle-wrapper.properties
[backuprules]: ../app/src/main/res/xml/backup_rules.xml
[extraction]: ../app/src/main/res/xml/data_extraction_rules.xml
[theme]: ../app/src/main/java/com/professor1416/phittoos/ui/theme/Theme.kt
[colors]: ../app/src/main/java/com/professor1416/phittoos/ui/theme/Color.kt
[splash]: ../app/src/main/res/values/themes.xml
[brand]: branding/PHITTOOS_BRAND_V1.md
[celebration]: ../app/src/main/java/com/professor1416/phittoos/ui/screens/frienddetail/SettlementCelebrationCard.kt
[partial]: ../app/src/test/java/com/professor1416/phittoos/PartialRepaymentEngineTest.kt
[settlement]: ../app/src/test/java/com/professor1416/phittoos/OfflineSettlementFlowTest.kt
[accounting]: ../app/src/test/java/com/professor1416/phittoos/PhittoosAccountingEngineTest.kt
[recovery]: ../app/src/test/java/com/professor1416/phittoos/ContextualAddAndWrongEntryRecoveryTest.kt
[backuptest]: ../app/src/test/java/com/professor1416/phittoos/PhittoosBackupRestoreTest.kt
[activitytest]: ../app/src/test/java/com/professor1416/phittoos/ActivityReminderHistoryTest.kt
[remindertest]: ../app/src/test/java/com/professor1416/phittoos/SmartReminderEngineTest.kt
[dashboardtest]: ../app/src/test/java/com/professor1416/phittoos/ExampleRobolectricTest.kt
[emptytest]: ../app/src/test/java/com/professor1416/phittoos/EmptyStateVisualTest.kt
[detailtest]: ../app/src/test/java/com/professor1416/phittoos/FriendDetailAccessibilityTest.kt
[responsive]: ../app/src/test/java/com/professor1416/phittoos/HomeScreenResponsiveLayoutTest.kt
[insettest]: ../app/src/test/java/com/professor1416/phittoos/LayoutAndSystemInsetsTest.kt
[celebrationtest]: ../app/src/test/java/com/professor1416/phittoos/SettlementCelebrationTest.kt
[instrumented]: ../app/src/androidTest/java/com/professor1416/phittoos/ExampleInstrumentedTest.kt
