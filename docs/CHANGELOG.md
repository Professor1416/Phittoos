# Phittoos V1 changelog

## 1. Changelog policy and snapshot

**Phittoos — Tere mere hisaab… Phittoos!** Source-backed development history for [Professor1416/Phittoos](https://github.com/Professor1416/Phittoos), not release certification or a marketing announcement.

| Snapshot | Evidence |
| --- | --- |
| Branch / inspected source SHA | `work` / `5fea38283eeeaf6b16bc3d0c2f2ef4485c7ccd65` |
| Remote main | Same SHA observed with `git ls-remote origin refs/heads/main` |
| Inspection date | 2026-10-12 UTC |
| Initial working tree | Clean; CHANGELOG.md absent |

**VERIFIED SOURCE** identifies implementation at this SHA; **VERIFIED COMMIT** identifies inspected commit/diff, not runtime correctness; **HISTORICAL REPORT** is older documentation/retained output; **UNVERIFIED** lacks current runtime/publication proof; **PROPOSED FUTURE** is outside approved V1. Dates below are Git commit dates as recorded with their timezone, not public-release dates. This is a selected milestone history, not every commit.

**Unreleased** describes development not established as published. A **release candidate** is a reported/prepared candidate, not production publication. A **Published Release** entry requires actual verified publication evidence and accurate version/date; versionName, commits, builds, tags or documentation alone do not prove public Play shipment. No dated `[1.0.0]` production entry is justified here. Update verified changes without upgrading historical results into new PASS claims (§9).

## [Unreleased] — V1 development

The following are **VERIFIED SOURCE** capabilities at the inspected SHA, with runtime/security/release limits in §6. They are owner-entered ledger behavior, not verified money transfer or friend participation.

### Added

- LENT/BORROWED entries for local friends, multiple independent entries, receivable/owed/OPEN totals and net position; zero net can coexist with open obligations.
- Cumulative partial repayments, final repayment and individual settlement; same-direction bulk with empty/mixed-direction refusal and ordinary history retention.
- Due dates/overdue indicators; owner-local LENT reminder stages and persisted activity history; private reliability summaries and settlement celebration.
- Six screen areas: Onboarding, Home, Add/Edit Transaction, Friend Detail, Activity and Settings; local profile/preferences.
- Plaintext user-selected JSON backup and validated replacement restore, plus non-restorable CSV export; these can leave the device through external SAF providers.

### Changed

- Guarded edit/delete for untouched OPEN entries and matching creation-history handling; aggregate/history eligibility edge cases remain unresolved.
- Occurrence-date selection/backdating and future-calendar-day rejection in add/edit paths; occurrence, due and payment/event time remain distinct.
- System/Light/Dark themes, semantic financial colors, approved corrected ₹/arrow branding and exact tagline; edge-to-edge/inset handling and responsive layout definitions. Device/visual fidelity remains unverified.
- Local-record wording and initial form/detail Flow behavior were refined; these changes did **not** establish safe Home loading/all-settled presentation.

### Security

- App-applied Firebase/remote-client dependencies and Google Services application were removed in inspected cleanup history. Current source has no app-controlled V1 ledger network/auth service or INTERNET declaration; unused catalog aliases/root apply-false plugin remain, not active features.
- Sandbox storage, notification permission checks/immutable intents and backup intent configuration exist; these are bounded source facts, not encryption, legal-compliance, dependency-audit or universal no-transfer guarantees.

## 3. Verified development milestones

Reverse chronological. Every row is **VERIFIED COMMIT** from changed files/diff; current retained behavior is cross-checked with linked source references (§10). No row is a test PASS or release announcement. Categories describe the repository change, not certification.

| Commit date | Category | Meaningful change and evidence boundary | Commit / evidence status |
| --- | --- | --- | --- |
| 2026-10-08 | Added | Occurrence-date picker/backdating, future-day helper/guards, repository persistence and creation-event date updates; TransactionDateBackdateTest added | [0d0b34c](https://github.com/Professor1416/Phittoos/commit/0d0b34cdab55c5a51418bb41682668cc4600195b) — VERIFIED COMMIT |
| 2026-09-29 | Changed | Theme system-bar icon appearance and screen inset/padding handling; LayoutAndSystemInsetsTest added. Does not prove every device bar/layout | [c465892](https://github.com/Professor1416/Phittoos/commit/c4658927affe196e86fc873ef151c23a451fa9f7) — VERIFIED COMMIT |
| 2026-09-26 | Changed | Local-record terminology and initial empty friend emissions/eager detail state collection refined; Home loading limitation remains | [f3af526](https://github.com/Professor1416/Phittoos/commit/f3af5261909b3069b13328fc780db1ae823eed59) — VERIFIED COMMIT |
| 2026-09-24 | Added | Contextual edit route, guarded open/unpaid edit/delete, associated activity update/delete, dialogs and recovery test source | [39049d1](https://github.com/Professor1416/Phittoos/commit/39049d182688d123207d6826a691635dcc4d0d81) — VERIFIED COMMIT |
| 2026-09-24 | Changed | System/Light/Dark preference/theme flow and empty-state UI added; same-day follow-up removed DashboardInsights and applied semantic theme colors across screens. Removed insight engine is not a current feature | [5ec9a28](https://github.com/Professor1416/Phittoos/commit/5ec9a2842c389a5e8771a025ad4b266304cc405b), [9af52e4](https://github.com/Professor1416/Phittoos/commit/9af52e4), [72bfec5](https://github.com/Professor1416/Phittoos/commit/72bfec5) — VERIFIED COMMIT group |
| 2026-09-22 | Fixed | Non-open/zero-remaining due-state handling and repayment-dialog validation refined; AccountingStateCorrectnessTest added. “Fixed” records inspected logic change, not universal financial correctness | [657a046](https://github.com/Professor1416/Phittoos/commit/657a046fa3b2cd7a425198cc6d5096fc578788ba) — VERIFIED COMMIT |
| 2026-09-20 | Security | Removed app-applied Firebase/remote-client dependencies and Google Services app plugin; sensitive-file ignore patterns/disclosures updated. Backup XML documents allowBackup intent rather than proving every OEM transfer disabled | [59cfe66](https://github.com/Professor1416/Phittoos/commit/59cfe66a438b009f7ba119904cd12e78f289fc05) — VERIFIED COMMIT |
| 2026-09-19 | Changed | Namespace/package and applicationId standardized to com.professor1416.phittoos; former package installs cannot be assumed in-place upgrade compatible | [36586bd](https://github.com/Professor1416/Phittoos/commit/36586bd1e85c0fda8ab44d179f2c296d6318d321) — VERIFIED COMMIT |
| 2026-09-15 | Added | JSON generation/validation/replacement flow, repository restore and Settings UI/test source added under the earlier com.example package; later identity migration moved paths | [1587e10](https://github.com/Professor1416/Phittoos/commit/1587e10eb181f959b0c588e9faa1cc2444ce4113) — VERIFIED COMMIT |

Earlier core accounting/repayment/reminder implementation is represented by the current source summary, without assigning uninspected introduction dates. Commit subjects were not treated as sufficient evidence; diffs constrain descriptions above.

## 4. Documentation milestones

The October documentation program is separate from application changes. **VERIFIED COMMIT** below means documentation committed in ancestry of observed remote main, not Android gate clearance or a production release. Dates are commit dates.

| Date | Document milestone | Commit / state |
| --- | --- | --- |
| 2026-10-12 | Root AGENTS.md bounded troubleshooting and exact final-report requirements finalized; future technology neutrality preserved | [51913ed](https://github.com/Professor1416/Phittoos/commit/51913ed5d7ea4d586752d7abe0de5853d6c99669) — VERIFIED COMMIT |
| 2026-10-12 | PROJECT_STATE.md source snapshot and evidence limitations | [ee1bfbc](https://github.com/Professor1416/Phittoos/commit/ee1bfbc169caa7a55403f80705e681bd438a15c9) — VERIFIED COMMIT |
| 2026-10-12 | ARCHITECTURE.md current V1 components/data flow and risks | [b4550fc](https://github.com/Professor1416/Phittoos/commit/b4550fc841a3aaed8ed2c082e09c233554d11f00) — VERIFIED COMMIT |
| 2026-10-12 | RULES.md financial/engineering acceptance and known gaps | [4fc1a27](https://github.com/Professor1416/Phittoos/commit/4fc1a270b0966f01a265b8b49763a04647f8e7e5) — VERIFIED COMMIT |
| 2026-10-12 | SECURITY.md V1 threat model and control/test gates | [4d389d9](https://github.com/Professor1416/Phittoos/commit/4d389d99edaff36bf54d89713aefd758961c17e3) — VERIFIED COMMIT |
| 2026-10-12 | PRD.md approved owner-only product requirements and proposed metrics | [fa8c30f](https://github.com/Professor1416/Phittoos/commit/fa8c30f4ba4457e407b196ca7771187ea4ef49dd) — VERIFIED COMMIT |
| 2026-10-12 | DESIGN.md actual themes/components/UI and unverified visual/device QA | [42c91f8](https://github.com/Professor1416/Phittoos/commit/42c91f87b433a5b44d3cca7c70fecb8d87cc13dd) — VERIFIED COMMIT |
| 2026-10-12 | TASKS.md 48 source-backed engineering/release tasks | [113dfb3](https://github.com/Professor1416/Phittoos/commit/113dfb31e218fd947d650ba944aec9462a69d342) — VERIFIED COMMIT |
| 2026-10-12 | RELEASE.md release procedure, signing/device/Console gates and blocked decision | [5fea382](https://github.com/Professor1416/Phittoos/commit/5fea38283eeeaf6b16bc3d0c2f2ef4485c7ccd65) — VERIFIED COMMIT |
| 2026-10-12 inspection | CHANGELOG.md (this task) | Working-tree draft only at authorship; **not yet committed/pushed** |

AGENTS.md introduction/update also appear on 2026-10-09 in [1b82683](https://github.com/Professor1416/Phittoos/commit/1b82683) and [67c777a](https://github.com/Professor1416/Phittoos/commit/67c777a). Later commits through this snapshot changed documentation only. Historical documentation snapshot SHAs remain their inspected baselines. No task status or financial gap became DONE through writing these files. Update the draft row only when actual commit/publication state is verified.

## 5. Release candidate history

**HISTORICAL REPORT:** [RELEASE_CANDIDATE.md](RELEASE_CANDIDATE.md) describes **Phittoos-V1.0.0-RC1**, reported build date **2026-09-19**, an unsigned release APK and an AAB output. AAB signing is not independently established; neither artifact is verified here as a current signed/installable candidate.

The report states **215 tests passed**, artifact sizes/hashes and source SHA **N/A**, with signing/device/Play checklist items still unverified. These are report claims, not newly executed results or a release tag. No assertion is made that those outputs still exist locally or that their hashes match current files. Subsequent 268/269 retained evidence (§6) is also historical and not a complete PASS. The report does not establish public publication, Play approval or universal upgrade/backup safety.

## 6. Known unresolved issues

See [TASKS.md](TASKS.md) for contracts rather than repeating its backlog. Recounted at this snapshot: **48 tasks; 17 OPEN, 30 VERIFY, 1 BLOCKED, 0 DONE; 35 mandatory and 13 conditional release entries**. PHI-V1-048 remains BLOCKED; this changelog clears none.

| Finding / evidence classification | Task trace |
| --- | --- |
| Source-observed creation finite/positive/range/precision gaps and Double display/arithmetic limits; extreme corruption not reproduced here | PHI-V1-003–004,008–009 |
| Actual simultaneous financial mutation proof missing; sequential “concurrent” test does not establish races safe | PHI-V1-005–007 |
| Source-observed backup status/time/cross-friend/version/resource validation limits | PHI-V1-011–013 |
| Room versus SharedPreferences recovery is not atomic; interruption/rollback outcomes unverified | PHI-V1-014–015 |
| Separate export reads and nullable SAF output followed by success/timestamp; specific provider/data-loss outcome not reproduced | PHI-V1-016–018 |
| Complete v1 upgrade evidence missing; silent enum fallback observed; universal migration safety unverified | PHI-V1-019–020 |
| Home initial loading zero state can render all-settled before actual balances; retained assertion failure does not prove production harmless | PHI-V1-002,021,038 |
| Runtime notification acquisition absent from inspected production code; fresh-install/device delivery, clock/privacy/history behavior unverified | PHI-V1-025–028,034 |
| Current full regression, signed candidate, device/Console/publication proof unavailable | PHI-V1-033,037–048 |

Retained local XML was inspected: **269 tests, 268 passed, 1 failure, 0 errors/skips**; ExampleRobolectricTest's zero-net OPEN summary assertion failed. This is **HISTORICAL REPORT**, not execution at this changelog SHA or a full-suite PASS. No current build/test/device/security reproduction was run. No new P0 defect was demonstrated; any reproduced wrong balance/target, duplicate financial effect, lost history or unsafe migration/partial restore remains **P0 / NO RELEASE** under AGENTS/RULES.

## 7. Version and release status

[app/build.gradle.kts](../app/build.gradle.kts) currently declares **versionName 1.0.0**, **versionCode 1**, **applicationId/namespace com.professor1416.phittoos**. These are **VERIFIED SOURCE** build values, not shipment evidence.

| Version label | Evidence | Status | Publication proof |
| --- | --- | --- | --- |
| Unreleased V1 development (declared 1.0.0/code 1) | Current source and inspected Git history | Implemented in source with unresolved gates | No public production release verified |
| Historical Phittoos-V1.0.0-RC1 | September 19 report, SHA N/A, unsigned APK and AAB output claims (AAB signing unverified) | HISTORICAL REPORT; current signing/device correctness UNVERIFIED | Report establishes no publication |
| Published releases | No local tags; `git ls-remote --tags origin` returned no tags at inspection; GitHub Releases queries inaccessible | **No published release verified in the inspected sources** | UNVERIFIED: gh GraphQL returned Forbidden and public REST endpoint returned HTTP 403 |

No empty Releases API response was obtained, so absence of GitHub Releases is **not** asserted. Git tag absence does not prove no Play distribution or other publication. [RELEASE.md](RELEASE.md) remains **NOT RELEASE-READY / BLOCKED** pending candidate-specific proof.

## 8. V1/V2 scope boundary

Current V1 is owner-only and offline-first: local friends are not authenticated users; settlements are owner assertions. Login, friend linking, mutual confirmation, shared ledgers and cloud sync remain **PROPOSED FUTURE**, not shipped/current acceptance. Unused enums/catalog entries and old shared-flow ideas are not features. No vendor, backend, framework or conflict design is selected. Permanent group/budgeting/bank/wallet/lending/interest/payment anti-goals remain unchanged.

## 9. Changelog maintenance protocol

- Add an Unreleased item only after inspecting the actual change/source and recording commit/evidence; distinguish a policy/doc change from an application fix.
- Create a versioned production entry only with approved actual publication evidence, correct version/date and tag/release/artifact references where available. GitHub prerelease/tag alone does not prove Play shipment; identify the distribution channel.
- Use commit dates for development and actual verified publication dates for releases; never fill unknown dates or hash/certificate/results from old reports.
- Group related changes without duplicate category entries. Record breaking applicationId, schema/migration, backup-format or history changes prominently with compatibility limits and backlog IDs.
- Preserve prior facts when later behavior changes; mark removed features rather than silently retaining them as current. Record corrected evidence explicitly.
- Keep unresolved gates visible until independently cleared; do not promote source/test-name/docs completion to runtime PASS or TASKS DONE.
- Refresh snapshot/remote/source and cross-document references after changes. Separate developer history from concise truthful user-facing Play release notes; no marketing/adoption/telemetry/compliance claims without evidence.

## 10. References and evidence limitations

Governing references: [AGENTS](../AGENTS.md), [PROJECT_STATE](PROJECT_STATE.md), [ARCHITECTURE](ARCHITECTURE.md), [RULES](RULES.md), [SECURITY](SECURITY.md), [PRD](PRD.md), [DESIGN](DESIGN.md), [TASKS](TASKS.md), [RELEASE](RELEASE.md), [historical RC report](RELEASE_CANDIDATE.md), [privacy](PRIVACY.md), [Play declarations](PLAY_DECLARATIONS.md), [brand](branding/PHITTOOS_BRAND_V1.md).

Current implementation pointers: [repository](../app/src/main/java/com/professor1416/phittoos/data/repository/PhittoosRepository.kt) add/edit/delete/recordRepayment/settleAllSameDirectionForFriend/restoreValidatedData; [TransactionEntity](../app/src/main/java/com/professor1416/phittoos/data/model/TransactionEntity.kt) paid/remaining/due helpers; [database](../app/src/main/java/com/professor1416/phittoos/data/db/AppDatabase.kt); [Add ViewModel](../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/AddTransactionViewModel.kt); [Home](../app/src/main/java/com/professor1416/phittoos/ui/screens/home/HomeScreen.kt); [backup](../app/src/main/java/com/professor1416/phittoos/export/PhittoosBackupManager.kt); [CSV](../app/src/main/java/com/professor1416/phittoos/export/PhittoosCsvExporter.kt); [reminders](../app/src/main/java/com/professor1416/phittoos/reminder); [theme](../app/src/main/java/com/professor1416/phittoos/ui/theme); [navigation](../app/src/main/java/com/professor1416/phittoos/ui/navigation/Screen.kt); [test sources](../app/src/test/java/com/professor1416/phittoos).

Exact inspected SHA: `5fea38283eeeaf6b16bc3d0c2f2ef4485c7ccd65`. Commit links point to actual objects in this checkout; short URLs identify unique inspected commits, not fabricated release tags. Feature milestone descriptions use inspected diffs; older package paths in those diffs differ from current paths. The selected history is incomplete by design. Local retained XML is ignored/nonportable evidence, lacks a release attestation and does not establish current regression or signed artifacts.

Tags were queried read-only and none returned; GitHub Releases access was unavailable (Forbidden/403), so publication absence cannot be concluded. Referenced historical chats were not retrieved because no conversation reader was available; they supplied no authoritative evidence. No adoption/download results, personal records or credentials were used. Only CHANGELOG.md changed in this documentation task; no tags/releases/builds/tests/commits/pushes were created or executed.
