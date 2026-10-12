<p align="center">
  <img src="logo_preview/logo_after.png" alt="Phittoos logo: a white rupee symbol between emerald and orange arrows on slate" width="160" />
</p>

<h1 align="center">Phittoos</h1>

<p align="center"><strong>Tere mere hisaab… Phittoos!</strong></p>

<p align="center">A private money ledger for remembering what friends owe you—and what you owe them.</p>

<p align="center">
  <kbd>Android</kbd> <kbd>Kotlin</kbd> <kbd>Jetpack Compose</kbd> <kbd>Offline-first</kbd> <kbd>🚧 In Development</kbd>
</p>

---

## 💸 What is Phittoos?

Sometimes you lend money, borrow a little, or receive part of it back—and later forget who owes whom.

Phittoos keeps those exchanges organized in one owner-controlled Android ledger. Record the friend, amount, direction and dates, then update your records as repayments happen.

**It records money exchanges. It does not transfer money, collect debts or provide loans.** Marking an entry settled records your own account of payment; it does not verify a friend's payment or agreement.

## 🤝 Why Phittoos?

- See **money to get back** and **money you owe** clearly.
- Keep several transactions with the same friend separate.
- Record partial repayments without losing the remaining amount.
- Revisit dates, notes and history when you need context.
- Keep a local, offline-first ledger with **no friend account required**.

## ✨ Features

These features have implementations in the current V1 source. Testing and release verification are still in progress.

- 👥 **Friends and balances** — local friend records, search, individual entries and an overall balance view.
- 💚 **Lent and borrowed** — clear direction for each amount, with separate outstanding obligations.
- 🧾 **Repayments and settlement** — partial repayments, individual settlement and eligible same-direction bulk settlement.
- 📅 **Dates and due information** — past transaction dates, optional due dates and overdue indicators.
- ⏰ **Owner-local reminders** — reminder logic for eligible overdue LENT entries; permission and device-delivery checks remain ongoing.
- 📖 **Activity and history** — review recorded exchanges, repayments and settlements.
- 🔎 **Private reliability insights** — a local summary of your recorded LENT history, not a credit score or independently verified reputation.
- 💾 **Backup and export** — JSON backup/replacement restore and CSV copies for viewing or sharing.
- 🎨 **System, Light and Dark themes** — with distinct financial colors and labels.
- 🎉 **Settlement celebration** — when a successful eligible operation clears the friend's open entries.

## 📱 How Phittoos works

A simple product journey, once you are using the app:

1. **Complete local onboarding.** Enter your name for the on-device profile; no account or login is created.

2. **Add or select a friend.** Friends are entries in your ledger, not linked app users.

3. **Record the exchange.** Choose **LENT** or **BORROWED**, enter the amount, and optionally add a note and due date. You can select a past occurrence date.

4. **Check what is pending.** Home shows your balances; Friend Detail keeps that friend's entries and due information together.

5. **Update a repayment.** Record a partial amount against the intended entry, or mark an eligible entry settled. Bulk settlement applies only when the friend's open entries all have the same direction.

6. **Review and preserve your records.** Visit Activity for history and Settings for themes, backups or CSV export. Restoring JSON **replaces current local records** after confirmation; CSV cannot be restored.

> Add friend → Record money → Track balance → Update repayment → Settle

Made a recording mistake? Direct edit/delete is restricted to eligible untouched OPEN entries. Entries with repayment or settlement history have protective restrictions.

## 💚🧡 Lent vs borrowed

| Direction | Example | Meaning |
| --- | --- | --- |
| **LENT** 💚 | You give a friend ₹500 | ₹500 to get back |
| **BORROWED** 🧡 | A friend gives you ₹300 | ₹300 you owe |

Both entries remain separate. In this example, net is ₹200 to get back—but the borrowed ₹300 is still an open obligation.

Likewise, ₹500 LENT and ₹500 BORROWED can produce **₹0 net with two OPEN entries**. Zero net does not mean everything is settled.

## 🔒 Privacy and offline-first

V1 keeps owner-controlled records in local app storage. There is no login, friend linking or automatic cloud synchronization, and reminders are intended for **you**, not sent to friends.

Phittoos does not connect to your bank or process automatic UPI payments. Your entries and settlement actions are your own ledger records.

A few boundaries matter:

- JSON/CSV exports are plaintext copies sent to a **provider you select**, which may store them outside the device or in the cloud.
- Clearing local data does not delete previously exported copies.
- Notifications may show financial context, depending on your device settings.
- Device loss without a usable backup can mean loss of your records. Backup, restore and failure handling still have verification gaps.

Read the [privacy policy](docs/PRIVACY.md) and [security review](docs/SECURITY.md) for details and known limits. Local-first is not a promise of perfect security or guaranteed recovery.

## 🛠️ Tech stack

A native Android app built with:

- **Kotlin** — application code.
- **Jetpack Compose / Material 3** — screens and themes.
- **MVVM / StateFlow** — screen state and updates.
- **Room / SQLite** — local ledger storage.
- **WorkManager** — background reminder checks.

The [architecture document](docs/ARCHITECTURE.md) explains the actual components and data flow.

## 🚧 Project status

**Phittoos is in active V1 development.** Core features exist in source; financial integrity, backup/restore, regression testing, device QA and Google Play release gates remain under verification.

**No public release or Play-ready status is confirmed by this README.** A documentation milestone or successful individual test is not release approval.

For the current evidence and next verification work:

- [Project state](docs/PROJECT_STATE.md) — implemented behavior and limitations.
- [Engineering backlog](docs/TASKS.md) — scoped tasks and release gates.
- [Release runbook](docs/RELEASE.md) — the evidence required before an approved release.

## 📚 Documentation for developers

Explore the repository through these references:

- [AGENTS.md](AGENTS.md) — engineering and agent instructions.
- [Product requirements](docs/PRD.md) — V1 scope and acceptance.
- [Architecture](docs/ARCHITECTURE.md) — code structure and data flow.
- [Financial rules](docs/RULES.md) — accounting safeguards.
- [Security](docs/SECURITY.md) — threat model and privacy boundaries.
- [Design](docs/DESIGN.md) — UI, themes and branding.
- [Project state](docs/PROJECT_STATE.md) — verified source observations.
- [Tasks](docs/TASKS.md) — engineering backlog.
- [Release](docs/RELEASE.md) — release runbook.
- [Changelog](docs/CHANGELOG.md) — development history.

These documents distinguish approved requirements, source implementation and missing verification. They do not authorize changes outside a task's scope.

## 🌱 Future possibilities

Friend linking, shared confirmations and synchronization are possible **V2 ideas**, not approved or available V1 features. Any exploration needs separate product, financial-integrity and privacy decisions; no dates or technology choices are promised.

---

<p align="center"><strong>Tere mere hisaab… Phittoos!</strong><br />A little more clarity for your everyday hisaab.</p>
