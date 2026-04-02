import Foundation
import os.lock
import LetSeeUI

/// A `URLProtocol` subclass that delegates all interception logic to the KMM `LetSeeCore` framework.
///
/// When mock interception is enabled (`DefaultLetSee.letSee.config.value.isMockEnabled == true`),
/// this protocol intercepts `URLSession` requests, converts them to KMM `DefaultRequest` objects,
/// and routes the KMM `Response` back through the standard `URLProtocolClient` callbacks.
///
/// - Note: `@unchecked Sendable` is safe here because all mutable state is protected by
///   `OSAllocatedUnfairLock` and the URL loading system's own synchronisation guarantees.
public final class LetSeeURLProtocol: URLProtocol, @unchecked Sendable {

    private static let handledKey = "LetSeeURLProtocol.handled"

    // MARK: - Thread-safe KMM request reference

    /// Stores the KMM `Request` created in `startLoading()` so that `stopLoading()` can finish it.
    /// Also tracks whether `stopLoading()` has been called so callbacks no-op after stop.
    private let _state = OSAllocatedUnfairLock<(request: (any Request)?, stopped: Bool)>(
        initialState: (nil, false)
    )

    // MARK: - URLProtocol overrides

    /// Returns a canonical version of `request` with an extended timeout so that the mock
    /// selection UI has time to be used without the request timing out prematurely.
    public override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        var mutableRequest = request
        mutableRequest.timeoutInterval = 3600
        return mutableRequest
    }

    /// Allows this protocol to handle the request when mock interception is enabled.
    /// Checks for the handled-key property to prevent recursive interception.
    public override class func canInit(with request: URLRequest) -> Bool {
        guard URLProtocol.property(forKey: handledKey, in: request) == nil else { return false }
        return currentLetSeeConfig().isMockEnabled
    }

    /// Converts the Swift `URLRequest` to a KMM `DefaultRequest`, registers it with the KMM
    /// engine via `addRequest(request:listener:)`, and wires the KMM `Result` callbacks back
    /// to the `URLProtocolClient`.
    public override func startLoading() {
        let swiftRequest = self.request
        let client = self.client

        guard let mutableRequest = (swiftRequest as NSURLRequest).mutableCopy() as? NSMutableURLRequest else { return }
        URLProtocol.setProperty(true, forKey: Self.handledKey, in: mutableRequest)

        let kmmRequest = toLetSeeRequest(swiftRequest)
        _state.withLock { $0.request = kmmRequest }

        let originalURL = swiftRequest.url ?? URL(string: "https://letsee.internal")!

        let result = URLAwareLetSeeResult(originalURL: originalURL, client: client, owner: self, stateRef: _state)
        DefaultLetSee.Companion.shared.letSee.addRequest(request: kmmRequest, listener: result)
    }

    /// Cleans up the KMM request from the request stack.
    ///
    /// `requestsManager.finish()` is a Kotlin `suspend` function; without SKIE it exports as a
    /// completion-handler-based function. We fire-and-forget inside a `Task {}`.
    /// Atomically reads and nils the request reference to avoid TOCTOU races.
    public override func stopLoading() {
        let kmmReq: (any Request)? = _state.withLock { state in
            state.stopped = true
            let req = state.request
            state.request = nil
            return req
        }
        guard let kmmReq else { return }
        Task {
            DefaultLetSee.Companion.shared.letSee.requestsManager.finish(request: kmmReq) { _ in }
        }
    }
}

// MARK: - URLAwareLetSeeResult

/// A `LetSeeCore.Result` implementation that uses the original request `URL` when constructing
/// `HTTPURLResponse` objects, ensuring the response URL matches the intercepted request.
///
/// Guards against double invocation with an atomic `delivered` flag, and no-ops after the
/// owning `LetSeeURLProtocol` has called `stopLoading()`.
private final class URLAwareLetSeeResult: LetSeeCore.Result, @unchecked Sendable {
    private let originalURL: URL
    private let client: (any URLProtocolClient)?
    private let owner: URLProtocol
    private let stateRef: OSAllocatedUnfairLock<(request: (any Request)?, stopped: Bool)>
    private let delivered = OSAllocatedUnfairLock(initialState: false)

    init(
        originalURL: URL,
        client: (any URLProtocolClient)?,
        owner: URLProtocol,
        stateRef: OSAllocatedUnfairLock<(request: (any Request)?, stopped: Bool)>
    ) {
        self.originalURL = originalURL
        self.client = client
        self.owner = owner
        self.stateRef = stateRef
    }

    func success(response: any Response) {
        guard markDelivered() else { return }
        let (httpResponse, data) = toURLResponse(response, originalURL: originalURL)
        if let httpResponse {
            client?.urlProtocol(owner, didReceive: httpResponse, cacheStoragePolicy: .notAllowed)
        }
        client?.urlProtocol(owner, didLoad: data ?? Data())
        client?.urlProtocolDidFinishLoading(owner)
    }

    func failure(error: any Response) {
        guard markDelivered() else { return }
        let nsError = NSError(
            domain: "LetSee",
            code: Int(error.responseCode),
            userInfo: [
                NSLocalizedDescriptionKey: error.errorMessage ?? "LetSee mock failure"
            ]
        )
        client?.urlProtocol(owner, didFailWithError: nsError)
    }

    /// Returns `true` the first time it is called, `false` on subsequent calls.
    /// Also returns `false` if the owning protocol has already stopped.
    private func markDelivered() -> Bool {
        let stopped = stateRef.withLock { $0.stopped }
        guard !stopped else { return false }
        return delivered.withLock { alreadySent in
            guard !alreadySent else { return false }
            alreadySent = true
            return true
        }
    }
}

