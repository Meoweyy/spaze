# Mock ViewModels for UI Testing

**Week 1 - Person 3 Deliverable for Person 1 & 2 (UI Team)**

This directory contains mock ViewModels with realistic sample data so you can develop and test UI screens **without waiting for the repository/database implementation**.

---

## 📁 Files

1. **`MockDataProvider.kt`** - Contains all sample data (carparks, users, sessions, etc.)
2. **`MockViewModels.kt`** - Contains 6 mock ViewModels ready to use

---

## 🎯 Available Mock ViewModels

| ViewModel | Purpose | Key Features |
|-----------|---------|--------------|
| `MockHomeViewModel` | Home screen with carpark list/map | Load, search, filter carparks |
| `MockSearchViewModel` | Search functionality | Search, filter, sort, recent searches |
| `MockFavouritesViewModel` | Favorites management | Add, remove, check favorites |
| `MockProfileViewModel` | User profile | View, update profile, change password, logout |
| `MockParkingSessionViewModel` | Active parking session | Start, end session, real-time cost tracking |
| `MockBudgetViewModel` | Budget management | Set budget, track spending, warnings |

---

## 🚀 How to Use Mock ViewModels

### Step 1: Import the Mock ViewModel

```kotlin
import com.sc2006.spaze.mock.MockHomeViewModel
import com.sc2006.spaze.mock.MockDataProvider
```

### Step 2: Use in Your Composable

```kotlin
@Composable
fun HomeScreen() {
    // Use mock ViewModel for testing
    val viewModel = MockHomeViewModel()

    val carparks by viewModel.carparks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Your UI code here
    if (isLoading) {
        CircularProgressIndicator()
    } else {
        LazyColumn {
            items(carparks) { carpark ->
                CarparkCard(carpark)
            }
        }
    }
}
```

### Step 3: Switch to Real ViewModel Later

When the repository is ready, simply change:

```kotlin
// OLD - Mock for testing
val viewModel = MockHomeViewModel()

// NEW - Real ViewModel with Hilt injection
val viewModel: HomeViewModel = hiltViewModel()
```

---

## 📊 Mock Data Available

### Carparks (8 locations)
- **CP001**: Orchard Central (45/150 lots, 250m away) ⭐ Favorite
- **CP002**: Marina Square (180/300 lots, 1.2km away)
- **CP003**: ION Orchard (12/400 lots, 450m away) ⭐ Favorite
- **CP004**: Vivocity (320/500 lots, 5.5km away)
- **CP005**: Bugis Junction (0/200 lots - FULL, 800m away)
- **CP006**: Suntec City (400/600 lots, 1.5km away) ⭐ Favorite
- **CP007**: Changi Airport T3 (650/1000 lots, 18km away)
- **CP008**: Plaza Singapura (35/250 lots, 350m away)

### User
- **ID**: mock_user_123
- **Name**: John Doe
- **Email**: john.doe@example.com

### Recent Searches
- "Orchard" (1 hour ago)
- "Marina Bay" (2 hours ago)
- "Bugis" (1 day ago)

### Active Parking Session
- Location: Orchard Central
- Lot: A-123
- Started: 1 hour ago
- Estimated Cost: $2.50

### Budget
- Monthly Budget: $100.00
- Current Spending: $45.50
- Remaining: $54.50
- Warning Threshold: 80% ($80)

---

## 📝 Usage Examples

### Example 1: HomeScreen with Mock Data

```kotlin
@Composable
fun HomeScreen() {
    val viewModel = MockHomeViewModel()
    val carparks by viewModel.carparks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Nearby Carparks") })
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Text("Error: $error", color = Color.Red)
            }
            else -> {
                LazyColumn(Modifier.padding(padding)) {
                    items(carparks) { carpark ->
                        CarparkItem(
                            carpark = carpark,
                            onFavoriteClick = { /* Handle favorite */ },
                            onClick = { /* Navigate to details */ }
                        )
                    }
                }
            }
        }
    }

    // Test the refresh functionality
    Button(onClick = { viewModel.refreshFromAPI() }) {
        Text("Refresh")
    }
}
```

