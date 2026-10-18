package com.professor1416.phittoos.prototype

import org.junit.Assert.*
import org.junit.Test

class SharedDebtPrototypeViewModelTest {
    private fun sharedModel() = SharedDebtPrototypeViewModel().apply { selectMode(DemoMode.SHARED) }

    @Test fun privateIsInitialAndDoesNotCreateSharedHistory() {
        val model = SharedDebtPrototypeViewModel()
        assertEquals(DemoMode.PRIVATE, model.uiState.value.mode)
        model.proposeDebt()
        model.acceptProposal()
        assertNull(model.uiState.value.proposal)
        assertTrue(model.uiState.value.history.isEmpty())
        assertEquals(30_000L, model.uiState.value.privateRecord.amountPaise)
    }

    @Test fun proposalLeavesAgreedBalanceZeroAndRetainsTerms() {
        val model = sharedModel()
        model.proposeDebt()
        val state = model.uiState.value
        assertEquals(DemoProposalStatus.PROPOSED, state.proposal!!.status)
        assertEquals(DemoParticipant.A.id, state.proposal.creditorId)
        assertEquals(DemoParticipant.B.id, state.proposal.debtorId)
        assertEquals(50_000L, state.proposal.amountPaise)
        assertEquals(0L, state.agreedOutstandingPaise)
        assertEquals(0L, state.receivablePaise)
        assertEquals(0L, state.payablePaise)
        assertEquals(listOf(DemoEventType.PROPOSED), state.history.map { it.type })
    }

    @Test fun proposerCannotSelfAcceptEvenThroughDirectAction() {
        val model = sharedModel()
        model.proposeDebt()
        val before = model.uiState.value
        assertFalse(before.canAccept)
        model.acceptProposal()
        assertEquals(before, model.uiState.value)
    }

    @Test fun onlyBMayAcceptAndBalancesDependOnPerspective() {
        val model = sharedModel()
        model.proposeDebt()
        model.selectParticipant(DemoParticipant.B)
        assertTrue(model.uiState.value.canAccept)
        model.acceptProposal()
        assertEquals(DemoProposalStatus.ACCEPTED, model.uiState.value.proposal!!.status)
        assertEquals(50_000L, model.uiState.value.agreedOutstandingPaise)
        assertEquals(0L, model.uiState.value.receivablePaise)
        assertEquals(50_000L, model.uiState.value.payablePaise)
        model.selectParticipant(DemoParticipant.A)
        assertEquals(50_000L, model.uiState.value.receivablePaise)
        assertEquals(0L, model.uiState.value.payablePaise)
    }

    @Test fun acceptedHistoryAppendsOnceAndOriginalProposalEventRemains() {
        val model = sharedModel()
        model.proposeDebt()
        val original = model.uiState.value.history.single()
        model.proposeDebt()
        assertEquals(listOf(original), model.uiState.value.history)
        model.selectParticipant(DemoParticipant.B)
        model.acceptProposal()
        val accepted = model.uiState.value
        assertEquals(original, accepted.history.first())
        assertEquals(2, accepted.history.size)
        assertEquals(DemoParticipant.B.id, accepted.history.last().actorId)
        assertEquals(50_000L, accepted.history.last().amountPaise)
        assertEquals(2, accepted.history.map { it.id }.distinct().size)
        model.acceptProposal()
        model.proposeDebt()
        assertEquals(accepted, model.uiState.value)
    }

    @Test fun privateModeCannotAcceptAndPrivateFixtureNeverChanges() {
        val model = sharedModel()
        val privateBefore = model.uiState.value.privateRecord
        model.proposeDebt()
        model.selectParticipant(DemoParticipant.B)
        model.selectMode(DemoMode.PRIVATE)
        val before = model.uiState.value
        model.acceptProposal()
        assertEquals(before, model.uiState.value)
        model.selectMode(DemoMode.SHARED)
        model.acceptProposal()
        assertEquals(privateBefore, model.uiState.value.privateRecord)
        assertFalse(model.uiState.value.history.any { it.id == privateBefore.id })
    }

    @Test fun bCannotCreateTheAProposalOrAcceptMissingProposal() {
        val model = sharedModel()
        model.selectParticipant(DemoParticipant.B)
        val before = model.uiState.value
        model.proposeDebt()
        model.acceptProposal()
        assertEquals(before, model.uiState.value)
    }

    @Test fun resetRestoresOnlySyntheticInitialStateAndNewRunWorks() {
        val model = sharedModel()
        model.proposeDebt()
        model.selectParticipant(DemoParticipant.B)
        model.acceptProposal()
        model.resetDemo()
        assertEquals(SharedDebtPrototypeState(), model.uiState.value)
        model.selectMode(DemoMode.SHARED)
        model.proposeDebt()
        assertEquals(1, model.uiState.value.history.size)
        assertEquals(0L, model.uiState.value.agreedOutstandingPaise)
    }

    @Test fun paiseFormattingDoesNotLoseFractionalOrLargeIntegerPrecision() {
        assertEquals("₹0", formatDemoPaise(0))
        assertEquals("₹500", formatDemoPaise(50_000))
        assertEquals("₹0.01", formatDemoPaise(1))
        assertEquals("₹500.05", formatDemoPaise(50_005))
        assertEquals("₹92233720368547758.07", formatDemoPaise(Long.MAX_VALUE))
        assertThrows(IllegalArgumentException::class.java) { formatDemoPaise(-1) }
    }
}
