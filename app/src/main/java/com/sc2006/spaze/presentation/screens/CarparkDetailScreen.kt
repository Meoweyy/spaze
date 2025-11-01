package com.sc2006.spaze.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sc2006.spaze.data.repository.CarparkDetail
import com.sc2006.spaze.data.repository.CarparkLotAvailability
import com.sc2006.spaze.presentation.viewmodel.CarparkDetailUiState
import com.sc2006.spaze.presentation.viewmodel.CarparkDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarparkDetailScreen(
    carparkNumber: String,
    onNavigateBack: () -> Unit,
    viewModel: CarparkDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(carparkNumber) {
        if (uiState.carparkNumber.isBlank() && carparkNumber.isNotBlank()) {
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Carpark ${uiState.carparkNumber.ifBlank { carparkNumber }}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        CarparkDetailContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onRetry = { viewModel.refresh() }
        )
    }
}

@Composable
private fun CarparkDetailContent(
    paddingValues: PaddingValues,
    uiState: CarparkDetailUiState,
    onRetry: () -> Unit
) {
    when {
        uiState.isLoading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Loading carpark details…")
            }
        }
        uiState.error != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = uiState.error, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text(text = "Retry")
                }
            }
        }
        uiState.detail != null -> {
            CarparkDetailBody(
                paddingValues = paddingValues,
                detail = uiState.detail
            )
        }
    }
}

@Composable
private fun CarparkDetailBody(
    paddingValues: PaddingValues,
    detail: CarparkDetail
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SummaryCard(detail)
        LotAvailabilityCard(detail.lotAvailability)
        PricingCard(detail)
        LastUpdatedCard(detail)
    }
}

@Composable
private fun SummaryCard(detail: CarparkDetail) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = detail.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(text = detail.address, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Available Lots", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "${detail.availableLots} / ${detail.totalLots}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Price Tier", style = MaterialTheme.typography.labelMedium)
                    Text(text = detail.priceTier.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun LotAvailabilityCard(lotAvailability: List<CarparkLotAvailability>) {
    if (lotAvailability.isEmpty()) return

    Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Lot Types", style = MaterialTheme.typography.titleMedium)
            lotAvailability.forEach { availability ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val indicatorColor = lotTypeColor(availability.lotType)
                        Column(
                            modifier = Modifier
                                .background(indicatorColor, shape = CircleShape)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = availability.lotType, color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(text = "Total: ${availability.totalLots}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(text = "Available: ${availability.availableLots}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PricingCard(detail: CarparkDetail) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Pricing", style = MaterialTheme.typography.titleMedium)
            Text(text = "Tier: ${detail.priceTier.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = detail.baseHourlyRate?.let { "Estimated rate: S$${"%.2f".format(it)} / hr" } ?: "Rate information unavailable",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LastUpdatedCard(detail: CarparkDetail) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Last Updated", style = MaterialTheme.typography.titleMedium)
            Text(text = formatRelativeTime(detail.lastUpdated), style = MaterialTheme.typography.bodyMedium)
            detail.lastUpdatedRaw?.let {
                Text(text = "Source timestamp: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun lotTypeColor(lotType: String): Color {
    return when (lotType.uppercase()) {
        "C" -> Color(0xFF1B5E20)
        "H" -> Color(0xFF006064)
        "Y" -> Color(0xFFF57F17)
        else -> Color(0xFF546E7A)
    }
}

private fun formatRelativeTime(lastUpdated: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - lastUpdated
    val minutes = diffMillis / 60000L
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "moments ago"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        else -> "$days days ago"
    }
}

