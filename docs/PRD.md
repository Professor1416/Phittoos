# Phittoos V1 product requirements

## 1. Document control and product identity

| Field | Value |
| --- | --- |
| Product | Phittoos — Android-first, private, offline-first person-to-person money ledger |
| Exact approved tagline | **Tere mere hisaab… Phittoos!** |
| Document revision | 1.0; 2026-10-12 UTC |
| Inspected branch / commit | `work` / `4d389d99edaff36bf54d89713aefd758961c17e3` |
| Remote main | Same SHA observed through `git ls-remote origin refs/heads/main` |
| Document status | V1 requirements baseline with source observations and unresolved acceptance gates; not release certification |
| Ownership | Product approval remains with the repository owner; no named product-management owner established by inspected evidence |

This PRD defines WHAT and WHY for the approved V1 scope. [AGENTS.md](../AGENTS.md) governs work; [PROJECT_STATE.md](PROJECT_STATE.md) records current state; [ARCHITECTURE.md](ARCHITECTURE.md) explains HOW; [RULES.md](RULES.md) defines financial acceptance; [SECURITY.md](SECURITY.md) defines security boundaries and gates. A feature's presence does not certify end-to-end correctness.

| Evidence term | Meaning |
| --- | --- |
| APPROVED REQUIREMENT | Required V1 behavior from current task/product boundaries and engineering rules; may be unmet |
| IMPLEMENTED IN SOURCE | Identifiable current code path, not newly verified runtime behavior |
| PARTIAL / KNOWN GAP | Relevant implementation exists but required safeguards or behavior are incomplete |
| NOT VERIFIED | Runtime/device/recovery claim lacks sufficient evidence |
| PROPOSED FUTURE | Unapproved exploratory scope, excluded from V1 acceptance |
| SUPERSEDED HISTORICAL IDEA | Older proposal conflicting with approved owner-only V1; not a V1 requirement |

No Android build, runtime test, interview, performance benchmark or market study was run for this document. The baseline was clean and PRD.md absent. Source takes precedence for implementation facts; approved product policy remains an obligation where source is incomplete. Preserve IDs when updating; add new IDs rather than silently renumbering existing requirements.

## 2. Executive product summary

Phittoos helps a device owner record informal money lent to or borrowed from local friends, understand what remains outstanding and retain private repayment history. It supports multiple entries per friend, partial repayments, settlements, due information, owner-local reminders and user-controlled backup/export.

A friend is a local ledger entry, not an authenticated participant. Settlement records the owner's assertion about payment; no money moves and no friend agreement is verified. The value proposition is a clear private record that works without login or connectivity, with explicit limitations around external files, device loss and verification.

## 3. Problem statement

People can forget informal lending/borrowing, confuse partial with full repayment, or lose the reasoning behind amounts spread across messages and notes. Remembering whom to approach and reconstructing a private timeline can become awkward. These are product problem hypotheses, not independently measured demand or proof of superiority over competitors.

V1 addresses recording and recall: it does not resolve disputes, collect debts, verify another person's conduct or automate borrower contact. Reminders help the owner remember, rather than sending messages to a friend.

## 4. Target audience and personas

The primary intended audience includes students, early-career professionals, roommates and others managing recurring one-to-one informal exchanges in India. Android access, rupee amounts and a preference for private local records frame V1; these are product targeting assumptions, not validated demographics or an eligibility restriction.

| Illustrative scenario, not interview evidence | Context / motivation | Privacy and usability need |
| --- | --- | --- |
| Student with recurring small exchanges | Quickly capture several loans/borrowings between classes; remember partial returns | Minimal entry effort; no contact access or friend enrollment |
| Early-career professional | Reconstruct who owes what from scattered notes/messages | Clear direction, paid/remaining and dates; private notes |
| Roommate recording individual exchanges | Keep separate records for different people and occasions | Multiple one-to-one entries; never transform these into group splitting |
| Owner preserving records across devices | Make an informed copy and restore decision | Understand replacement, plaintext exports and external-copy retention |

No historical Rahul persona PDF was available. These scenarios are illustrative hypotheses; recruitment and usability feedback must validate them, including users outside this demographic and users relying on assistive technology.

