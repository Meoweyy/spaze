package com.sc2006.spaze.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sc2006.spaze.presentation.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    var validationError by remember { mutableStateOf<String?>(null) }
    
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
                title = { Text("Change Password") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Current Password field
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { 
                    currentPassword = it
                    validationError = null
                },
                label = { Text("Current Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                trailingIcon = {
                    IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                        Icon(
                            imageVector = if (showCurrentPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showCurrentPassword) "Hide password" else "Show password"
                        )
                    }
                },
                enabled = !uiState.isLoading,
                isError = validationError != null && currentPassword.isBlank()
            )
            
            // New Password field
            OutlinedTextField(
                value = newPassword,
                onValueChange = { 
                    newPassword = it
                    validationError = null
                },
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            imageVector = if (showNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showNewPassword) "Hide password" else "Show password"
                        )
                    }
                },
                enabled = !uiState.isLoading
            )
            
            // Password strength indicator
            if (newPassword.isNotEmpty()) {
                val strength = getPasswordStrength(newPassword)
                val (color, text) = when (strength) {
                    PasswordStrength.WEAK -> MaterialTheme.colorScheme.error to "Weak"
                    PasswordStrength.MEDIUM -> MaterialTheme.colorScheme.primary to "Medium"
                    PasswordStrength.STRONG -> MaterialTheme.colorScheme.tertiary to "Strong"
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Password strength: $text",
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                    if (strength == PasswordStrength.WEAK) {
                        Text(
                            text = "At least 6 characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Confirm Password field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    validationError = null
                },
                label = { Text("Confirm New Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                        )
                    }
                },
                enabled = !uiState.isLoading,
                isError = validationError != null && confirmPassword != newPassword
            )
            
            // Validation error message
            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Backend error message
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Change Password button
            Button(
                onClick = {
                    // Validate inputs
                    validationError = when {
                        currentPassword.isBlank() -> "Please enter your current password"
                        newPassword.length < 6 -> "Password must be at least 6 characters"
                        newPassword == currentPassword -> "New password must be different from current password"
                        newPassword != confirmPassword -> "Passwords do not match"
                        else -> {
                            // All validation passed
                            viewModel.changePassword(currentPassword, newPassword)
                            null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && 
                    currentPassword.isNotBlank() && 
                    newPassword.isNotBlank() && 
                    confirmPassword.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Change Password")
                }
            }
        }
    }
}

// Password strength enum
enum class PasswordStrength {
    WEAK, MEDIUM, STRONG
}

// Calculate password strength
fun getPasswordStrength(password: String): PasswordStrength {
    return when {
        password.length < 6 -> PasswordStrength.WEAK
        password.length <= 8 -> PasswordStrength.MEDIUM
        password.length > 8 -> PasswordStrength.STRONG
        else -> PasswordStrength.WEAK
    }
}

