# Phittoos architecture diagrams — Phase 2.5 (2026-10-13)

> **Status:** Documentation-only, review snapshot. Pages 01–05 describe inspected CURRENT V1; Pages 06–10 are PROPOSED V2, **not implemented, approved, or security-certified**.

**Canonical editable board:** https://www.tldraw.com/f/vI2EVU51EskwE1jUkDMBv

## Current V1 diagrams (based on source, not runtime proof)

| Page | Diagram |
|---|---|
| 01 | [Android System Architecture](v1/01-android-system-architecture.svg) |
| 02 | [Accounting & Balance Flow](v1/02-accounting-balance-flow.svg) |
| 03 | [Transaction & Settlement State Machine](v1/03-transaction-settlement-state-machine.svg) |
| 04 | [Database Relationships (ERD)](v1/04-database-relationships-erd.svg) |
| 05 | [Security & Privacy Boundaries](v1/05-security-privacy-boundaries.svg) |

V1 is an offline, owner-entered ledger. No login, cloud syncing, remote counterparty agreement or payment verification is implemented. Local settlement records the owner's assertion. The Room model stores monetary amounts as Double, not integer paise. Identified validation, concurrency, export and restore risks are documented in existing source docs; these diagrams do not resolve them.

## Proposed V2 diagrams — decisions pending

| Page | Diagram |
|---|---|
| 06 | [High-Level Architecture](v2-proposed/06-v2-high-level-architecture.svg) |
| 07 | [Friend Linking & Consent Flow](v2-proposed/07-friend-linking-consent.svg) |
| 08 | [Shared Financial Event Flow](v2-proposed/08-shared-financial-events.svg) |
| 09 | [Offline Sync & Conflict Resolution](v2-proposed/09-offline-sync-conflicts.svg) |
| 10 | [Security & Trust Model](v2-proposed/10-security-trust-model.svg) |

V2 plans assume optional account linking and shared events but do NOT specify a selected auth service, backend, database, sync transport, money representation, dispute policy, data lifecycle, or production security guarantee.

## Editable native snapshot

[tldraw document-only editor snapshot (2026-10-13)](native/phittoos-editor-snapshot-2026-10-13.json) — JSON containing the full board document records and schema, with editor session set to null. It includes the preserved legacy Page 1 and all ten dedicated pages. It is intended for a developer using the tldraw Editor snapshot/loadSnapshot APIs, **not guaranteed to be a direct .tldr menu-import file**. Keep the live board as the primary interactive editing experience.

SVGs are versioned exports of native tldraw shapes. They may contain SVG foreignObject/embedded styles; depending on GitHub/security sanitization, some viewers may render text differently. Review every exported SVG in the target viewer and use the live board as the reliable editable reference. A separate PNG preview pass is optional.

## Verification and provenance

- Diagram correction and readback completed on 2026-10-13; see [audit report](REVIEW_2026-10-13.md).
- Source documentation: [ARCHITECTURE.md](../ARCHITECTURE.md), [RULES.md](../RULES.md), [SECURITY.md](../SECURITY.md), [PROJECT_STATE.md](../PROJECT_STATE.md), root [AGENTS.md](../../AGENTS.md). These documents contain distinct October 12 source snapshot SHAs; refresh source before relying on them for implementation.
- All arrows were bound and no geometric node overlap was detected on dedicated pages. This is a **diagram topology audit**, not automated UI text-overflow testing, runtime QA or security certification.
- Pages 06–10 must remain marked PROPOSED until six signed-off V2 contracts and test plans exist.

## Change and implementation process

1. Review source code and documentation first; label VERIFIED, PROPOSED and DECISION REQUIRED explicitly.
2. Update the corresponding live editable tldraw page; inspect the rendered diagram and arrow direction.
3. Re-export versioned SVG and native snapshot and update this manifest. Never silently overwrite V1 evidence with V2 proposals.
4. Approve six contracts before V2 coding: Accounting, State Machine, Event/Data, Sync/Idempotency, Authorization and Privacy/Data Lifecycle.
5. Derive scoped tests from the contracts, including replay, double repayment, lost acknowledgment, stale state, revocation and reinstall/recovery behavior.

**Scope of this PR:** Only documentation/exports in docs/architecture. No Android source, build, Room schema, dependency or production configuration changes.