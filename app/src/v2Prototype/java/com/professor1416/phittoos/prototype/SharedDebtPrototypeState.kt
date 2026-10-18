package com.professor1416.phittoos.prototype

enum class DemoMode { PRIVATE, SHARED }
enum class DemoParticipant(val id: String) { A("demo-a"), B("demo-b") }
enum class DemoProposalStatus { PROPOSED, ACCEPTED }
enum class DemoEventType { PROPOSED, ACCEPTED }

data class DemoDebtProposal(
    val id: String = "demo-debt-1",
    val creditorId: String = DemoParticipant.A.id,
    val debtorId: String = DemoParticipant.B.id,
    val proposerId: String = DemoParticipant.A.id,
    val amountPaise: Long = 50_000L,
    val status: DemoProposalStatus = DemoProposalStatus.PROPOSED
)

data class DemoAgreementEvent(
    val id: String,
    val type: DemoEventType,
    val actorId: String,
    val amountPaise: Long
)

data class DemoPrivateRecord(
    val id: String = "private-demo-only",
    val ownerId: String = DemoParticipant.A.id,
    val amountPaise: Long = 30_000L
)

data class SharedDebtPrototypeState(
    val mode: DemoMode = DemoMode.PRIVATE,
    val participant: DemoParticipant = DemoParticipant.A,
    val privateRecord: DemoPrivateRecord = DemoPrivateRecord(),
    val proposal: DemoDebtProposal? = null,
    val history: List<DemoAgreementEvent> = emptyList()
) {
    val agreedOutstandingPaise: Long
        get() = proposal?.takeIf { it.status == DemoProposalStatus.ACCEPTED }?.amountPaise ?: 0L
    val receivablePaise: Long
        get() = if (participant.id == proposal?.creditorId) agreedOutstandingPaise else 0L
    val payablePaise: Long
        get() = if (participant.id == proposal?.debtorId) agreedOutstandingPaise else 0L
    val canPropose: Boolean
        get() = mode == DemoMode.SHARED && participant == DemoParticipant.A && proposal == null
    val canAccept: Boolean
        get() = mode == DemoMode.SHARED && proposal?.status == DemoProposalStatus.PROPOSED &&
            participant.id == proposal.debtorId && participant.id != proposal.proposerId
}

/** Presentation only: no Double conversion and no V1 currency/storage dependency. */
fun formatDemoPaise(amountPaise: Long): String {
    require(amountPaise >= 0L)
    val rupees = amountPaise / 100L
    val paise = amountPaise % 100L
    return if (paise == 0L) "₹$rupees" else "₹$rupees.${paise.toString().padStart(2, '0')}"
}
