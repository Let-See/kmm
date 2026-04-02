import Foundation
import LetSeeCore

public extension LetSeeKit {

    // MARK: - runDataTask

    /// Runs a `URLSessionDataTask` with LetSee interception applied when mocking is enabled.
    ///
    /// Behaviour mirrors the original iOS `LetSee.runDataTask`:
    /// 1. Injects the `LETSEE-LOGGER-ID` header if not already present.
    /// 2. When mocks are enabled, creates a session with `LetSeeURLProtocol` injected and
    ///    calls `addRequest` on the KMM engine to register the interception.
    /// 3. When mocks are disabled, uses `defaultSession` directly.
    ///
    /// - Parameters:
    ///   - defaultSession: The base `URLSession` to use when mocks are disabled.
    ///   - request: The request to execute.
    ///   - completionHandler: Callback invoked with the response data, URL response, or error.
    /// - Returns: The `URLSessionDataTask` (caller must call `.resume()` if needed,
    ///   or use the async overload which handles resumption automatically).
    @discardableResult
    static func runDataTask(
        using defaultSession: URLSession = .shared,
        with request: URLRequest,
        completionHandler: @escaping @Sendable (Data?, URLResponse?, Error?) -> Void
    ) -> URLSessionDataTask {
        var mutableRequest = request
        if mutableRequest.value(forHTTPHeaderField: "LETSEE-LOGGER-ID") == nil {
            mutableRequest.setValue(UUID().uuidString, forHTTPHeaderField: "LETSEE-LOGGER-ID")
        }

        let session: URLSession
        if currentLetSeeConfig().isMockEnabled {
            let config = LetSeeKit.addLetSeeProtocol(to: defaultSession.configuration)
            session = URLSession(configuration: config)

            // Register the request with the KMM engine so it appears in the request stack.
            // `addRequest` is NOT suspend — it launches a coroutine internally and returns immediately.
            let kmmRequest = toLetSeeRequest(mutableRequest)
            DefaultLetSee.Companion.shared.letSee.addRequest(
                request: kmmRequest,
                listener: LetSeeResult { _ in
                    // Response is delivered via URLProtocol callbacks; this listener
                    // is intentionally a no-op for the runDataTask path.
                }
            )
        } else {
            session = defaultSession
        }

        return session.dataTask(with: mutableRequest, completionHandler: completionHandler)
    }

    // MARK: - async overload

    /// Async overload of `runDataTask`. Starts the task and suspends until completion.
    ///
    /// - Parameters:
    ///   - request: The request to execute.
    ///   - session: The base `URLSession` (default: `.shared`).
    /// - Returns: The response `Data` and `URLResponse`.
    /// - Throws: Any networking error.
    static func data(
        for request: URLRequest,
        using session: URLSession = .shared
    ) async throws -> (Data, URLResponse) {
        try await withCheckedThrowingContinuation { continuation in
            let task = runDataTask(using: session, with: request) { data, response, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                guard let data, let response else {
                    continuation.resume(
                        throwing: NSError(
                            domain: "LetSee",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "No data or response received"]
                        )
                    )
                    return
                }
                continuation.resume(returning: (data, response))
            }
            task.resume()
        }
    }
}