### Example 2: SearchScreen with Filters

```kotlin
@Composable
fun SearchScreen() {
    val viewModel = MockSearchViewModel()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    Column {
        // Search bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search carparks") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = { viewModel.searchCarparks(searchQuery) }) {
            Text("Search")
        }

        // Filter controls
        Row {
            Button(onClick = { viewModel.filterResults(minLots = 10, maxDistance = 1000f) }) {
                Text("Min 10 lots, Within 1km")
            }
            Button(onClick = { viewModel.sortResults(SortOption.DISTANCE) }) {
                Text("Sort by Distance")
            }
        }

        // Results
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(searchResults) { carpark ->
                    CarparkCard(carpark)
                }
            }
        }

        // Recent searches
        Text("Recent Searches", style = MaterialTheme.typography.headlineSmall)
        recentSearches.forEach { search ->
            Text(search.query)
        }
    }
}
```

### Example 3: FavouritesScreen

```kotlin
@Composable
fun FavouritesScreen() {
    val viewModel = MockFavouritesViewModel()
    val favorites by viewModel.favoriteCarparks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success messages
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No favorites yet")
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(favorites) { carpark ->
                    CarparkCard(
                        carpark = carpark,
                        onRemoveClick = { viewModel.removeFromFavorites(carpark.carparkID) }
                    )
                }
            }
        }
    }
}
```

### Example 4: ProfileScreen

```kotlin
@Composable
fun ProfileScreen() {
    val viewModel = MockProfileViewModel()
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(Modifier.padding(16.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)

        if (isLoading) {
            CircularProgressIndicator()
        }

        // User info
        Text("Name: ${user.userName}")
        Text("Email: ${user.email}")

        Spacer(Modifier.height(16.dp))

        // Edit profile button
        Button(onClick = {
            viewModel.updateProfile("Jane Doe", "jane.doe@example.com")
        }) {
            Text("Update Profile")
        }

        // Change password button
        Button(onClick = {
            viewModel.changePassword("oldpass", "newpass123")
        }) {
            Text("Change Password")
        }

        // Logout button
        OutlinedButton(onClick = { viewModel.logout() }) {
            Text("Logout")
        }

        // Show messages
        successMessage?.let {
            Text(it, color = Color.Green)
        }
        error?.let {
            Text(it, color = Color.Red)
        }
    }
}
```

### Example 5: ParkingSessionScreen

```kotlin
@Composable
fun ParkingSessionScreen() {
    val viewModel = MockParkingSessionViewModel()
    val activeSession by viewModel.activeSession.collectAsState()
    val estimatedCost by viewModel.estimatedCost.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(Modifier.padding(16.dp)) {
        activeSession?.let { session ->
            Text("Active Session", style = MaterialTheme.typography.headlineMedium)
            Text("Location: ${session.carparkName}")
            Text("Lot: ${session.lotNumber}")
            Text("Estimated Cost: $${"%.2f".format(estimatedCost)}")

            Button(onClick = { viewModel.endSession() }) {
                Text("End Session")
            }
        } ?: run {
            Text("No active session")
            Button(onClick = {
                viewModel.startSession("CP001", "Orchard Central", "A-123")
            }) {
                Text("Start Test Session")
            }
        }
    }
}
```

### Example 6: BudgetScreen

```kotlin
@Composable
fun BudgetScreen() {
    val viewModel = MockBudgetViewModel()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val currentSpending by viewModel.currentSpending.collectAsState()
    val remainingBudget by viewModel.remainingBudget.collectAsState()
    val isWarning by viewModel.isWarningTriggered.collectAsState()
    val isExceeded by viewModel.isBudgetExceeded.collectAsState()

    Column(Modifier.padding(16.dp)) {
        Text("Budget Overview", style = MaterialTheme.typography.headlineMedium)

        LinearProgressIndicator(
            progress = currentSpending / monthlyBudget,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Monthly Budget: $${"%.2f".format(monthlyBudget)}")
        Text("Current Spending: $${"%.2f".format(currentSpending)}")
        Text(
            "Remaining: $${"%.2f".format(remainingBudget)}",
            color = when {
                isExceeded -> Color.Red
                isWarning -> Color(0xFFFF9800)
                else -> Color.Green
            }
        )

        if (isWarning && !isExceeded) {
            Text("⚠️ Warning: 80% of budget used", color = Color(0xFFFF9800))
        }
        if (isExceeded) {
            Text("❌ Budget exceeded!", color = Color.Red)
        }

        // Test buttons
        Button(onClick = { viewModel.addSpending(10f) }) {
            Text("Add $10 Spending")
        }
        Button(onClick = { viewModel.setMonthlyBudget(150f) }) {
            Text("Set Budget to $150")
        }
    }
}
```

