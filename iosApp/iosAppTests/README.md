# iosAppTests — LetSee URLProtocol Integration Tests

## Adding the Test Target to Xcode

The `iosApp.xcodeproj` does not yet include a test target. Follow these steps to create one:

1. Open `kmm/iosApp/iosApp.xcodeproj` in Xcode.
2. **File → New → Target…** → choose **iOS Unit Testing Bundle**.
3. Set the **Product Name** to `iosAppTests`.
4. Set **Target to be Tested** to `iosApp`.
5. Click **Finish**.
6. Drag all `.swift` files from this directory (`kmm/iosApp/iosAppTests/`) into the new `iosAppTests` group in the Xcode project navigator.
7. In **Build Phases → Link Binary With Libraries**, ensure these frameworks are linked:
   - `LetSeeCore.framework` (from the KMM build)
   - `LetSeeUI.framework` (if any tests reference UI helpers)
8. Verify the test target's **Build Settings → Framework Search Paths** includes the path to the KMM framework output (typically `$(SRCROOT)/../LetSeeCore/build/...`).

## Running the Tests

```
⌘U          # Run all tests in Xcode
```

Or from the command line (once the scheme includes the test target):

```bash
xcodebuild test \
  -project kmm/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16'
```

## Test Overview

| Test | What it verifies |
|------|-----------------|
| `testRequestAppearsInQueueWhenIntercepted` | A `URLSession` request intercepted by `LetSeeURLProtocol` shows up in `requestsManager.requestsStack` |
| `testMockResponseDelivery` | Responding to a queued request via `requestsManager.respond(request:withResponse:)` delivers data back through the `URLSession` completion handler |
| `testSessionConfigurationContainsLetSeeURLProtocol` | `LetSeeKit.sessionConfiguration` includes `LetSeeURLProtocol` in `protocolClasses` |
| `testMocksDisabledBypassesInterception` | When `isMockEnabled` is `false`, requests are **not** intercepted and do not appear in the queue |
| `testMultipleConcurrentRequests` | Three simultaneous requests all appear in the queue concurrently |

## Notes

- Tests use the `DefaultLetSee.Companion.shared.letSee` singleton. Each test resets configuration in `setUp` / `tearDown`.
- Kotlin `suspend` functions (e.g. `respond`, `finish`) are exported as completion-handler APIs since SKIE is not enabled.
- `LiveHandlerBridgeKt.createLiveResponse(...)` is used to construct mock `Response` objects from Swift-native `Data`, avoiding manual `KotlinByteArray` construction.
