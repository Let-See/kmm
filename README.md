
https://user-images.githubusercontent.com/13612410/212567902-b967615b-8f14-4c4c-b128-8e1529eade2c.mp4

# LetSee!

LetSee provides an easy way to mock API responses at runtime for **Android** and **iOS** apps built with Kotlin Multiplatform. Save server responses as JSON files, pick the one you want on the fly, and test every edge case without restarting or changing code.

## Features

- **Runtime mock selection** — intercept any API call and choose which JSON response to return
- **Scenarios** — define ordered steps so LetSee auto-responds to a sequence of requests
- **On-the-fly editing** — paste or edit JSON directly at runtime
- **Live-to-server fallback** — let individual requests hit the real server while mocking others
- **Floating debug overlay** — draggable button with quick-access mock picker (Android Compose & iOS UIKit)

## Installation

### Android (Gradle — Kotlin DSL)

Add the Maven Central dependency to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("ai.arrox.letsee.kmm:LetSeeCore:0.0.8")
    // Optional: debug overlay UI (Compose Multiplatform)
    implementation("ai.arrox.letsee.kmm:LetSeeUI:0.0.8")
}
```

Make sure `mavenCentral()` is in your `settings.gradle.kts` repositories:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
```

### iOS (Swift Package Manager)

Add the KMM repository as a Swift Package dependency:

1. In Xcode, go to **File → Add Package Dependencies**
2. Enter the repository URL: `https://github.com/Let-See/kmm`
3. Select the version/branch you want

## Quick Start — Android

### 1. Add mock JSON files

Place your mock files in `src/main/assets/Mocks/`:

```
assets/
└── Mocks/
    ├── products/
    │   ├── success_productList.json
    │   └── error_notFound.json
    └── categories/
        ├── success_categoryList.json
        └── success_emptyList.json
```

### 2. Initialize LetSee

In your `Application` class or main `Activity`:

```kotlin
import io.github.letsee.DefaultLetSee
import io.github.letsee.initLetSee

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initLetSee()
    }
}
```

### 3. Add the debug overlay (optional)

Wrap your app content with `LetSeeOverlay` in your root composable:

```kotlin
import io.github.letsee.ui.LetSeeOverlay
import io.github.letsee.DefaultLetSee

setContent {
    LetSeeOverlay(DefaultLetSee.letSee) {
        MyAppContent()
    }
}
```

This adds a draggable floating button that lets you pick mock responses at runtime.

### 4. Intercept network requests

Use the LetSee OkHttp interceptor to route requests through LetSee:

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(DefaultLetSee.letSee.interceptor())
    .build()
```

When mocking is enabled, LetSee intercepts matching requests and returns the mock you select. When disabled, requests pass through to the real server.

## Quick Start — iOS

### 1. Add mock JSON files

Add a `Mocks` folder to your app bundle with JSON files organized by endpoint path.

### 2. Setup LetSee

```swift
import LetSeeCore

// In your AppDelegate or app startup
let config = LetSeeConfiguration(
    baseURL: URL(string: "https://api.example.com/")!,
    isMockEnabled: true
)
LetSee.shared.config(config)
LetSee.shared.addMocks(from: Bundle.main.bundlePath + "/Mocks")
```

### 3. Add the floating overlay

```swift
// Create the LetSee debug window
let letSeeWindow = LetSeeWindow(frame: window.frame)
letSeeWindow.windowScene = window.windowScene
```

## Mock file conventions

- **`success_*.json`** — successful response mock (default if no prefix)
- **`error_*.json`** — error response mock

### Path mapping (`.ls.global.json`)

Place a `.ls.global.json` in your Mocks root to map folders to complex URL paths:

```json
{
    "maps": [
        { "folder": "/products", "to": "/v1/staging" },
        { "folder": "/categories", "to": "/v2/api" }
    ]
}
```

## Scenarios

Define automated response sequences as `.plist` (iOS) or JSON files. Each step specifies a folder and response file name. When a scenario is active, LetSee auto-responds in order without manual selection.

## Architecture

```
kmm/
├── LetSeeCore/     # Shared KMP logic (mock loading, interception, scenarios)
├── LetSeeUI/       # Compose Multiplatform debug overlay
├── androidApp/     # Android showcase app
└── iosApp/         # iOS showcase app
```

## Publishing

Artifacts are published to Maven Central under the group `ai.arrox.letsee.kmm`.

| Artifact | Description |
|---|---|
| `LetSeeCore` | Core library — mock loading, interception, scenarios |
| `LetSeeUI` | Debug overlay — floating button, mock picker (Compose) |

## License

MIT