---

## 🎨 Testing Different Scenarios

### Test Loading States
```kotlin
viewModel.loadNearbyCarparks() // Watch isLoading become true then false
```

### Test Search Functionality
```kotlin
viewModel.searchCarparks("Orchard") // Should return 3 results
viewModel.searchCarparks("Marina") // Should return 1 result
viewModel.searchCarparks("xyz") // Should return empty list
```

### Test Filtering
```kotlin
viewModel.filterByAvailability(50) // Only carparks with 50+ lots
viewModel.filterResults(minLots = 10, maxDistance = 500f) // Within 500m and 10+ lots
```

### Test Sorting
```kotlin
viewModel.sortResults(SortOption.DISTANCE) // Closest first
viewModel.sortResults(SortOption.AVAILABILITY) // Most available first
```

### Test Favorites
```kotlin
viewModel.addToFavorites("CP002") // Add Marina Square
viewModel.removeFromFavorites("CP001") // Remove Orchard Central
val isFav = viewModel.isFavorite("CP003") // Check if favorite
```

---

## ⏱️ Realistic Delays

All mock operations include delays to simulate real API calls:
- **Short delay** (500ms): Search, add/remove favorites, check favorite
- **Medium delay** (1000ms): Load carparks, load profile
- **Long delay** (1500ms): Refresh from API, change password

This helps you test loading states and animations!

---

## 🔄 Switching to Real ViewModels

When repository implementation is complete:

### Before (Mock):
```kotlin
@Composable
fun HomeScreen() {
    val viewModel = MockHomeViewModel()
    // ... rest of code
}
```

### After (Real with Hilt):
```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    // ... same code, no changes needed!
}
```

The interface is identical, so your UI code won't need changes!

---

## 📋 Checklist for UI Development

### For Person 1 (UI Designer):
- [ ] Copy `MockDataProvider.kt` and `MockViewModels.kt` to your project
- [ ] Test HomeScreen with `MockHomeViewModel`
- [ ] Test SearchScreen with `MockSearchViewModel`
- [ ] Test FavouritesScreen with `MockFavouritesViewModel`
- [ ] Verify loading states display correctly
- [ ] Verify error states display correctly
- [ ] Test all button interactions

### For Person 2 (UI Developer):
- [ ] Integrate mock ViewModels into Composable screens
- [ ] Implement search UI with filters
- [ ] Implement favorites UI with add/remove
- [ ] Implement profile UI with edit/password change
- [ ] Test all navigation flows
- [ ] Verify StateFlow updates trigger recomposition
- [ ] Prepare for switching to real ViewModels

---

## 🐛 Troubleshooting

**Q: ViewModel not updating UI?**
A: Make sure you're using `collectAsState()`:
```kotlin
val data by viewModel.data.collectAsState() // ✅ Correct
val data = viewModel.data.value // ❌ Won't trigger recomposition
```

**Q: How do I test empty states?**
A: Modify MockDataProvider to return empty lists temporarily.

**Q: Can I modify the mock data?**
A: Yes! Edit `MockDataProvider.mockCarparks` to add/remove/modify data.

**Q: When should I switch to real ViewModels?**
A: When Person 4 completes repository implementation (Week 2).

---

## 📞 Questions?

Contact Person 3 (ViewModel Layer developer) if you need:
- Additional mock data
- Different test scenarios
- Modifications to mock ViewModels
- Help switching to real ViewModels

---

**Happy UI Development! 🚀**

*These mock ViewModels are designed to unblock your work and let you develop UI screens independently.*