## 5. Jobs to be done and product outcomes

| Job | Required V1 outcome / trace |
| --- | --- |
| When I lend, record who owes me, how much and why | Friend + LENT amount/date/optional note; FR-002–005 |
| When I borrow, remember what I owe | BORROWED kept distinct from LENT; FR-004/006 |
| When money is partially returned, update that exact entry | Cumulative paid and remaining without changing another entry; FR-008 |
| When an entry is cleared, keep understandable history | Settled row, timestamp and event retained; FR-009/012 |
| When a due date passes, recognize it privately | Due/overdue labels and qualified owner reminder; FR-015/016 |
| When preserving data, understand the backup decision | Restorable JSON versus viewing CSV; confirmed replacement and honest failures; FR-019–021 |

Successful outcomes mean observable ledger clarity and correct persistence, not externally proven repayment or guaranteed recovery.

## 6. Product principles

Capture should be quick without sacrificing direction, identity or validation. Show gross receivable and owed amounts alongside net; minimize mental arithmetic. Default to owner-controlled local data and no friend participation. Explain what was recorded, what remains and what failed without false success.

Corrections are allowed only before financial history makes direct rewriting unsafe. Keep destructive operations explicit. Support accessible labels, predictable offline behavior and clear loading/error states. Present limitations honestly: Android scheduling is approximate, exported files leave app control and local records are owner assertions. Universal error-free behavior is not promised.

## 7. V1 scope and non-goals

| Category | Boundary |
| --- | --- |
| IN SCOPE | Local onboarding/profile, friends, LENT/BORROWED records, dates/notes, balances/search/history, partial/full settlement, guarded same-direction bulk, eligible correction/deletion, due indicators, owner-local reminders, private reliability, celebration, themes, JSON replacement restore, CSV and local clear |
| OUT OF SCOPE — current V1 | Login/accounts, invitations/linking, friend verification/confirmation, shared participation, cloud sync, contacts permission, automatic borrower messaging/notifications |
| Permanent product anti-goals | Group bill splitting, personal budgeting/expense tracking, bank/wallet/lending service, interest/credit calculation, payment processing, automatic UPI payment recording, public financial reputation |
| PROPOSED FUTURE | Optional linking/shared records/consent/mutual confirmation/secure sync; separate decisions and authorization required (§17) |

Owner-only reliability is a local heuristic, not a credit score. Historical two-way verification, contact permission and shared flows described in the request are SUPERSEDED HISTORICAL IDEAS for V1; their original documents were unavailable for independent review. They do not become acceptance criteria through old terminology or unused enum values.

## 8. Primary user journeys

Routes come from [Screen.kt][routes]; all confirmations below are owner-side dialogs, never another friend's approval screen.

| Journey | Current path and expected result | Evidence / limitation |
| --- | --- | --- |
| First launch | `onboarding`: intro → name step → completion → `home`; returning launch uses stored completion | [Onboarding][onboarding]; skip controls also complete locally, no login |
| Add LENT | Home → `add_transaction` → select/create local friend → LENT, amount, occurrence date, optional note/due → save | [Add ViewModel][addvm]; new OPEN entry, creation activity; validation gaps |
| Add BORROWED | Same form, choose BORROWED → save owner obligation | Same evidence; no bank/payment action |
| Multiple entries | `friend_detail/{friendId}` → contextual add with friendId → independent entry | [Friend detail][detail]; preserve each amount/direction/date |
| Partial repayment | Friend detail → intended entry's repayment dialog → valid payment → paid/remaining refresh | [repository][repo], [partial tests][partial]; true overlap unverified |
| Individual full settlement | Select entry and owner confirmation, or record final repayment → settled history | [settlement tests][settlement]; no external proof of payment |
| Bulk settlement | Friend detail → eligible bulk action → owner confirmation → all that friend's OPEN entries settled | Same-direction only; mixed/empty refused |
| Mixed directions / zero net | Inspect separate entries; settle individually when directions differ | Net ₹0 must still show OPEN obligations; initial Home loading gap |
| Inspect history | Friend detail timeline and `activity` → creation/payment/settlement/reminder events | [Activity][activity]; due attention is not automatically a persisted event |
| Due/reminders | Inspect labels; Settings reminder toggle; OS notification when eligible/permitted | LENT 7/15/30 overdue stages; delivery/permission acquisition incomplete |
| Backup/restore | `settings` → “Back up your data” → provider; “Restore from backup” → select JSON → summary → “Restore & Replace” or Cancel | [Settings][settings]; replaces current data, not merge; provider/semantic/recovery gaps |
| CSV/settings | Settings → “Export as CSV”; edit profile, System/Light/Dark, reminders | CSV for viewing/sharing, cannot restore |
| Correct/delete | Friend detail selects eligible untouched OPEN entry; edit uses add route with transactionId; deletion confirms | [recovery tests][recovery]; paid/settled entries blocked by normal guards |
| Clear local data | Settings → “Clear all data” → confirmation or Cancel → onboarding on success | External copies retained; cross-system failure recovery unverified |

