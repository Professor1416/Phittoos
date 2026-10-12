# Phittoos V1 Google Play release runbook

## 1. Document control and release status

| Field | Inspected state |
| --- | --- |
| Repository | Professor1416/Phittoos |
| Branch / exact source SHA | `work` / `113dfb31e218fd947d650ba944aec9462a69d342` |
| Remote main | Same SHA observed through `git ls-remote origin refs/heads/main` |
| Revision / inspection date | Runbook 1.0 / 2026-10-12 UTC |
| Initial state | Clean staged/unstaged/untracked repository; RELEASE.md absent |
| Audience | Developer, QA, security/privacy reviewer, release operator and approving owner |
| Current release readiness | **NOT RELEASE-READY / BLOCKED** — current safety, regression, signed-artifact, device and Console proof missing |

This is a procedure for a future separately authorized release, not release approval or permission to execute commands below. Only this document was created. No build, test, signing/key access, device/URL/Console check or publication occurred here.

Use **VERIFIED** only with exact baseline/candidate evidence; **FAILED** for executed failure; **NOT RUN** for unexecuted steps; **NOT APPLICABLE** only with documented applicability reasoning. Source observation and historical reports are separate from executed acceptance. All procedure checkboxes are initially unchecked; no default GO. [TASKS.md](TASKS.md) statuses remain unchanged.

## 2. Product and release scope

**Phittoos — Tere mere hisaab… Phittoos!** V1 is an offline-first, single-user Android ledger for local friends, LENT/BORROWED records, balances, partial/final repayment, individual/same-direction settlement, due information, owner reminders/history/reliability, celebration, settings/themes, JSON replacement restore and CSV export.

No login, backend/cloud sync, friend linking/mutual confirmation, borrower verification/notification, group splitting, budgeting, banking/wallet/interest or UPI/payment processing. Settled means the owner's recorded assertion, not proven payment. External plaintext SAF copies can reside with a user-selected provider; this is not automatic sync.

Product implementation, release verification and Play publication are different milestones. This runbook covers V1 only. No code fixes, credentials, artifact signing/building, uploading/submission or publication is authorized by writing it. Owner approval must explicitly name the production action/track/candidate.

## 3. Release roles and approval authority

| Responsibility | Obligation |
| --- | --- |
| Developer | Resolve authorized scoped gaps; preserve financial invariants; supply reproducible source/build/test evidence |
| QA | Independently exercise positive/rejected/race/recovery/device scenarios; report exact results and missing checks |
| Security/privacy reviewer | Review candidate permissions/components, imports/exports, dependencies, signing boundaries and truthful disclosures |
| Release operator | Validate artifacts/certificates/version, prepare track/Console records and carry out only approved actions |
| Final owner | Accept evidence-based conditional dispositions; approve go/no-go and separately authorize upload/rollout/publication |

These are responsibilities, not assigned people. An agent may prepare/review evidence within scope but cannot independently approve publication. Independent checker review must not treat developer assertion or historical checklist as proof. Missing credentials/device/Console access is a specific prerequisite to resolve securely, never a reason to fabricate readiness.

## 4. Release identity and versioning

Actual values from [app/build.gradle.kts][build]:

| Identity | Current declaration |
| --- | --- |
| applicationId / namespace | `com.professor1416.phittoos` / `com.professor1416.phittoos` |
| versionName / versionCode | `1.0.0` / `1` |
| minSdk / targetSdk | `24` / `36` |
| compileSdk | Release 36, minor API level 1 |
| Java source/target compatibility | 11; this is not proof of the required/used build JVM |

- [ ] Check final artifact identity/version against this source and approved candidate record (PHI-V1-033/040).
- [ ] Keep the production application identity locked. Any proposed change requires separate approval and data/installation impact assessment.
- [ ] Use a versionCode acceptable to the current Console and higher for a new update; never assume source code 1 remains usable after upload. Record approved versionName/code before building.
- [ ] Verify target SDK and other current Play requirements at submission time; declared 36 is not automatic compliance.
- [ ] Same-package upgrade requires compatible signing identity and retained data/migrations. Prior `com.aistudio.phittoos.mhzrtp` installs are a different package; do not claim in-place upgrade. A verified JSON backup may support manual transfer, not guaranteed compatibility/recovery.

No configuration change is performed here.

## 5. Release readiness gate register

