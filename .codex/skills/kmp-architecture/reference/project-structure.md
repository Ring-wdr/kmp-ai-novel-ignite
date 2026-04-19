# KMP Project Structure

## Contents

- [Module Organization](#module-organization)
- [Shared Module Layers](#shared-module-layers)
- [Feature Organization](#feature-organization)
- [Platform App Structure](#platform-app-structure)

---

## Module Organization

### Standard KMP Template

```
project/
├── shared/                    # KMP shared module
├── composeApp/               # Android app with Compose
├── iosApp/                   # iOS app (Xcode workspace)
├── gradle/
│   └── libs.versions.toml    # Version catalog
├── build.gradle.kts          # Root build
└── settings.gradle.kts       # Module includes
```

### Multi-Module Setup

```
project/
├── core/
│   ├── common/              # Base utilities, extensions
│   ├── network/             # Ktor client, API base
│   ├── database/            # SQLDelight, migrations
│   └── di/                  # Koin modules
├── feature/
│   ├── auth/                # Authentication feature
│   ├── home/                # Home feature
│   └── settings/            # Settings feature
├── app/
│   ├── android/             # Android app
│   └── ios/                 # iOS app
└── gradle/
    └── libs.versions.toml
```

---

## Shared Module Layers

### Clean Architecture in shared/

```
shared/src/commonMain/kotlin/com/example/
├── domain/                   # Business logic (innermost)
│   ├── model/               # Domain entities
│   │   └── User.kt
│   ├── repository/          # Repository interfaces
│   │   └── UserRepository.kt
│   └── usecase/             # Use cases
│       └── GetUserUseCase.kt
├── data/                     # Data layer
│   ├── repository/          # Repository implementations
│   │   └── UserRepositoryImpl.kt
│   ├── local/               # Local data sources
│   │   ├── UserLocalDataSource.kt
│   │   └── UserDao.kt
│   ├── remote/              # Remote data sources
│   │   ├── UserRemoteDataSource.kt
│   │   └── UserApi.kt
│   └── dto/                 # Data transfer objects
│       ├── UserDto.kt
│       └── Mappers.kt
└── di/                       # Dependency injection
    └── Modules.kt
```

### Layer Dependencies

```
┌─────────────────────────────────────────┐
│              Presentation               │
│        (composeApp / iosApp)           │
└───────────────────┬─────────────────────┘
                    │ depends on
┌───────────────────▼─────────────────────┐
│                Domain                   │
│    (models, repository interfaces,      │
│           use cases)                    │
└───────────────────┬─────────────────────┘
                    │ implemented by
┌───────────────────▼─────────────────────┐
│                 Data                    │
│  (repository impls, data sources,       │
│        DTOs, mappers)                   │
└─────────────────────────────────────────┘
```

---

## Feature Organization

### By Feature (Recommended for large projects)

```
shared/src/commonMain/kotlin/com/example/
├── feature/
│   ├── auth/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   └── AuthState.kt
│   │   │   ├── repository/
│   │   │   │   └── AuthRepository.kt
│   │   │   └── usecase/
│   │   │       ├── LoginUseCase.kt
│   │   │       └── LogoutUseCase.kt
│   │   ├── data/
│   │   │   ├── repository/
│   │   │   │   └── AuthRepositoryImpl.kt
│   │   │   └── remote/
│   │   │       └── AuthApi.kt
│   │   └── di/
│   │       └── AuthModule.kt
│   └── library/
│       ├── domain/
│       ├── data/
│       └── di/
└── core/
    ├── network/              # Shared Ktor setup
    ├── database/             # Shared SQLDelight setup
    └── di/                   # Core modules
```

### By Layer (Simpler projects)

```
shared/src/commonMain/kotlin/com/example/
├── domain/
│   ├── model/
│   │   ├── User.kt
│   │   ├── Book.kt
│   │   └── AuthState.kt
│   ├── repository/
│   │   ├── UserRepository.kt
│   │   ├── BookRepository.kt
│   │   └── AuthRepository.kt
│   └── usecase/
│       ├── user/
│       ├── book/
│       └── auth/
├── data/
│   ├── repository/
│   ├── local/
│   ├── remote/
│   └── dto/
└── di/
```

---

## Platform App Structure

### Android (composeApp)

```
composeApp/src/androidMain/kotlin/com/example/
├── MainActivity.kt
├── App.kt                    # Application class
├── ui/
│   ├── navigation/
│   │   └── AppNavigation.kt
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   └── screen/
│       ├── home/
│       │   ├── HomeScreen.kt
│       │   └── HomeViewModel.kt
│       ├── detail/
│       │   ├── DetailScreen.kt
│       │   └── DetailViewModel.kt
│       └── common/
│           ├── LoadingScreen.kt
│           └── ErrorScreen.kt
└── di/
    └── AndroidModule.kt
```

### iOS (iosApp)

```
iosApp/
├── iosApp/
│   ├── iOSApp.swift          # App entry point
│   ├── ContentView.swift     # Root view
│   ├── ViewModels/
│   │   └── ObservableWrappers.swift
│   ├── Views/
│   │   ├── HomeView.swift
│   │   └── DetailView.swift
│   └── Utils/
│       └── Extensions.swift
├── iosApp.xcodeproj
└── iosApp.xcworkspace        # If using CocoaPods
```

---

## Resource Organization

### Shared Resources

```
shared/src/
├── commonMain/
│   └── composeResources/      # Compose Multiplatform resources
│       ├── drawable/
│       ├── values/
│       │   └── strings.xml
│       └── font/
└── androidMain/
    └── res/                   # Android-specific resources
```

### SQLDelight

```
shared/src/commonMain/sqldelight/
├── com/
│   └── example/
│       └── database/
│           ├── User.sq        # User table & queries
│           ├── Book.sq
│           └── migrations/
│               ├── 1.sqm
│               └── 2.sqm
└── database.sq                # Database name config (optional)
```
