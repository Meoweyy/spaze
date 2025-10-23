# Spaze - Smart Parking Application

## SC2006 Software Engineering
**Lab Group:** TDDC | **Team:** 1

**Team Members:**
- Riccardo Franti (U2421618H)
- Ethan Tan (U2322601C)
- Tang Xinbo (U2322024A)
- Heng Tze Hoi (U2422982J)
- Roy Koulik (U2421208L)

---

## Project Overview

Spaze is an Android application that helps users find available parking spaces in Singapore. The app integrates with HDB Carpark Availability API to provide real-time parking information, budget tracking, and personalized features.

---

## System Architecture

### Entity-Control-Boundary (ECB) Architecture

The application follows the ECB architectural pattern as defined in Lab 2 deliverables:

```
┌─────────────────────────────────────────────────────────────┐
│                    BOUNDARY LAYER                           │
│  - SearchUI          - FavouritesUI      - ProfileUI        │
│  - BudgetUI          - HomepageUI        - LoginUI          │
│  - CarparkAPI        - Google Maps API                      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    CONTROL LAYER                            │
│  - SearchController      - FavouritesController             │
│  - BudgetController      - ProfileController                │
│  - HomepageController    - AuthenticationManager            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     ENTITY LAYER                            │
│  - User              - Carparks          - Budget           │
│  - Map               - Favourites        - ParkingSession   │
└─────────────────────────────────────────────────────────────┘
```

### Implementation Architecture (MVVM + Repository Pattern)

```
┌─────────────────────────────────────────────────────────────┐
│                 PRESENTATION LAYER                          │
│  UI Components (Jetpack Compose) + ViewModels               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   DOMAIN LAYER                              │
│  Repositories (Data Access Abstraction)                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    DATA LAYER                               │
│  Local: Room Database (DAOs + Entities)                     │
│  Remote: Retrofit API Services                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Mapping to Class Diagrams

### Entity Classes

The implementation directly maps to the entity class diagram:

**Core Entities:**
- `UserEntity` → User (with userID, userName, email, password, preferences)
- `CarparkEntity` → Carparks (with carparkID, location, availability, pricing)
- `BudgetEntity` → Budget (with budgetID, monthlyBudget, currentSpending)
- `MapLocationEntity` → Map (with location coordinates, routes, distance)
- `FavoriteEntity` → Favourites (links User and Carpark)
- `ParkingSessionEntity` → ParkingSession (tracks active parking)

**Relationships:**
- User has one-to-many relationship with Budget, Favourites, and ParkingSession
- Carpark has many-to-many relationship with User through Favourites
- Map has one-to-one relationship with Carpark

### Control Classes

**Controllers:**
- `SearchController` → Implemented via `CarparkViewModel` + `CarparkRepository`
- `BudgetController` → Implemented via `BudgetViewModel` + `BudgetRepository`
- `FavouritesController` → Implemented via `FavoritesViewModel` + `CarparkRepository`
- `ProfileController` → Implemented via `ProfileViewModel` + `AuthRepository`
- `HomepageController` → Implemented via main navigation coordination
- `AuthenticationManager` → Implemented via `AuthRepository`

### Boundary Classes

**UI Boundaries:**
- `SearchUI`, `FavouritesUI`, `BudgetUI`, `ProfileUI`, `HomepageUI`, `LoginUI`
- Implemented as Jetpack Compose UI components

**External Boundaries:**
- `CarparkAPI` → Implemented via `CarparkApiService` (Retrofit)
- `Google Maps API` → Integrated via Google Maps SDK
- `User Database` → Implemented via Room Database with DAOs

---

## Application Skeleton

### Project Structure

```
Spaze11/
├── app/
│   ├── src/main/java/com/sc2006/spaze/
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── entity/          # Entity Classes
│   │   │   │   │   ├── UserEntity.kt
│   │   │   │   │   ├── CarparkEntity.kt
│   │   │   │   │   ├── BudgetEntity.kt
│   │   │   │   │   ├── FavoriteEntity.kt
│   │   │   │   │   ├── MapLocationEntity.kt
│   │   │   │   │   └── ParkingSessionEntity.kt
│   │   │   │   ├── dao/             # Data Access Objects
│   │   │   │   │   ├── UserDao.kt
│   │   │   │   │   ├── CarparkDao.kt
│   │   │   │   │   ├── BudgetDao.kt
│   │   │   │   │   └── FavoriteDao.kt
│   │   │   │   └── database/
│   │   │   │       └── SpazeDatabase.kt
│   │   │   ├── remote/
│   │   │   │   ├── api/             # API Services
│   │   │   │   │   └── CarparkApiService.kt
│   │   │   │   └── dto/             # Data Transfer Objects
│   │   │   └── repository/          # Repository Layer
│   │   │       ├── AuthRepository.kt
│   │   │       ├── CarparkRepository.kt
│   │   │       ├── BudgetRepository.kt
│   │   │       └── ParkingSessionRepository.kt
│   │   ├── presentation/
│   │   │   ├── viewmodel/           # ViewModels (Controllers)
│   │   │   │   ├── AuthViewModel.kt
│   │   │   │   ├── CarparkViewModel.kt
│   │   │   │   ├── BudgetViewModel.kt
│   │   │   │   ├── FavoritesViewModel.kt
│   │   │   │   └── ProfileViewModel.kt
│   │   │   └── ui/                  # UI Components (Boundaries)
│   │   │       ├── auth/
│   │   │       ├── home/
│   │   │       ├── search/
│   │   │       ├── budget/
│   │   │       └── profile/
│   │   ├── di/                      # Dependency Injection
│   │   │   └── AppModule.kt
│   │   └── SpazeApplication.kt      # Application Entry Point
```

### Technology Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM + Repository Pattern
- **DI:** Hilt (Dagger)
- **Database:** Room
- **Networking:** Retrofit + OkHttp
- **Async:** Coroutines + Flow
- **Maps:** Google Maps SDK

---

## Key Design Principles Applied

### SOLID Principles
- **Single Responsibility:** Each entity, repository, and ViewModel has one clear purpose
- **Dependency Inversion:** ViewModels depend on repository interfaces, not implementations

### GRASP Principles
- **Information Expert:** Entities contain business logic for their data (e.g., `BudgetEntity` calculates budget status)
- **Controller:** ViewModels coordinate between UI and repositories
- **Low Coupling:** Layers communicate through interfaces
- **High Cohesion:** Related functionality grouped (e.g., all auth logic in `AuthRepository`)

### Design Patterns
- **Repository Pattern:** Data access abstraction
- **Dependency Injection:** Hilt manages object creation
- **Observer Pattern:** StateFlow for reactive UI updates
- **Singleton:** Repositories are application-wide singletons

---

## Use Case Implementation

**UC 1.1 - Create Account:** `AuthViewModel` → `AuthRepository` → `UserDao`  
**UC 1.2 - Login Account:** `AuthViewModel` → `AuthRepository` → `UserDao`  
**UC 2.1 - Search Carpark:** `CarparkViewModel` → `CarparkRepository` → `CarparkDao` + `CarparkApiService`  
**UC 2.2 - Favorite Carpark:** `FavoritesViewModel` → `CarparkRepository` → `FavoriteDao`  
**UC 2.3 - Retrieve API Data:** `CarparkRepository` → `CarparkApiService` → `CarparkDao`  
**UC 3.1 - Budget Control:** `BudgetViewModel` → `BudgetRepository` → `BudgetDao`
