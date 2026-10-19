package com.professor1416.phittoos.prototype

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.professor1416.phittoos.R
import com.professor1416.phittoos.ui.components.AvatarInitial
import com.professor1416.phittoos.ui.theme.PhittoosColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedDebtPrototypeScreen(
    state: SharedDebtPrototypeState,
    onMode: (DemoMode) -> Unit,
    onParticipant: (DemoParticipant) -> Unit,
    onPropose: () -> Unit,
    onAccept: () -> Unit,
    onReset: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.v2_demo_title)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag("prototype_screen")) {
            // Outside the scroller: the synthetic-data disclosure remains visible.
            Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    stringResource(R.string.v2_demo_banner),
                    Modifier.fillMaxWidth().padding(12.dp).testTag("demo_banner"),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DemoMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.mode == mode,
                            onClick = { onMode(mode) },
                            label = { Text(stringResource(if (mode == DemoMode.PRIVATE) R.string.v2_private else R.string.v2_shared)) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("mode_${mode.name}")
                        )
                    }
                }
                Text(stringResource(R.string.v2_demo_perspective), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DemoParticipant.entries.forEach { actor ->
                        FilterChip(
                            selected = state.participant == actor,
                            onClick = { onParticipant(actor) },
                            label = { Text(stringResource(if (actor == DemoParticipant.A) R.string.v2_actor_a else R.string.v2_actor_b)) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("actor_${actor.name}")
                        )
                    }
                }
                if (state.mode == DemoMode.PRIVATE) {
                    DemoCard("private_card") {
                        Text(stringResource(R.string.v2_private_heading), style = MaterialTheme.typography.titleMedium)
                        if (state.participant.id == state.privateRecord.ownerId) {
                            Text(stringResource(R.string.v2_private_note))
                            Text(stringResource(R.string.v2_private_amount, formatDemoPaise(state.privateRecord.amountPaise)))
                            Text(stringResource(R.string.v2_private_explanation))
                        } else Text(stringResource(R.string.v2_no_private_record))
                    }
                } else {
                    DemoCard("agreed_balance") {
                        Text(stringResource(R.string.v2_shared_heading), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.v2_agreed_outstanding, formatDemoPaise(state.agreedOutstandingPaise)),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (state.agreedOutstandingPaise == 0L) Text(stringResource(R.string.v2_no_agreed_debt))
                        else {
                            val receivable = state.receivablePaise > 0L
                            val colors = PhittoosColors.financial
                            Text(
                                stringResource(if (receivable) R.string.v2_receivable else R.string.v2_payable, formatDemoPaise(state.agreedOutstandingPaise)),
                                color = if (receivable) colors.lentAccent else colors.borrowedAccent
                            )
                        }
                    }
                    DemoCard("proposal_card") {
                        AvatarInitial("Participant ${state.participant.name}")
                        Text(stringResource(R.string.v2_debt_terms, formatDemoPaise(50_000L)))
                        Text(stringResource(when (state.proposal?.status) {
                            null -> R.string.v2_status_draft
                            DemoProposalStatus.PROPOSED -> R.string.v2_status_proposed
                            DemoProposalStatus.ACCEPTED -> R.string.v2_status_accepted
                        }), Modifier.testTag("proposal_status"))
                        when (state.proposal?.status) {
                            null -> {
                                Button(onClick = onPropose, enabled = state.canPropose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("propose_debt")) {
                                    Text(stringResource(R.string.v2_propose))
                                }
                                if (!state.canPropose) Text(stringResource(R.string.v2_switch_to_proposer))
                            }
                            DemoProposalStatus.PROPOSED -> {
                                Text(stringResource(R.string.v2_pending))
                                Button(onClick = onAccept, enabled = state.canAccept, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("accept_debt")) {
                                    Text(stringResource(R.string.v2_accept))
                                }
                            }
                            DemoProposalStatus.ACCEPTED -> Text(stringResource(R.string.v2_accepted_explanation))
                        }
                    }
                    DemoCard("event_history") {
                        Text(stringResource(R.string.v2_history), style = MaterialTheme.typography.titleMedium)
                        if (state.history.isEmpty()) Text(stringResource(R.string.v2_no_events))
                        state.history.forEach { event ->
                            Text(
                                stringResource(if (event.type == DemoEventType.PROPOSED) R.string.v2_event_proposed else R.string.v2_event_accepted, formatDemoPaise(event.amountPaise)),
                                Modifier.testTag("event_${event.id}")
                            )
                        }
                    }
                }
                Text(stringResource(R.string.v2_reset_explanation), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reset_demo")) {
                    Text(stringResource(R.string.v2_reset))
                }
            }
        }
    }
}

@Composable
private fun DemoCard(tag: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().testTag(tag)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
