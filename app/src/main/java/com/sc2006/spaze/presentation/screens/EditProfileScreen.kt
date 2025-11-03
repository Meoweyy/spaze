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
import com.sc2006.spaze.presentation.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = uiState.user
    
    // Local state for text fields
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Update fields when user loads
    LaunchedEffect(currentUser) {
        username = currentUser?.userName ?: ""
        email = currentUser?.email ?: ""
    }
    
    // Navigate back on success
    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            viewModel.clearUpdateSuccess()
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            Spacer(modifier = Modifier.height(8.dp))
            
            // Username field
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    showError = false
                },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && username.isBlank(),
                enabled = !uiState.isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    showError = false
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && !isValidEmail(email),
                enabled = !uiState.isLoading
            )
            
            if (showError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Show backend error
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save button
            Button(
                onClick = {
                    // Validate inputs
                    when {
                        username.isBlank() -> {
                            showError = true
                            errorMessage = "Username cannot be empty"
                        }
                        !isValidEmail(email) -> {
                            showError = true
                            errorMessage = "Please enter a valid email"
                        }
                        else -> {
                            showError = false
                            viewModel.updateProfile(username, email)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && !username.isBlank() && isValidEmail(email)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Changes")
                }
            }
        }
    }
}

// Email validation helper
private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

