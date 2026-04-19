---
name: kmp
description: Kotlin Multiplatform patterns and architecture. Use when working with shared code, source sets, expect/actual declarations, or project configuration.
---

# Kotlin Multiplatform Skill

## Contents

- [Overview](#overview)
- [Directory Structure](#directory-structure)
- [Reference Files](#reference-files)
- [Source Sets](#source-sets)
- [Common Patterns](#common-patterns)
- [Best Practices](#best-practices)

---

## Overview

Kotlin Multiplatform (KMP) enables sharing code across platforms:
- Android (Kotlin/JVM)
- iOS (Kotlin/Native)
- Desktop (Kotlin/JVM)
- Web (Kotlin/JS, Kotlin/WASM)

Key concepts:
- **commonMain**: Shared code that compiles for all targets
- **expect/actual**: Platform-specific implementations
- **Source sets**: Organized code for different targets

---

## Directory Structure

```
project-root/
├── shared/                           # Shared KMP module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/        # Shared code
│       │   └── com/example/
│       │       ├── domain/           # Domain models, use cases
│       │       ├── data/             # Repositories, data sources
│       │       │   ├── local/        # SQLDelight DAOs
│       │       │   └── remote/       # Ktor API clients
│       │       └── di/               # Koin modules
│       ├── commonTest/kotlin/        # Shared tests
│       ├── androidMain/kotlin/       # Android actual implementations
│       ├── iosMain/kotlin/           # iOS actual implementations
│       └── iosTest/kotlin/           # iOS-specific tests
├── composeApp/                       # Android app module
│   ├── build.gradle.kts
│   └── src/
│       └── androidMain/kotlin/
│           └── com/example/
│               ├── ui/               # Compose screens
│               ├── viewmodel/        # ViewModels
│               └── MainActivity.kt
├── iosApp/                           # iOS app (Xcode project)
│   └── iosApp/
│       ├── ContentView.swift
│       └── iOSApp.swift
├── gradle/
│   └── libs.versions.toml           # Version catalog
├── build.gradle.kts                  # Root build file
└── settings.gradle.kts
```

---

## Reference Files

Detailed patterns and examples are in separate reference files:

| Topic | File | Description |
|-------|------|-------------|
| **Project Structure** | [reference/project-structure.md](reference/project-structure.md) | Module organization, build configuration |
| **Expect/Actual** | [reference/expect-actual.md](reference/expect-actual.md) | Platform-specific declarations |
| **Gradle** | [reference/gradle.md](reference/gradle.md) | Build configuration, version catalog |

---

## Source Sets

### Hierarchy

```
commonMain
├── androidMain
├── iosMain
│   ├── iosArm64Main (iOS device)
│   └── iosX64Main (iOS simulator)
└── jvmMain (if JVM target enabled)
```

### commonMain - Shared Code

```kotlin
// shared/src/commonMain/kotlin/com/example/domain/User.kt
data class User(
    val id: String,
    val name: String,
    val email: String
)

// shared/src/commonMain/kotlin/com/example/domain/UserRepository.kt
interface UserRepository {
    suspend fun getUser(id: String): User?
    suspend fun saveUser(user: User)
    fun observeUsers(): Flow<List<User>>
}
```

### Platform Source Sets

```kotlin
// shared/src/androidMain/kotlin/com/example/Platform.android.kt
actual fun getPlatformName(): String = "Android"

// shared/src/iosMain/kotlin/com/example/Platform.ios.kt
actual fun getPlatformName(): String = "iOS"
```

---

## Common Patterns

### Domain Model

```kotlin
// sealed class for result types
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// Extension functions
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}
```

### Repository Pattern

```kotlin
class UserRepositoryImpl(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource
) : UserRepository {

    override suspend fun getUser(id: String): User? {
        return try {
            val remote = remoteDataSource.fetchUser(id)
            localDataSource.saveUser(remote)
            remote
        } catch (e: Exception) {
            localDataSource.getUser(id)
        }
    }

    override fun observeUsers(): Flow<List<User>> {
        return localDataSource.observeUsers()
    }
}
```

### Use Case

```kotlin
class GetUserUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<User> {
        return try {
            val user = userRepository.getUser(userId)
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(UserNotFoundException(userId))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

---

## Best Practices

| Area | Recommendation |
|------|----------------|
| **Architecture** | Clean architecture with domain/data/presentation layers |
| **Dependencies** | Inject via Koin, avoid direct instantiation |
| **Error handling** | Use sealed Result types, not exceptions |
| **Async** | Use suspend functions and Flow |
| **State** | Immutable data classes, StateFlow for UI state |
| **Platform code** | Minimize with expect/actual, keep logic in common |
| **Testing** | Write tests in commonTest when possible |
| **Naming** | Platform files: `Foo.android.kt`, `Foo.ios.kt` |
