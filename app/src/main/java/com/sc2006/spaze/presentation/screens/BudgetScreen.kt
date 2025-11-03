package com.sc2006.spaze.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sc2006.spaze.presentation.viewmodel.BudgetViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    userId: String = "user123", // TODO: replace with real auth user id
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentBudget by viewModel.currentBudget.collectAsState()

    // Local UI state for "set budget" dialog
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var draftBudgetText by remember { mutableStateOf("") }

    // Load budget flows when this composable appears
    LaunchedEffect(userId) {
        viewModel.loadBudget(userId)
        viewModel.loadAllBudgets(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)) {

            currentBudget?.let { budget ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Monthly Budget", style = MaterialTheme.typography.titleMedium)
                            Text(text = "$${"%.2f".format(budget.monthlyBudget)}", style = MaterialTheme.typography.headlineMedium)

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(text = "Current Spending", style = MaterialTheme.typography.titleSmall)
                            Text(text = "$${"%.2f".format(budget.currentMonthSpending)}", style = MaterialTheme.typography.titleLarge)

                            Spacer(modifier = Modifier.height(12.dp))

                            val progress = (uiState.usagePercentage.coerceIn(0.0, 100.0) / 100f).toFloat()
                            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Remaining: $${"%.2f".format(uiState.remainingBudget)}", style = MaterialTheme.typography.bodyMedium)

                            Spacer(modifier = Modifier.height(12.dp))

                            if (uiState.usagePercentage >= 100.0) {
                                Text(text = "❌ You've exceeded your budget!", color = MaterialTheme.colorScheme.error)
                            } else if (uiState.usagePercentage >= 80.0) {
                                Text(text = "⚠️ You're reaching your budget limit!", color = MaterialTheme.colorScheme.error)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { showSetBudgetDialog = true }) {
                                    Text("Edit Budget")
                                }
                                OutlinedButton(onClick = { viewModel.addSpending(userId, 5.0) }) {
                                    // quick test button to add spending (replace with real input)
                                    Text("Add SGD 5 (test)")
                                }
                            }
                        }
                    }
                }
            } ?: run {
                // No budget set view
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("No budget set")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        draftBudgetText = "100.0"
                        showSetBudgetDialog = true
                    }) {
                        Text("Set Budget")
                    }
                }
            }

            // Show messages
            uiState.successMessage?.let { msg ->
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter), action = {
                    TextButton(onClick = { viewModel.clearSuccessMessage() }) { Text("OK") }
                }) {
                    Text(msg)
                }
            }

            uiState.error?.let { err ->
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter)) {
                    Text(err)
                }
            }
        }
    }

    // Simple dialog to set monthly budget (simple display + edit as requested)
    if (showSetBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showSetBudgetDialog = false },
            title = { Text("Set Monthly Budget (SGD)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftBudgetText,
                        onValueChange = { draftBudgetText = it },
                        label = { Text("Amount") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = draftBudgetText.toDoubleOrNull()
                    if (amount != null) {
                        viewModel.setMonthlyBudget(userId, amount)
                        showSetBudgetDialog = false
                    } else {
                        // simple local validation: keep dialog open or show UI error
                        draftBudgetText = ""
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetBudgetDialog = false }) { Text("Cancel") }
            }
        )
    }
}
