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
import com.sc2006.spaze.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    val userId = authState.currentUser?.userID

    val uiState by budgetViewModel.uiState.collectAsState()
    val currentBudget by budgetViewModel.currentBudget.collectAsState()

    // Dialog states
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var draftBudgetText by remember { mutableStateOf("") }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Load budget data when the authenticated user changes
    LaunchedEffect(userId) {
        if (!userId.isNullOrBlank()) {
            budgetViewModel.loadBudget(userId)
            budgetViewModel.loadAllBudgets(userId)
        }
    }

    // Show success messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            budgetViewModel.clearSuccessMessage()
        }
    }
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            budgetViewModel.clearError()
        }
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                )
            }

            when {
                userId.isNullOrBlank() -> {
                    // Not logged in
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Please log in to manage your budget.")
                    }
                }

                currentBudget == null -> {
                    // No budget set
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
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

                else -> {
                    // Budget summary
                    val budget = currentBudget!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Monthly Budget",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "SGD ${"%.2f".format(budget.monthlyBudget)}",
                                    style = MaterialTheme.typography.headlineMedium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Current Spending",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "SGD ${"%.2f".format(budget.currentMonthSpending)}",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val progress = (uiState.usagePercentage.coerceIn(0.0, 100.0) / 100f).toFloat()
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Remaining: SGD ${"%.2f".format(uiState.remainingBudget)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                when {
                                    uiState.usagePercentage >= 100.0 -> {
                                        Text(
                                            text = "❌ You've exceeded your budget!",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    uiState.usagePercentage >= 80.0 -> {
                                        Text(
                                            text = "⚠️ You're reaching your budget limit!",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { showSetBudgetDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Edit Budget")
                                    }
                                    OutlinedButton(
                                        onClick = { showResetConfirm = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reset Current Spending")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Set Monthly Budget
    if (showSetBudgetDialog && !userId.isNullOrBlank()) {
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
                        budgetViewModel.setMonthlyBudget(userId, amount)
                        showSetBudgetDialog = false
                    } else {
                        draftBudgetText = ""
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Reset Spending
    if (showResetConfirm && !userId.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Current Spending") },
            text = { Text("Are you sure you want to reset your current month’s spending to SGD 0.00?") },
            confirmButton = {
                TextButton(onClick = {
                    budgetViewModel.resetCurrentSpending(userId)
                    showResetConfirm = false
                }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
