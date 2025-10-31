package com.sc2006.spaze.presentation.screens

<<<<<<< Updated upstream
// BudgetScreen.kt
// Shows monthly budget info and Set Budget button
// Person 2 - UI Layer

import androidx.compose.foundation.layout.*
=======
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
>>>>>>> Stashed changes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
<<<<<<< Updated upstream
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
=======
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sc2006.spaze.presentation.viewmodel.BudgetUiState
import com.sc2006.spaze.presentation.viewmodel.BudgetViewModel
import java.text.NumberFormat
import java.util.Locale
>>>>>>> Stashed changes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit
) {
<<<<<<< Updated upstream
=======
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "SG")) }
    val sliderLimit = uiState.budgetLimit?.coerceAtLeast(1.0) ?: 1.0
    val sliderValue = uiState.currentAmount.coerceIn(0.0, sliderLimit)

>>>>>>> Stashed changes
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
<<<<<<< Updated upstream
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Monthly Budget", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("No budget set", color = Color.Gray)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { /* future logic */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                Text("Set Budget")
=======
        BudgetContent(
            paddingValues = paddingValues,
            uiState = uiState,
            sliderLimit = sliderLimit.toFloat(),
            sliderValue = sliderValue.toFloat(),
            currencyFormatter = currencyFormatter,
            onAmountChange = viewModel::onAmountChange,
            onAmountCommit = viewModel::persistAmountChange,
            onChangeLimit = viewModel::onChangeLimitClicked
        )
    }

    if (uiState.showSetupDialog) {
        BudgetLimitDialog(
            title = "Set Monthly Budget",
            confirmLabel = "Save",
            initialValue = null,
            allowDismiss = false,
            onConfirm = viewModel::submitInitialLimit,
            onDismiss = {}
        )
    }

    if (uiState.showChangeLimitDialog && !uiState.showSetupDialog) {
        BudgetLimitDialog(
            title = "Change Budget Limit",
            confirmLabel = "Update",
            initialValue = uiState.budgetLimit,
            allowDismiss = true,
            onConfirm = viewModel::updateLimit,
            onDismiss = viewModel::dismissChangeLimitDialog
        )
    }
}

@Composable
private fun BudgetContent(
    paddingValues: PaddingValues,
    uiState: BudgetUiState,
    sliderLimit: Float,
    sliderValue: Float,
    currencyFormatter: NumberFormat,
    onAmountChange: (Float) -> Unit,
    onAmountCommit: () -> Unit,
    onChangeLimit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Monthly Budget",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.budgetLimit == null) {
            Text(
                text = "Set a limit to start tracking your parking spend.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            val formattedCurrent = currencyFormatter.format(uiState.currentAmount)
            val formattedLimit = currencyFormatter.format(uiState.budgetLimit)

            Text(
                text = "$formattedCurrent / $formattedLimit",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = sliderValue,
                onValueChange = onAmountChange,
                valueRange = 0f..sliderLimit,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                onValueChangeFinished = onAmountCommit,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(onClick = onChangeLimit) {
                Text(text = "Change Limit")
            }
        }

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BudgetLimitDialog(
    title: String,
    confirmLabel: String,
    initialValue: Double?,
    allowDismiss: Boolean,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember(initialValue) { mutableStateOf(initialValue?.let { String.format("%.0f", it) } ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (allowDismiss) onDismiss() },
        title = { Text(title) },
        text = {
            Column {
                Text("Enter an amount for your monthly budget limit.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        errorMessage = null
                    },
                    singleLine = true,
                    label = { Text("Amount (SGD)") }
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = inputText.toDoubleOrNull()
                if (value != null && value > 0) {
                    onConfirm(value)
                    onDismiss()
                } else {
                    errorMessage = "Enter a positive number"
                }
            }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            if (allowDismiss) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
>>>>>>> Stashed changes
            }
        }
    )
}