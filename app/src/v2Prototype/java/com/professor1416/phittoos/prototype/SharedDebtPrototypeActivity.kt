package com.professor1416.phittoos.prototype

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.professor1416.phittoos.ui.theme.PhittoosTheme

class SharedDebtPrototypeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: SharedDebtPrototypeViewModel = viewModel()
            val state by model.uiState.collectAsStateWithLifecycle()
            PhittoosTheme {
                SharedDebtPrototypeScreen(
                    state, model::selectMode, model::selectParticipant,
                    model::proposeDebt, model::acceptProposal, model::resetDemo
                )
            }
        }
    }
}