## 9. Functional requirements matrix

Every row is an **APPROVED REQUIREMENT** within owner-only V1. Must/Should/Could express product importance, not defect severity. Core integrity conditions remain mandatory regardless of priority. “Source” abbreviates IMPLEMENTED IN SOURCE; runtime acceptance remains NOT VERIFIED unless separately evidenced. These criteria define future QA, not tests executed here.

| ID | User-facing requirement | Priority | Approved scope | Observed status | Observable acceptance criteria | Evidence / rule |
| --- | --- | --- | --- | --- | --- | --- |
| FR-001 | Local onboarding/profile | Must | V1 local | Source; skip supported | Intro/name completion or skip reaches Home; name continuation rejects blank; relaunch preserves completion; no account created | [Onboarding][onboarding], [preferences][prefs] |
| FR-002 | Select/create local friend | Must | V1 local | Source | Nonblank trimmed name creates/selects friend; invalid/missing selection prevents transaction save; no enrollment/contact permission | [Add ViewModel][addvm], TX-001 |
| FR-003 | Friend list/search | Must | V1 local | Source | Name search filters matching local friends; selecting correct ID opens its ledger; clearing search restores list | [Home ViewModel][homevm] |
| FR-004 | Explicit LENT/BORROWED | Must | V1 ledger | Source | Save chosen direction; LENT raises receivable, BORROWED raises owed; labels explain who owes whom | FIN-001/002; [accounting tests][accounting] |
| FR-005 | Amount/date/note validation | Must | V1 ledger | PARTIAL / KNOWN GAP | Finite positive rupee input under approved paise/range policy; optional note; past/today occurrence accepted, future day refused; optional today/future due accepted in normal form | MON/TX/DATE rules; [Add ViewModel][addvm] |
| FR-006 | Accurate Home summary | Must | V1 local | Source; loading gap | Gross remaining receivable/owed and net match OPEN entries; zero net with OPEN entries never labeled all settled after load | FIN-003–006; [Home][home] |
| FR-007 | Friend-specific detail | Must | V1 local | Source | Multiple entries retain distinct IDs/direction/date; friend totals match its OPEN remaining; other friends excluded | [detail][detail], [repository][repo] |
| FR-008 | Partial repayment | Must | V1 ledger | Source; boundary/overlap NOT VERIFIED | Valid payment updates only target cumulative paid/remaining; partial stays OPEN; overpayment rejected with remaining guidance; history matches amount | FIN-007/008/014; [partial tests][partial]; SEC-V1-003 |
| FR-009 | Full individual settlement | Must | V1 ledger | Source; overlap NOT VERIFIED | Final repayment or settle sets paid=amount, settled time and CONFIRMED once; repeat adds no effect/event; unrelated entries untouched | FIN-009/010/012; [settlement tests][settlement] |
| FR-010 | Same-direction bulk | Must | V1 ledger | Source; atomic failure NOT VERIFIED | Nonempty all-LENT or all-BORROWED OPEN set settles for selected friend; correct events retained; unrelated rows unchanged | FIN-011/018; SEC-V1-004 |
| FR-011 | Mixed/zero-net protection | Must | V1 ledger | Source; UI loading gap | Mixed bulk refused without writes; equal opposite entries remain OPEN until individually settled; net alone never closes entries | FIN-006/011; [settlement tests][settlement] |
| FR-012 | Understandable history | Must | V1 local | Source; imported associations partial | Creation/payment/settlement activity matches target and occurrence/event dates; ordinary settlement retains prior records | FIN-012–014; [Activity tests][activitytests] |
| FR-013 | Eligible correction | Must | V1 ledger | Source; event eligibility gap | Only untouched OPEN can edit; same ID/friend; amount/direction/note/dates and creation event agree; paid/settled rejects without change | TX-002/003; [recovery tests][recovery] |
| FR-014 | Eligible deletion | Must | V1 ledger | Source; event eligibility gap | Explicit confirmation deletes only untouched target and its activities; Cancel/repaid/settled leaves ledger unchanged | FIN-013; TX-002/003 |
| FR-015 | Due/overdue clarity | Must | V1 local | Source; clock freshness NOT VERIFIED | No-due/today/upcoming/overdue distinguished; settled/zero-remaining not actively overdue; occurrence date unchanged by payment | DATE rules; [date tests][dates] |
| FR-016 | Owner-local reminders | Should | V1 local | PARTIAL / KNOWN GAP | Only qualifying OPEN LENT notifies owner; disabled/denied safely suppresses; 7/15/30 stage/history truthful; no friend message; no exact-time promise | REM rules; [reminder tests][reminders]; SEC-V1-013/014 |
| FR-017 | Private reliability explanation | Should | V1 local | Source; wording limitations | LENT-only owner insight; insufficient history labeled; BORROWED does not penalize friend; missing delay evidence never claimed verified punctuality | [Reliability][reliability], ACT-001 |
| FR-018 | Settlement celebration | Could | V1 local | Source; device visuals NOT VERIFIED | Successful settlement/full repayment plus no OPEN entries and active screen triggers celebration; no trigger on delete/cancel/zero net | BULK-003; [celebration tests][celebration] |
| FR-019 | Restorable JSON backup | Must | V1 data | PARTIAL / KNOWN GAP | User-selected plaintext copy preserves supported ledger/history fields; success/last-backup time only after completed stream; cancellation/failure never claims saved | BACK-004/005; SEC-V1-010; [backup][backup] |
| FR-020 | Confirmed validated replacement | Must | V1 data | PARTIAL / KNOWN GAP | Summary warns existing data replaced; Cancel leaves state; invalid/inconsistent backup rejected before destruction; supported restore preserves IDs/balances/history with recoverable failure | FIN-017/018; BACK-001–003; SEC-V1-007/009 |
| FR-021 | CSV viewing export | Should | V1 data | Source; provider/formula gaps | Readable supported columns and escaping; explicitly non-restorable; no success on null/failed stream; disclosed external-copy risks | BACK-005/006; [CSV][csv], [Settings][settings] |
| FR-022 | Local settings/themes | Should | V1 local | Source | Profile/reminder/System-Light-Dark choices persist across relaunch; semantic money colors remain readable in each mode | [preferences][prefs], [theme tests][themes] |
| FR-023 | Explicit local clear | Must | V1 data | Source; failure recovery NOT VERIFIED | Cancel preserves data; confirmation removes local ledger/preferences, clears notifications/periodic work and returns onboarding; never claims external files erased | FIN-013; [Settings ViewModel][settingsvm] |
| FR-024 | Empty/loading/error recovery | Must | V1 local | PARTIAL / KNOWN GAP | Empty ledger and no-search-match distinguishable; loading never asserts all settled; failed save/import/export shows actionable failure, no false saved state; retry avoids duplicate effects | UI-002; [Home][home], [Add ViewModel][addvm] |

