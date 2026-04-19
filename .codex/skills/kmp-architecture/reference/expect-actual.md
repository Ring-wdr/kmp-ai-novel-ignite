# Expect/Actual Declarations

## Contents

- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Common Patterns](#common-patterns)
- [Platform-Specific APIs](#platform-specific-apis)
- [Best Practices](#best-practices)

---

## Overview

`expect/actual` is KMP's mechanism for platform-specific code:
- `expect` declares an API in commonMain
- `actual` provides implementation in platform source sets

---

## Basic Usage

### Functions

```kotlin
// commonMain
expect fun getPlatformName(): String

// androidMain
actual fun getPlatformName(): String = "Android ${Build.VERSION.SDK_INT}"

// iosMain
actual fun getPlatformName(): String = UIDevice.currentDevice.systemName()
```

### Classes

```kotlin
// commonMain
expect class Platform() {
    val name: String
    val version: String
}

// androidMain
actual class Platform actual constructor() {
    actual val name: String = "Android"
    actual val version: String = Build.VERSION.RELEASE
}

// iosMain
actual class Platform actual constructor() {
    actual val name: String = UIDevice.currentDevice.systemName()
    actual val version: String = UIDevice.currentDevice.systemVersion()
}
```

### Objects

```kotlin
// commonMain
expect object AppContext {
    fun initialize(context: Any?)
}

// androidMain
actual object AppContext {
    private lateinit var appContext: Context

    actual fun initialize(context: Any?) {
        appContext = context as Context
    }

    fun get(): Context = appContext
}

// iosMain
actual object AppContext {
    actual fun initialize(context: Any?) {
        // No-op on iOS
    }
}
```

---

## Common Patterns

### UUID Generation

```kotlin
// commonMain
expect fun randomUUID(): String

// androidMain
import java.util.UUID
actual fun randomUUID(): String = UUID.randomUUID().toString()

// iosMain
import platform.Foundation.NSUUID
actual fun randomUUID(): String = NSUUID().UUIDString()
```

### Current Timestamp

```kotlin
// commonMain
expect fun currentTimeMillis(): Long

// androidMain
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

// iosMain
import platform.Foundation.NSDate
actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
```

### Dispatchers

```kotlin
// commonMain
expect val ioDispatcher: CoroutineDispatcher

// androidMain
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

// iosMain
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
```

### Logging

```kotlin
// commonMain
expect fun logDebug(tag: String, message: String)
expect fun logError(tag: String, message: String, throwable: Throwable? = null)

// androidMain
import android.util.Log
actual fun logDebug(tag: String, message: String) {
    Log.d(tag, message)
}
actual fun logError(tag: String, message: String, throwable: Throwable?) {
    Log.e(tag, message, throwable)
}

// iosMain
actual fun logDebug(tag: String, message: String) {
    println("DEBUG [$tag]: $message")
}
actual fun logError(tag: String, message: String, throwable: Throwable?) {
    println("ERROR [$tag]: $message ${throwable?.message ?: ""}")
}
```

---

## Platform-Specific APIs

### File Storage

```kotlin
// commonMain
expect class FileStorage {
    fun read(filename: String): String?
    fun write(filename: String, content: String)
    fun delete(filename: String): Boolean
}

// androidMain
actual class FileStorage(private val context: Context) {
    private val filesDir = context.filesDir

    actual fun read(filename: String): String? {
        val file = File(filesDir, filename)
        return if (file.exists()) file.readText() else null
    }

    actual fun write(filename: String, content: String) {
        File(filesDir, filename).writeText(content)
    }

    actual fun delete(filename: String): Boolean {
        return File(filesDir, filename).delete()
    }
}

// iosMain
import platform.Foundation.*
actual class FileStorage {
    private val documentsDir: String
        get() = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String

    actual fun read(filename: String): String? {
        val path = "$documentsDir/$filename"
        return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
    }

    actual fun write(filename: String, content: String) {
        val path = "$documentsDir/$filename"
        (content as NSString).writeToFile(path, true, NSUTF8StringEncoding, null)
    }

    actual fun delete(filename: String): Boolean {
        val path = "$documentsDir/$filename"
        return NSFileManager.defaultManager.removeItemAtPath(path, null)
    }
}
```

### Secure Storage (Keychain/Keystore)

```kotlin
// commonMain
expect class SecureStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

// androidMain - Uses EncryptedSharedPreferences
actual class SecureStorage(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    actual fun getString(key: String): String? = prefs.getString(key, null)
    actual fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    actual fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

// iosMain - Uses Keychain
actual class SecureStorage {
    // Keychain implementation...
}
```

---

## Best Practices

| Area | Recommendation |
|------|----------------|
| **Minimize usage** | Keep expect/actual to platform essentials |
| **File naming** | Use `.android.kt`, `.ios.kt` suffixes |
| **Interfaces first** | Define interface in common, inject impl |
| **Constructor** | Match constructor signatures exactly |
| **Testing** | Create test implementations in commonTest |
| **Defaults** | Provide sensible defaults where possible |

### Prefer Interface + DI Over Expect/Actual

```kotlin
// Better approach for most cases
// commonMain
interface Logger {
    fun debug(message: String)
    fun error(message: String, throwable: Throwable?)
}

// androidMain
class AndroidLogger : Logger {
    override fun debug(message: String) = Log.d("App", message)
    override fun error(message: String, throwable: Throwable?) =
        Log.e("App", message, throwable)
}

// Koin module
val androidModule = module {
    single<Logger> { AndroidLogger() }
}
```

### When to Use Expect/Actual

Use expect/actual for:
- Platform primitives (UUID, timestamps)
- Platform singletons (Dispatchers)
- Factory functions
- Simple utility functions

Use interface + DI for:
- Complex services
- Testable components
- Things with dependencies
