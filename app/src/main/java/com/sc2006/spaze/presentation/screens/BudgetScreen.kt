package com.sc2006.spaze.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sc2006.spaze.presentation.viewmodel.BudgetViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val currentBudget by viewModel.currentBudget.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showSetBudgetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadBudget("user123")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Budget",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    currentBudget?.let { budget ->
                        Text(
                            text = "SGD ${budget.monthlyBudget}",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = (uiState.usagePercentage / 100).toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Spent: SGD ${budget.currentMonthSpending}")
                            Text("Remaining: SGD ${uiState.remainingBudget}")
                        }
                    } ?: Text("No budget set")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showSetBudgetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentBudget == null) "Set Budget" else "Update Budget")
            }

            if (uiState.showWarning) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠️ You've used 80% of your budget!",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (uiState.showExceeded) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "❌ Budget exceeded!",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}