## 10. Accounting business requirements

The acceptance contract is [RULES.md §3](RULES.md#3-financial-invariants), FIN-001 through FIN-018. Product translation:

| FIN trace | Required owner-visible behavior / FR trace |
| --- | --- |
| FIN-001–005 | Clearly separate receivable and owed; use valid paid/remaining values from OPEN entries; net subtracts owed. FR-004/006/007 |
| FIN-006 | Zero net is not no debt: retain every OPEN entry and its action. FR-006/011/024 |
| FIN-007–010 | Cumulative partial payment affects one target; remaining never negative; final settlement occurs once; repeats do not fabricate money/history. FR-008/009 |
| FIN-011 | Bulk means one direction for one friend, never netting mixed obligations. FR-010/011 |
| FIN-012/013 | Normal settlement keeps history; eligible deletion, restore replacement and local clear are explicitly distinct destructive actions. FR-012/014/020/023 |
| FIN-014–016 | Correct friend/transaction/direction associations; invalid numbers/IDs cannot silently enter or overwrite the ledger. FR-002/005/008/012/020 |
| FIN-017/018 | Validated recoverable restore and safe upgrades preserve balances and history; no partial replacement presented as success. FR-019/020 |

Synthetic example: LENT ₹500 with ₹200 paid leaves ₹300 receivable; BORROWED ₹100 leaves ₹100 owed; net is +₹200. Settling the remaining LENT ₹300 leaves the BORROWED entry OPEN and net −₹100. This is an arithmetic illustration, not a test executed here.

Current money storage is Double, not integer paise. RULES.md MON rules define current tolerances and precision obligations; this PRD does not change them or invent a maximum amount. Creation/range/precision, true concurrency, imported semantics, history-based correction eligibility and recovery gaps remain unmet/unverified acceptance, despite normal-path source implementations.

## 11. Interaction and content requirements

The exact locked tagline is **Tere mere hisaab… Phittoos!** [Brand specification](branding/PHITTOOS_BRAND_V1.md) locks Slate `#0F172A`, White `#FFFFFF`, Emerald `#10B981`, Orange `#F97316` for branding/logo assets. These do not override the accessible UI theme. Preserve emerald/green LENT, orange/coral BORROWED, red overdue/error and amber partial/caution, paired with words/icons rather than color alone.

[Home source][home] uses **“You'll get back”** and **“You owe”** for totals; net copy includes “You'll receive…”, “You need to pay…”, “Overall balance ₹0” and “All settled up (₹0)”. Only the tagline is locked verbatim; other wording is verified current copy, subject to authorized clarity changes. All-settled requires loaded data and no OPEN entries; source currently lacks that loading gate.

OPEN means outstanding ledger record; CONFIRMED is displayed as settled, not authenticated friend confirmation. Explain original, paid and remaining amounts in repayment context. Due Today/Upcoming/Overdue are presentation meanings from due classification, not a promised identical literal on every card; historical settled due dates must not imply active debt. Owner reminder wording must not imply delivery to the friend. Reliability must say it reflects “your records” and is private; missing delay data is not proof of punctuality. Destructive dialogs must explain replacement/removal and cancellation. Saving/errors must not obscure whether a financial mutation completed.

## 12. Non-functional requirements

| Area | Required acceptance / evidence limit |
| --- | --- |
| Android support | Declared min SDK 24, target 36 in [app build](../app/build.gradle.kts); validate core flows on representative supported devices/API levels before release, not just compile support |
| Offline use | Airplane-mode onboarding, capture, balances/history, repayments and settings work without account/network; SAF provider availability may differ |
| Responsiveness | Fast capture is a product goal; timed usability sessions must report elapsed time/device/context. No latency benchmark or numeric threshold is invented |
| Persistence/recovery | Relaunch retains committed records/preferences; failed/interrupted actions leave clear recoverable state without duplicate money/events; cross-store restore evidence missing |
| Financial integrity | Assert target isolation, totals/status/history after normal/rejected/overlapping operations; FIN rules govern |
| Privacy/permissions | No app-controlled V1 ledger network or unnecessary permissions; merged release and real-device boundaries reviewed; exports explicitly external/plaintext |
| Accessibility | At least practical 48dp targets, legible contrast, labeled financial state, large text without hidden amounts/actions, TalkBack navigation and both themes; measurements/device checks required, no compliance certificate |
| Reminders | Safe disabled/denied behavior; owner-only qualified content; Android approximate background schedule explained; delivery/crash/DST verification required |
| Loading/errors | Unknown state cannot imply settled; canceled/failed provider actions cannot advance successful backup status; failure messages give safe next step |
| Maintainability/upgrades | Stable requirement trace, targeted/full regression and historical database upgrades preserve records; no destructive migration fallback; no weakened tests |

## 13. Security, privacy and data expectations

Owner-controlled local Room/preferences are observed; app PIN/biometrics or application-level encryption are not implemented. Device access/sandbox assumptions do not protect a compromised or unlocked device universally. Local friend notes and reliability remain private V1 records, not shared facts.

The implemented JSON/CSV export flows require explicit user selection and can place plaintext on a cloud provider. OS backup/device-transfer behavior remains separately unverified; local-first does not guarantee no data transfer on every device. Notifications can expose friend/amount under device lock-screen settings. Import is untrusted input; validation must precede replacement and reject financial inconsistencies. Local clear/uninstall does not erase retained external copies, and no guaranteed total recovery or forensic erasure is claimed.

[SECURITY.md](SECURITY.md) controls SEC-V1-001–016 specify the detailed boundaries. Privacy/Play wording must match actual exports/permissions before release; current draft-reminder sharing/permission wording discrepancies are recorded there, not fixed here. No DPDP, Play-policy, WCAG or audited-security compliance claim follows from this PRD.

## 14. Edge cases and rejection criteria

| Case | Required visible behavior / unchanged-state expectation | Current evidence / missing verification |
| --- | --- | --- |
| Zero/negative/nonfinite/overflow/overprecision money | Explain invalid amount; no row/event; approved precision/range policy | Creation checks incomplete; edit/repay finite guards present |
| Partial/exact final/overpayment | Show cumulative paid/remaining; final settles once; too much rejected with remaining amount | Partial suite/source; numerical boundaries and real overlap require checks |
| Repeated settlement | No new payment/event/time; safe already-settled outcome | Sequential coverage; overlapping replay NOT VERIFIED |
| Mixed bulk or no OPEN entries | Refuse/disable without changes; individual settlement offered where applicable | Repository guard present |
| Equal opposite OPEN amounts | Net ₹0 with separate OPEN obligations; never auto-settle | Normal accounting present; initial loading assertion unresolved |
| Future occurrence/past normal-form due date | Clear date error; allow past/today occurrence and absent/today/future due | Form guards; direct add due guard gap; import historical overdue compatibility separate |
| Missing/deleted friend/transaction | Missing-target message/fallback, no other record modified | Normal guards/navigation lookup; device/race behavior NOT VERIFIED |
| Paid/settled/corrupt-history edit/delete | Refuse without rewriting/removing historical money | Aggregate guards; history-query/corrupt aggregate gap |
| Corrupt/inconsistent/unsupported backup | Explain rejection; retain current data before replacement | Structural tests; full semantics/version/commit recovery gap |
| Provider cancellation/null/throwing stream | No false success or timestamp; retry path; no ledger mutation by export | Cancellation/throws handled; null-output success gap |
| Loading/unknown data | Neutral loading state rather than “All settled”; then accurate result | Home initial zero rendering gap |
| Reminder disabled/permission denied | No sent-history claim and no borrower contact; ledger still usable | Checks present, acquisition/device delivery unverified |
| Historical DB upgrade/failure | Preserve IDs/balances/history or safe explicit failure; never silently wipe | Registered v1→4 chain, limited v2/v3 fixtures, full chain NOT VERIFIED |

## 15. Product success metrics

These are proposed measurement definitions, not achieved results, installed telemetry or release thresholds. Start with consented usability sessions/synthetic ledgers and voluntary aggregate feedback; retention measurement needs an explicitly approved privacy-respecting method. Do not silently add analytics or collect friend names/amounts/notes/backups.

| Proposed measure | Definition / collection question |
| --- | --- |
| Time to first successful transaction | Elapsed intro start to committed first entry in observed session; where do users hesitate? |
| Entry completion rate | Successfully saved intended entries / attempted entry tasks; separate validation errors and abandonment |
| Active ledger utility | Voluntarily reported distinct friends with useful records and recurring use; no installed activity collection assumed |
| Balance clarity | Participants correctly identify receivable, owed and zero-net OPEN obligations in synthetic scenarios |
| Partial repayment success | Intended-target task completes with correct cumulative paid/remaining/history; reject overpayment |
| Backup/restore success | Completed export yields readable supported JSON and matching restored ledger in isolated QA; false-success attempts count as failure |
| Repeat utility/retention | Consented follow-up on whether users still use the ledger; timeframe/target to be agreed after baseline research |
| Satisfaction/accessibility | Task feedback and barriers, including TalkBack/large-text users; no invented survey score |

No market validation, adoption, conversion rate, superiority claim or benchmark is asserted. Numeric KPI targets remain open pending baseline evidence and owner approval.

## 16. Release acceptance and open risks

Product scope coverage is not release approval. Required evidence must be tied to current source/artifact, command, device/API where applicable and actual outcome:

- Current targeted financial tests and full regression pass; historic retained **268/269** (one dashboard assertion failure) is not green, and isolated success is insufficient.
- Monetary input/range/precision, target isolation, repayment/settlement repeats/overlap, mixed bulk and history assertions satisfy FIN/MON rules.
- Validated backup semantics, provider null/write failures, export consistency and interrupted replacement/recovery verified; Room/preferences split currently prevents a blanket atomic guarantee.
- Complete historical upgrade coverage preserves IDs/directions/balances/events; limited existing v2/v3 fixtures do not certify v1 or every real database.
- Fresh-install notification permission acquisition, denial, owner-only delivery, lock-screen privacy, background lifecycle and timezone/device-transfer checks evidenced.
- Privacy/Play declarations aligned with implementation and external plaintext copies; production signing, merged release permissions/components and installable artifacts verified.
- Representative physical-device, TalkBack, large-text, themes and loading/error-state QA performed; JVM source tests alone do not establish this.

**P0 / NO RELEASE** for reproduced corrupted ledger/history, wrong-target money mutation, duplicate financial effects, unsafe migration or partial restore, regardless of build success. Serious validation/recovery/verification gaps remain unresolved release gates, not fabricated exploits. No new runtime reproduction, regression or release certification accompanies this document. See RULES.md §14 and SECURITY.md §15 for escalation.

## 17. Future V2 — proposed, not approved

Potential optional friend linking, shared records, consent, mutual confirmation and secure sync are **PROPOSED FUTURE**, not implemented or approved here. Questions include whether linking is wanted, what confirmation means, who can edit/view shared versus private information, revocation, identity/recovery, replay/idempotency, offline conflict/history handling and data retention/deletion.

Future work requires explicit product authorization, research and trust/security decisions. No vendor/backend/framework/conflict algorithm is selected. These concepts have no FR row or V1 release dependency. Permanent anti-goals remain unchanged.

## 18. Dependencies, assumptions and open decisions

V1 depends on Android local persistence, the owner's accurate input, device access/storage and Android scheduling/notification settings. SAF recovery additionally depends on an available provider and an actual usable copy; device loss without a copy can mean loss of the ledger. Friends need no installation or participation.

Approved behavior is the owner-only scope and acceptance above. User research must validate audience, task language, entry effort and metric targets. Engineering must establish safe monetary range/precision with legacy compatibility, full restore/upgrade/recovery and device gates. These are decision/verification boundaries, not authorization to redesign storage or introduce features.

Onboarding skip is current source behavior; its interaction with profile-dependent backup validation needs verification rather than assuming every skipped profile is export/restore-ready. JSON presently backs up profile/onboarding/reminders but not theme/last-backup timestamp; this PRD requires honest supported-field disclosure, not an unapproved format expansion. Historical backup compatibility, accessibility/brand device rendering and public disclosure accuracy need evidence. Nothing here schedules implementation or duplicates TASKS.md.

## 19. Traceability and source references

Inspected baseline: `4d389d99edaff36bf54d89713aefd758961c17e3`. Earlier five documents have earlier snapshot SHAs; Git comparison from `67c777a` to this baseline shows documentation-only changes, not fresh application verification. Refresh FR statuses when source changes; retain evidence/limitations and record revision dates.

Sources read include [AGENTS.md](../AGENTS.md), [PROJECT_STATE.md](PROJECT_STATE.md), [ARCHITECTURE.md](ARCHITECTURE.md), [RULES.md](RULES.md), [SECURITY.md](SECURITY.md), [brand](branding/PHITTOOS_BRAND_V1.md), [strings](../app/src/main/res/values/strings.xml), [routes][routes], linked screens/ViewModels/repository/export/domain paths and existing tests. FR matrix links FIN and SEC IDs to critical requirements; these are acceptance references, not new tests or guarantees. Additional tests: [transaction dates](../app/src/test/java/com/professor1416/phittoos/TransactionDateBackdateTest.kt), [settings/export](../app/src/test/java/com/professor1416/phittoos/SettingsAndExportTest.kt), [reliability](../app/src/test/java/com/professor1416/phittoos/ReliabilityEngineTest.kt), [dashboard assertion](../app/src/test/java/com/professor1416/phittoos/ExampleRobolectricTest.kt), [onboarding accessibility](../app/src/test/java/com/professor1416/phittoos/OnboardingAccessibilityTest.kt).

Historical files `phittoos-prd(1).pdf`, `phittoos-user-persona.pdf`, `phittoos-lending-tracker-ux-flow.pdf`, `phittoos-tech-stack(1).pdf` and `Phittoos_Master_Roadmap(1).md` were not found in the workspace; no original contents were invented. No conversation reader was available to retrieve the referenced chats; preview/request descriptions remain historical context only. The described old shared-confirmation/contact concepts conflict with approved V1 and remain excluded. No personal ledger, raw user backup or credential values were accessed.

This task performed documentation/Git/source checks only. Runtime/device/performance/market results remain NOT VERIFIED. The retained suite result is prior evidence, not a current regression run. Existing financial/security gaps and other-document inconsistencies are preserved; no other document or application code was modified.

[routes]: ../app/src/main/java/com/professor1416/phittoos/ui/navigation/Screen.kt
[onboarding]: ../app/src/main/java/com/professor1416/phittoos/ui/screens/onboarding/OnboardingScreen.kt
[addvm]: ../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/AddTransactionViewModel.kt
[home]: ../app/src/main/java/com/professor1416/phittoos/ui/screens/home/HomeScreen.kt
[homevm]: ../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/HomeViewModel.kt
[detail]: ../app/src/main/java/com/professor1416/phittoos/ui/screens/frienddetail/FriendDetailScreen.kt
[activity]: ../app/src/main/java/com/professor1416/phittoos/ui/screens/activity/ActivityScreen.kt
[settings]: ../app/src/main/java/com/professor1416/phittoos/ui/screens/settings/SettingsScreen.kt
[settingsvm]: ../app/src/main/java/com/professor1416/phittoos/ui/viewmodel/SettingsViewModel.kt
[prefs]: ../app/src/main/java/com/professor1416/phittoos/data/preferences/UserPreferences.kt
[repo]: ../app/src/main/java/com/professor1416/phittoos/data/repository/PhittoosRepository.kt
[backup]: ../app/src/main/java/com/professor1416/phittoos/export/PhittoosBackupManager.kt
[csv]: ../app/src/main/java/com/professor1416/phittoos/export/PhittoosCsvExporter.kt
[reliability]: ../app/src/main/java/com/professor1416/phittoos/domain/ReliabilityEngine.kt
[accounting]: ../app/src/test/java/com/professor1416/phittoos/PhittoosAccountingEngineTest.kt
[partial]: ../app/src/test/java/com/professor1416/phittoos/PartialRepaymentEngineTest.kt
[settlement]: ../app/src/test/java/com/professor1416/phittoos/OfflineSettlementFlowTest.kt
[recovery]: ../app/src/test/java/com/professor1416/phittoos/ContextualAddAndWrongEntryRecoveryTest.kt
[activitytests]: ../app/src/test/java/com/professor1416/phittoos/ActivityReminderHistoryTest.kt
[dates]: ../app/src/test/java/com/professor1416/phittoos/DueDateOverdueEngineTest.kt
[reminders]: ../app/src/test/java/com/professor1416/phittoos/SmartReminderEngineTest.kt
[celebration]: ../app/src/test/java/com/professor1416/phittoos/SettlementCelebrationTest.kt
[themes]: ../app/src/test/java/com/professor1416/phittoos/ThemePreferenceTest.kt