Recounted [TASKS.md](TASKS.md): **48 IDs PHI-V1-001–048; 35 YES (including final decision), 13 CONDITIONAL; 17 OPEN, 30 VERIFY, 1 BLOCKED, 0 DONE**. Thus 34 other mandatory prerequisites precede mandatory final decision 048. Source existence/completed documentation clears none.

| Area | Task suffixes (PHI-V1-) | Clearing evidence |
| --- | --- | --- |
| Baseline/regression | 001–002,038–039 | Verified runner/clean build, explained failure, current full/targeted counts, actual device smoke |
| Accounting | 003–010 | Authoritative amount/precision, target/history, actual overlap, replay, tolerance, direction/zero-net assertions |
| Persistence/export | 011–020 | Crafted-input rejection, version/resource policy, rollback/cross-store recovery, coherent snapshots/provider truth, CSV disposition, complete legacy upgrades/enums |
| UX/reminders | 021–032 | Loaded UI/error/busy truth, search/dates, permission/owner disclosure, reminder history, themes/accessibility/insets/brand/celebration |
| Security/privacy | 033–037 | Candidate merged permissions/components, platform transfer, accurate policies, deletion boundaries, resolved dependency/license/signing review |
| Store/artifact/device/operations | 040–047 | Current policy/Console prerequisites, production-signed validation, install/upgrade/recovery, live URLs, declarations/listing, recovery procedure |
| Final decision | 048 | Independent completed evidence review and explicit owner decision; **BLOCKED** |

Mandatory YES items: **001–012,014–017,019–021,023,026,033,035,037–048**. Conditional: **013,018,022,024–025,027–032,034,036**. Mandatory entries require their stated clearing evidence. Every conditional entry requires a recorded verified result or justified applicability/impact disposition accepted by reviewers/owner; CONDITIONAL is not silent exemption. Safety obligations and any reproduced P0 cannot be waived by this classification. Consult each §13 TASKS acceptance contract; do not copy or substitute it with a vague checklist.

Before proceeding to submission, refresh these counts/statuses against actual backlog. Any required gate lacking named-candidate evidence prevents GO. Do not change backlog statuses in this documentation task.

## 6. Financial integrity acceptance

