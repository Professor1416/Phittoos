package com.professor1416.phittoos.prototype

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Synthetic UX simulation only. Perspective selection is not identity verification. */
class SharedDebtPrototypeViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(SharedDebtPrototypeState())
    val uiState = mutableState.asStateFlow()

    fun selectMode(mode: DemoMode) { mutableState.update { it.copy(mode = mode) } }
    fun selectParticipant(participant: DemoParticipant) {
        mutableState.update { it.copy(participant = participant) }
    }

    fun proposeDebt() {
        mutableState.update { state ->
            if (!state.canPropose) state else {
                val proposal = DemoDebtProposal()
                state.copy(
                    proposal = proposal,
                    history = state.history + DemoAgreementEvent(
                        "${proposal.id}-proposed", DemoEventType.PROPOSED,
                        state.participant.id, proposal.amountPaise
                    )
                )
            }
        }
    }

    fun acceptProposal() {
        mutableState.update { state ->
            val proposal = state.proposal
            if (!state.canAccept || proposal == null) state else state.copy(
                proposal = proposal.copy(status = DemoProposalStatus.ACCEPTED),
                history = state.history + DemoAgreementEvent(
                    "${proposal.id}-accepted", DemoEventType.ACCEPTED,
                    state.participant.id, proposal.amountPaise
                )
            )
        }
    }

    fun resetDemo() { mutableState.value = SharedDebtPrototypeState() }
}
