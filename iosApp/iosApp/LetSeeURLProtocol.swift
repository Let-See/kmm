import Foundation
import os.lock
import LetSeeCore

/// A `URLProtocol` subclass that delegates all interception logic to the KMM `LetSeeCore` framework.
///
/// When mock interception is enabled (`DefaultLetSee.letSee.config.value.isMockEnabled == true`),
/// this protocol intercepts `URLSession` requests, converts them to KMM `DefaultRequest` objects,
/// and routes the KMM `Response` back through the standard `URLProtocolClient` callbacks.
///
/// - Note: `@unchecked Sendable` is safe here because all mutable state is protected by
///   `OSAllocatedUnfairLock` and the URL loading system's own synchronisation guarantees.
public final class LetSeeURLProtocol: URLProtocol, @unchecked Sendable {

    // MARK: - Thread-safe KMM request reference

    /// Stores the KMM `Request` created in `startLoading()` so that `stopLoading()` can finish it.
    private let _kmmRequestLock = OSAllocatedUnfairLock<(any Request)?>(initialState: nil)

    private var kmmRequest: (any Request)? {
        get { _kmmRequestLock.withLock { $0 } }
        set { _kmmRequestLock.withLock { $0 = newValue } }
    }

    // MARK: - URLProtocol overrides

    /// Returns a canonical version of `request` with an extended timeout so that the mock
    /// selection UI has time to be used without the request timing out prematurely.
    public override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        var mutableRequest = request
        mutableRequest.timeoutInterval = 3600
        return mutableRequest
    }

    /// Allows this protocol to handle the request when mock interception is enabled.
    public override class func canInit(with request: URLRequest) -> Bool {
        currentLetSeeConfig().isMockEnabled
    }

    /// Converts the Swift `URLRequest` to a KMM `DefaultRequest`, registers it with the KMM
    /// engine via `addRequest(request:listener:)`, and wires the KMM `Result` callbacks back
    /// to the `URLProtocolClient`.
    public override func startLoading() {
        let swiftRequest = self.request
        let client = self.client

        let kmmRequest = toLetSeeRequest(swiftRequest)
        self.kmmRequest = kmmRequest

        let originalURL = swiftRequest.url ?? URL(string: "https://letsee.internal")!

        let result = URLAwareLetSeeResult(originalURL: originalURL, client: client, owner: self)
        DefaultLetSee.Companion.shared.letSee.addRequest(request: kmmRequest, listener: result)
    }

    /// Cleans up the KMM request from the request stack.
    ///
    /// `requestsManager.finish()` is a Kotlin `suspend` function; without SKIE it exports as a
    /// completion-handler-based function. We fire-and-forget inside a `Task {}`.
    public override func stopLoading() {
        guard let kmmReq = self.kmmRequest else { return }
        Task {
            DefaultLetSee.Companion.shared.letSee.requestsManager.finish(request: kmmReq) { _ in }
        }
        self.kmmRequest = nil
    }
}

// MARK: - URLAwareLetSeeResult

/// A `LetSeeCore.Result` implementation that uses the original request `URL` when constructing
/// `HTTPURLResponse` objects, ensuring the response URL matches the intercepted request.
///
/// Safety note: `@unchecked Sendable` — same rationale as `LetSeeResult`: closure/state set
/// once at construction, called exactly once from a KMM coroutine thread.
private final class URLAwareLetSeeResult: LetSeeCore.Result, @unchecked Sendable {
    private let originalURL: URL
    private let client: (any URLProtocolClient)?
    private let owner: URLProtocol

    init(originalURL: URL, client: (any URLProtocolClient)?, owner: URLProtocol) {
        self.originalURL = originalURL
        self.client = client
        self.owner = owner
    }

    func success(response: any Response) {
        let (httpResponse, data) = toURLResponse(response, originalURL: originalURL)
        if let httpResponse {
            client?.urlProtocol(owner, didReceive: httpResponse, cacheStoragePolicy: .notAllowed)
        }
        client?.urlProtocol(owner, didLoad: data ?? Data())
        client?.urlProtocolDidFinishLoading(owner)
    }

    func failure(error: any Response) {
        let nsError = NSError(
            domain: "LetSee",
            code: Int(error.responseCode),
            userInfo: [
                NSLocalizedDescriptionKey: error.errorMessage ?? "LetSee mock failure"
            ]
        )
        client?.urlProtocol(owner, didFailWithError: nsError)
        client?.urlProtocolDidFinishLoading(owner)
    }
}