Use synthetic multi-friend ledgers and actual production-equivalent Room/database/activity dependencies. Inspect persisted target/unrelated records and event counts, not just UI text/return value. Independent reviewer compares assertions to [RULES FIN-001–018](RULES.md#3-financial-invariants), PRD FR-004–014 and SEC-V1-001–006.

- [ ] LENT remaining is receivable, BORROWED remaining owed; only OPEN contributes; gross totals and net are correct after every supported mutation.
- [ ] Equal LENT/BORROWED OPEN amounts show zero net but retain obligations/actions; loading cannot prematurely claim all settled.
- [ ] Partial payments accumulate only on intended ID; remaining stays nonnegative; final paid=amount, CONFIRMED and settledAt/event correct once.
- [ ] Invalid/nonfinite/negative/zero/overflow and unapproved precision reject before writes; clamping/display rounding is not validation.
- [ ] Overpayment rejects without state/events; verify below/at/above current epsilon **0.0001** and full threshold **amount−0.005**, preserving approved contract rather than widening tolerances.
- [ ] Individual/repeated/bulk calls have no duplicate effects; test real overlap, not sequential tests named concurrent.
- [ ] Bulk accepts only nonempty same-direction OPEN entries for one friend; mixed refuses without changes; net alone never triggers closure.
- [ ] Ordinary settlement retains transaction/history; untouched correction preserves ID/friend and creation event, paid/settled/history-corrupt entries reject; deletion removes only explicitly eligible target/events.
- [ ] Wrong/missing target and imported event association cannot alter unrelated friends/directions/history.

**NO-GO:** reproduced wrong-target/duplicate financial effect, accepted invalid money, wrong balance/history, unsafe restore/migration or lost records. Treat reproduced financial corruption as **P0 / NO RELEASE** regardless of build totals. Unverified scenarios remain critical gates, not invented successful exploits. No amount representation change is authorized here.

## 7. Backup, restore and upgrade safety

PHI-V1-011–020/036/042 govern; JSON is restorable supported-format data, CSV is viewing/sharing only. Never advise clearing/uninstalling a real ledger to test recovery before verifying a usable copy and obtaining specific authorization.

- [ ] Test complete supported JSON round trip against IDs/directions/amounts/paid/status/times/history/settings subset; current backup omits theme/last-backup time. Include skipped-onboarding/blank-profile compatibility.
- [ ] Reject crafted duplicate/invalid IDs, orphan/cross-friend events, bad enums/amounts and inconsistent paid/status/settledAt/timestamps before replacement; approved legacy/version handling evidenced.
- [ ] Exercise unsupported/ambiguous versions, bounded oversized/malformed inputs and provider read failure; no uncontrolled resource stress on real data.
- [ ] Verify confirmed replacement and Cancel behavior; inject clear/insert/commit failure, process interruption and restart. Existing pre-validation rejection does not prove mid-write rollback.
- [ ] Verify database rollback separately from SharedPreferences recovery: Room transaction cannot roll back separate apply writes. Reconcile notification/work state after restore/clear.
- [ ] Test export during mutation/clear/restore for coherent snapshot; null/throwing/write/flush/close output must not report success or advance backup time.
- [ ] Review CSV formula-leading names/notes with approved consumer policy; quoting alone is not formula safety. Explain external plaintext retention.
- [ ] Exercise actual supported v1/v2/v3→v4 databases, including corrupt/legacy enums and FK/history assertions. All migrations registered, no destructive fallback; current limited v2/v3 fixtures/exportSchema=false do not certify full chain.
- [ ] Verify signed same-identity upgrade, reinstall/restore and different-package transfer separately on disposable data; do not promise recovery without a verified valid backup.

Record pre/post synthetic ledger comparison and failure/recovery outcome. A success snackbar is not evidence a file was written or replacement was coherent.

## 8. Reproducible build and test procedure

**All commands in this runbook are future examples, NOT RUN and not validated as executable in this task.** Execute only in an authorized verified build environment; missing tools are prerequisites, not permission to install during documentation. Preserve underlying exit codes and actual results.

1. Capture branch/HEAD/remote/status and candidate source. Preserve unrelated work; obtain a clean controlled checkout through approved workflow, never discard user changes.
2. Inventory runner files. This snapshot tracks only [gradle-wrapper.properties][wrapper], distribution **9.3.1**; no tracked gradlew/gradlew.bat/wrapper JAR. Use an existing verified compatible Gradle runner, or separately authorize reproducible runner preparation. Do not assume `./gradlew` works.
3. Record Gradle/AGP/JDK/SDK/build-tools versions and installed Android release36/minor1 compile platform. Source Java 11 is not a build-JVM selection; earlier JDK21 environment reports are historical. Verify compatibility and distribution/dependency trust.
4. Record declared/currently resolved dependencies and local test/build input; absence of tracked GitHub Actions does not establish absence of external checks.
5. Review clean's effects: `clean` removes generated outputs/logs; preserve required prior evidence and user artifacts before using it. Tests can write the tracked greeting screenshot—inspect changes afterward.
6. With verified `gradle` runner, from repository root, use future checks such as:

```bash
gradle --version
gradle :app:tasks --all
gradle clean :app:assembleDebug :app:testDebugUnitTest :app:lintRelease
```

7. Run relevant targeted suites (for example `gradle :app:testDebugUnitTest --tests 'com.professor1416.phittoos.PartialRepaymentEngineTest'`) plus added actual-overlap/import/rollback/migration cases under authorized test work. Targeted PASS does not replace full suite.
8. Run `gradle :app:connectedDebugAndroidTest` only with an actual supported attached emulator/device; identity-only existing instrumentation is insufficient. Record unavailability as NOT RUN, not PASS.
9. AGENTS also requires `compile_applet` for code tasks. It is an external helper not tracked here; execute if actually available and relevant or report exact missing capability. Native assemble establishes its own result, not an invented helper PASS.
10. Investigate every failure/warning relevant to financial/security gates; no weakened assertions, disabled tests or relaxed tolerances. Retain complete full-regression result after final code changes. Lint/source review is not dependency/security certification.

For each check record SHA, cwd, exact invocation, environment, exit status, executed/pass/fail/error/skip counts, logs/XML and artifact mapping. The historical 268/269 and older 215-test report cannot populate a current PASS row.

## 9. Production signing and key security

Observed [build configuration][build]: release signing exists only when KEYSTORE_PATH (default root my-upload-key.jks) points to an existing file; keyAlias is **upload**, passwords come from STORE_PASSWORD/KEY_PASSWORD. Otherwise no release signingConfig is found; never infer signed output from task success. Debug uses a separate debug signing config and is not production proof. No actual production key/file/binding availability was checked here.

Under Google Play App Signing, distinguish the **upload key** authenticating uploaded bundles from the **app signing key** signing delivered APKs. Delivered Play APK certificates can differ from a locally upload-key-signed test APK. Verify configured certificates/lineage/track, not assumed equality. Installing over an app signed with an incompatible different certificate may fail; never uninstall a user's ledger as a shortcut.

- [ ] Owner chooses authorized signing/Play App Signing process and verifies public certificate fingerprints in the actual Console.
- [ ] Store private production key outside repository, restrict access, maintain encrypted controlled backup/recovery and record responsible custodians without passwords.
- [ ] Bind required secrets through approved secure environment/local tooling; avoid command-line password arguments, shell-history literals, environment dumps, verbose secret logs or copying keys into evidence.
- [ ] Inspect alias/type/expiry and public certificate using trusted tooling with interactive/secure password acquisition, e.g. `keytool -list -v -keystore "$UPLOAD_KEYSTORE_PATH" -alias upload`. The path variable is a placeholder for authorized local configuration; no password value/example is supplied.
- [ ] Review leak response/upload-key reset and app-signing-key controls with current Play guidance; they are different procedures. Do not promise arbitrary key replacement/update compatibility.

No key generated, read, recovered or used by this documentation task. Public certificates/fingerprints belong in artifact evidence; private keys/passwords never do.

## 10. APK/AAB creation and verification

After safety/build gates and authorized signing preparation, build from final candidate source with verified runner:

```bash
gradle :app:assembleRelease :app:bundleRelease
```

Current conventional output roots are `app/build/outputs/apk/release/` and `app/build/outputs/bundle/release/`; discover actual filenames/variant metadata from that run. Do not accept stale files just because paths exist. Source release minification is false; task success is not security or signing clearance. An unsigned APK/AAB is not a signed publishable candidate; stop and resolve configuration in a separately authorized task rather than relabeling it.

Populate non-secret path variables for selected artifacts/tool JAR in an approved shell before these examples; never use guessed credentials or filenames:

```bash
sha256sum "$RELEASE_APK_PATH" "$RELEASE_AAB_PATH"
apksigner verify --verbose --print-certs "$RELEASE_APK_PATH"
jarsigner -verify -verbose -certs "$RELEASE_AAB_PATH"
java -jar "$BUNDLETOOL_JAR_PATH" validate --bundle="$RELEASE_AAB_PATH"
```

- [ ] APK: verify Android signature schemes/certificate using apksigner and compare expected public fingerprint; inspect applicationId/version/SDK with trusted APK analyzer/aapt tooling. Record actual tool versions, hashes and byte sizes.
- [ ] AAB: verify JAR signing separately with jarsigner (including actual signer, signature integrity and assessed certificate warnings), compare upload certificate, then structurally validate with a verified available bundletool. `apksigner` is APK verification, not AAB verification. Structural validation alone does not prove correct signing/installation.
- [ ] Reject unsigned/invalid/wrong-key artifacts; assess expected self-signed certificate trust warnings explicitly, never suppress cryptographic failures or ignore wrong identity.
- [ ] If testing AAB-generated APKs, use approved bundletool workflow or Play testing-track delivery; record generation/signing source and device-delivered certificate. AAB cannot simply be installed as an APK.
- [ ] Copy/distribute only the reviewed immutable candidate; re-hash after transfer. Record all measurements from actual output, not historical hashes/sizes.

No commands/tool installation/build/signature verification occurred here. The old report's unsigned artifact hashes are historical only.

## 11. Manifest, permissions and dependency review

PHI-V1-033/034/035/037 require candidate-specific evidence:

- [ ] Read fresh merged **release** manifest and delivered artifact permissions/components, not source or retained debug variant alone.
- [ ] Preserve V1 no INTERNET/app-controlled ledger networking; justify ACCESS_NETWORK_STATE and other transitive declarations. Source declares POST_NOTIFICATIONS; retained debug adds WorkManager/platform permissions and tooling components, not proven release inventory.
- [ ] Review exported MainActivity/system/library components with permission protection and intent reachability; private startup provider remains private. No source FileProvider exists; generic AGENTS wording is not proof of one.
- [ ] Verify runtime notification acquisition as well as declaration/check; denial/revocation safe. Channel actual name is repayment_reminders; owner-only content may expose financial information on lock screen.
- [ ] Verify backup/device-transfer behavior: allowBackup=false plus template rules/no explicit device-transfer section is not universal OEM guarantee.
- [ ] Audit resolved release dependency tree/SDK behaviors, licenses/attributions and applicable vulnerabilities using recorded method/date. Unused Firebase/network/auth catalog entries are not active services; root Google Services apply-false is not app activation.
- [ ] Compare release-only versus debug dependencies and candidate findings with privacy/Play disclosures. No clean supply-chain/security/legal-compliance assertion without actual evidence.

No live dependency audit or freshly merged release review was performed for this runbook.

## 12. Real-device QA matrix

Every row starts **NOT RUN**. Future operator records device/model/API/OEM/build, app/artifact hash/version/certificate, settings and observations. Use synthetic data, controlled device and safe backups; JVM test names are not device outcomes.

| Scenario | Required observation / evidence |
| --- | --- |
| Clean install/onboarding | Correct branding, completion/skip/profile persistence; no login; restart returns appropriate route |
| Same-identity upgrade | Signing compatible, migration keeps IDs/balances/history/preferences; no silent wipe |
| Offline core journeys | Airplane-mode add both directions, search/detail, partial/full/mixed bulk/history/settings work; no network required |
| Restart/process interruption | No duplicate effects/false success; recoverable save/restore/prefs state documented |
| Home states | Loading/empty/no match/zero-net OPEN/settled distinguishable; gross/net accurate |
| Themes/text/responsiveness | Light/Dark/System, 200% text, long names/amounts, portrait/landscape/RTL and widths preserve critical content |
| TalkBack/action targets | Labels, selected states, dialog focus/errors/dismissal, practical 48dp targets and readable measured contrast |
| Notification permission | Fresh API33+ grant/deny/revoke and pre33 behavior; no false sent history when blocked |
| Reminder lifecycle | Qualified owner LENT stages, no BORROWED/settled; Doze/battery restriction/reboot/overlap/history failure limits recorded; no exact-time promise |
| Notification privacy/routing | Locked/unlocked visibility and correct existing/deleted friend route, reused Activity behavior |
| JSON/CSV providers | Real/fake null/throwing/cancel streams, truthful success, coherent copy; CSV not restore |
| Restore/reinstall | Explicit replacement/Cancel, semantic rejection and recovery; compare valid copied backup to reconstructed history |
| Local clear | Cancel safe; confirm clears intended local data/work/notifications, external copies retained |
| Dates | Today/backdate/due/overdue, midnight/timezone/DST/resume freshness; payment timestamps distinct |
| Brand/motion/insets | Native launcher masks/monochrome/splash, reduced motion/celebration gating, gesture/three-button bars/IME do not obscure actions |
| Crash/ANR | Observable failure collection sanitized; no reproducible integrity issue hidden by general crash-free result |

Unavailable device or scenario remains NOT RUN; applicability exclusions require reviewer reasoning. Do not use uninstall/clear-data as generic troubleshooting on real owner records.

## 13. Privacy policy and support URL verification

Intended values from current [PRIVACY.md](PRIVACY.md), [support page](index.html) and [privacy page](privacy/index.html):

| Endpoint/contact | Current intended value | Verification here |
| --- | --- | --- |
| Support website | https://professor1416.github.io/Phittoos/ | NOT RUN |
| Privacy policy | https://professor1416.github.io/Phittoos/privacy/ | NOT RUN |
| Support email | phittoos.app@gmail.com | Delivery/ownership NOT VERIFIED |

- [ ] Check public unauthenticated HTTPS availability/redirects and readable mobile content; GitHub Pages activation is not assumed from tracked HTML.
- [ ] Compare live HTML with approved corrected Markdown and signed candidate. Fix absent draft-sharing claims and inaccurate permission/source-only wording under PHI-V1-035 before relying on pages.
- [ ] Disclose owner-only local ledger, notification visibility, plaintext selected SAF providers/cloud retention, import replacement and local clear versus external copies. No universal “no data leaves device” or guaranteed recovery/transfer/erasure claim.
- [ ] Check support links/contact usability and exact tagline. Record date, HTTP/content result and reviewer; status code alone does not establish correct policy.

Future read-only checks can use a verified HTTPS client/browser (e.g. curl -I -L then actual content review); not executed here. Updating/publishing pages requires separate scope/authorization.

## 14. Play Console app setup

After candidate/disclosure proof, owner/operator checks **current** official guidance and actual account/Console questions (PHI-V1-040/044/046). No account access/eligibility/completion assumed.

1. Verify account identity/eligibility and required production-access prerequisites; keep credentials out of documentation.
2. Create/select intended app only when authorized; confirm package/application identity, name and applicable language/category/distribution choices.
3. Review financial-feature/category declarations for an informal personal ledger, not payments/lending/bank/UPI service; use actual current options rather than inventing a permanent taxonomy.
4. Validate app access (V1 no login), ads/target audience/content rating and data safety against final resolved binary and behavior.
5. Review permissions and local notifications; distinguish app networking from user-selected external files. Historical suggested “No collection/sharing” answers are not universally pre-approved answers.
6. Provide verified privacy/support URLs and required contact details; confirm app signing/upload setup and certificate mapping.
7. Save reviewed decision evidence without credentials or user data. Any unresolved policy/account requirement blocks applicable submission; do not tick a checklist on behalf of unavailable owner access.

No Console edit/upload/submission performed here. Account-dependent testing requirements must be checked, not guessed tester counts/durations.

## 15. Store listing and asset checklist

PHI-V1-045; generate nothing during this task.

- [ ] Approved app name and exact tagline; truthful short/full descriptions showing owner-recorded exchanges and local reminders.
- [ ] Approved corrected ₹/arrow logo; current applicable store icon/feature-graphic dimensions and format checked at preparation time.
- [ ] Candidate screenshots captured with synthetic data and actual screens; no external mockup, unreadable greeting.png or invented visual proof used as release screenshot.
- [ ] No login/shared confirmation/cloud/payment/reputation claim; explain settlement assertion, not verified transfer.
- [ ] Listing screenshots/copy correspond to artifact version, support/privacy contacts and accessible UX; required languages/assets/restrictions checked in current Console.
- [ ] Review external plaintext export/data limits; do not promise encryption/compliance/creditworthiness or full recovery.

Static logo preview inspection in DESIGN.md is not native device/store asset certification.

## 16. Testing tracks and pre-launch checks

Use internal and closed testing, production eligibility and device catalog as applicable to current account/program. Track availability, tester opt-in, required duration/coverage and production-access conditions must be evidenced in Console/current official guidance, not inferred from old reports.

- [ ] With explicit owner upload authorization, select reviewed signed candidate on intended test track; verify delivered package/version/signing certificate.
- [ ] Invite only authorized testers through approved process; document opt-in/access and sanitize feedback; no automatic messages are authorized by this runbook.
- [ ] Run actual device upgrade/offline/backup/reminder journeys; investigate crash/ANR/integrity reports before promotion.
- [ ] Review current Pre-launch report and device catalog exclusions/warnings; automated crawler results do not replace owner-led accounting/recovery QA.
- [ ] Record applicable closed-testing/production-access evidence and required remediation. Unsupported/unavailable track or account evidence remains blocked/not run.
- [ ] Promote only the same reviewed artifact, or rebuild/revalidate if changed; production action needs a separate explicit authorization.

No testing track configured or release uploaded by this task.

## 17. Release go/no-go decision

PHI-V1-048 stays **BLOCKED**. Copy this fillable record into a separately authorized evidence record when results exist; leave unknowns blank/NOT RUN, never default GO.

| Decision field | Candidate record |
| --- | --- |
| Candidate ID / versionName / versionCode | NOT ESTABLISHED |
| Source SHA / reproducible environment | NOT ESTABLISHED |
| APK/AAB paths, byte sizes, SHA-256 | NOT VERIFIED |
| Upload / delivered signing certificate fingerprints | NOT VERIFIED |
| Targeted/full tests, build/lint summary | NOT RUN at current candidate |
| Device/install/upgrade/restore matrix | NOT RUN at current candidate |
| Privacy/URL/Console/current-policy evidence | NOT VERIFIED |
| Open P0/P1 findings and dispositions | Unresolved TASKS gates; no new P0 reproduction asserted here |
| Other mandatory gates (34 prerequisites) | Unresolved; writing docs clears none |
| Conditional gates (13) | Each needs evidence/applicability/impact disposition |
| Independent QA/security/release reviewer verdict | NOT GIVEN |
| Owner approval / exact authorized action and track | NOT GIVEN |
| Decision date | NOT SET |
| GO / NO-GO | **NO-GO / BLOCKED at this snapshot** |

Required decision: no reproduced P0, no unresolved critical mandatory gate, documented conditional dispositions and candidate-linked evidence. Build/general test success cannot override financial corruption. Review and publication authorization are distinct; GO does not automatically authorize an agent to publish.

## 18. Rollout and post-release monitoring

Only after explicit owner production authorization:

1. Confirm approved candidate/version, track/countries/device audience and current Console rollout capabilities. Use controlled/staged rollout where supported; initial release/track behavior may differ, so do not invent percentages or halt guarantees.
2. Record operator/time/action/rollout state and monitor available Play/Android crash/ANR diagnostics, tester/user feedback and support. No analytics/crash SDK is installed or approved by this document.
3. Collect only sanitized necessary diagnostics, versions/device context and synthetic reproduction. Do not request raw financial backups/names/notes or passwords as default support data.
4. Treat financial-integrity reports as urgent gate events even with low crash rate. Distinguish unverified report from confirmed reproduction, but pause unsafe expansion while assessing credible harm.
5. Promote only after review of applicable test/feedback thresholds established by owner; avoid arbitrary targets or claims of monitored success. Document retention/access controls for support evidence.

PHI-V1-047 must supply an approved operations/recovery plan before production; it is currently OPEN. No rollout or monitoring system activation occurred here.

## 19. Incident response and recovery

For financial corruption, unsafe restore/migration, private-data exposure, signing compromise or widespread crash:

- [ ] Identify affected version/artifact/OS and financial/security impact; preserve sanitized evidence and existing data. Stop unsafe operations/release progression; obtain owner authorization to halt rollout where supported.
- [ ] Confirm reproduction with disposable synthetic fixtures; classify financial corruption P0 / NO RELEASE. Record uncertain prerequisites without invented exploit claims.
- [ ] Separate stopping new distribution from repairing already-installed ledgers. Google Play generally does not provide an arbitrary instant downgrade of every installed app to a previous APK; version/signature/schema compatibility constrain rollback.
- [ ] Prepare a scoped, tested **forward fix** with valid versionCode, migration/restore/history recovery and regression/device checks. Do not downgrade schema, enable destructive fallback or relabel damaged rows silently.
- [ ] Do not recommend uninstall/clear-data as a generic corruption fix. Verify an actual valid backup before discussing owner-approved recovery; no guarantee lost data can be recovered without one.
- [ ] For signing compromise, protect access and follow actual upload-key/app-signing-key recovery procedures with owner/Play; do not promise arbitrary certificate rotation or distribute incompatible packages.
- [ ] For privacy exposure, assess external copies/notifications/transfer/support data and containment; local deletion cannot revoke provider copies. Use authorized expert/legal review when actual incident requires it, without claiming compliance.
- [ ] Owner approves production fixes, rollout/communications and recovery actions; record exact scope and evidence. Re-review final artifact and candidate gates before resuming.

This is a procedure, not an assertion that incident tooling, reversible migration or guaranteed recovery exists.

## 20. Release evidence and audit trail

Convention only—no directories/registers/artifacts created here. In a future authorized workflow keep a protected candidate evidence location, for example an external `release-evidence/<candidate-id>/` register with build/test/device/privacy/decision references. Choose secure storage/retention outside app data; do not add generated release assets to Git automatically.

| Evidence | Required record |
| --- | --- |
| Source/environment | Exact SHA, branch/clean state, runner/JDK/SDK/dependency context and candidate version |
| Commands | Actual cwd/invocation, tool versions, exit codes/time and logs; secret arguments redacted/never captured |
| Tests | Executed/pass/fail/error/skip counts and targeted IDs, XML/log hashes, failure investigation and checker |
| Artifacts | APK/AAB filename/variant/size/hash, validation outputs and public certificate fingerprints; generated/delivered source |
| Device | Device/API/OEM/settings, installed version/certificate, synthetic before/after money/history and scenario outcomes |
| Privacy/store | Live URL/current Console-policy/declaration/listing review, applicability and date; no account credentials |
| Gates/decision | Task IDs, result/disposition, candidate link, independent reviewer, exact owner approval/action/date |

Never store signing passwords/private keys, process environment dumps or personal ledger content. Historical reports remain historical. Rebuilding/changing source produces a new evidence mapping; do not attach old hashes/results to new output.

## 21. Known limitations and open decisions

**Current status remains NOT RELEASE-READY / BLOCKED.** TASKS contains 17 OPEN/30 VERIFY/1 BLOCKED; no gate was completed by this runbook. Historical September 19 SHA-N/A unsigned APK, unverified AAB signing and 215-test claims and later retained 268/269 failure lack current signed/device/Console attestation.

Observed gaps include authoritative creation amount/range/precision, incomplete backup semantic/version/resource validation, preferences outside SQLite rollback, separate export reads, nullable-output false success, aggregate-only untouched checks, enum fallback, missing full v1 upgrade coverage, Home loading truth and runtime notification acquisition. Real overlap, provider exploit outcomes, process-death recovery, OEM transfer, accessibility/device delivery remain unverified scenarios; no new runtime reproduction was performed.

Open prerequisites/decisions include current build runner/helper, production signing/key/Console access, safe money/legacy contract, supported backup formats, candidate-specific device/upgrade/recovery, live pages/disclosure correction, current Play applicability and operational recovery evidence. No automatic SDK/dependency/storage migration, telemetry system or V2 functionality is chosen.

## 22. References and update protocol

Governing sources: [AGENTS](../AGENTS.md), [TASKS](TASKS.md), [PROJECT_STATE](PROJECT_STATE.md), [ARCHITECTURE](ARCHITECTURE.md), [RULES](RULES.md), [SECURITY](SECURITY.md), [PRD](PRD.md), [DESIGN](DESIGN.md), [PRIVACY](PRIVACY.md), [PLAY_DECLARATIONS](PLAY_DECLARATIONS.md), [RELEASE_CANDIDATE](RELEASE_CANDIDATE.md), [LICENSES](LICENSES.md), [brand](branding/PHITTOOS_BRAND_V1.md).

Source baseline `113dfb31e218fd947d650ba944aec9462a69d342`; linked declarations/paths exist at inspection. Additional references: [root build](../build.gradle.kts), [settings](../settings.gradle.kts), [version catalog](../gradle/libs.versions.toml), [Application](../app/src/main/java/com/professor1416/phittoos/PhittoosApplication.kt), [MainActivity](../app/src/main/java/com/professor1416/phittoos/MainActivity.kt), [repository](../app/src/main/java/com/professor1416/phittoos/data/repository/PhittoosRepository.kt), [database/migrations](../app/src/main/java/com/professor1416/phittoos/data/db/AppDatabase.kt), [converters](../app/src/main/java/com/professor1416/phittoos/data/db/Converters.kt), [backup](../app/src/main/java/com/professor1416/phittoos/export/PhittoosBackupManager.kt), [CSV](../app/src/main/java/com/professor1416/phittoos/export/PhittoosCsvExporter.kt), [Settings](../app/src/main/java/com/professor1416/phittoos/ui/screens/settings/SettingsScreen.kt), [notification helper](../app/src/main/java/com/professor1416/phittoos/reminder/ReminderNotificationHelper.kt), [test sources](../app/src/test/java/com/professor1416/phittoos), [instrumented identity test](../app/src/androidTest/java/com/professor1416/phittoos/ExampleInstrumentedTest.kt).

Before using runbook: refresh HEAD/remote/status, all applicable task statuses, identity/version, resolved dependencies/permissions/SDK/policy and candidate. Revalidate affected financial/data/device/Console/signing evidence after any change; retain old audit history. Current source wins over old report for implementation, not over mandatory safety requirements. Commands/URLs/external Play rules described here are NOT RUN/NOT VERIFIED unless a future evidence record proves them. No referenced conversation was independently retrieved; chat claims are not proof.

Only RELEASE.md is authorized in this task. Do not update TASKS statuses, previous docs, assets or code, and do not start CHANGELOG.md/README.md. Future publishing/building/incident actions need their own scope and owner authorization.

[build]: ../app/build.gradle.kts
[wrapper]: ../gradle/wrapper/gradle-wrapper.properties
