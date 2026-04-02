import Foundation
import LetSeeCore

// MARK: - URLRequest → KMM DefaultRequest

/// Converts a Swift `URLRequest` into a KMM `DefaultRequest`.
func toLetSeeRequest(_ urlRequest: URLRequest) -> DefaultRequest {
    let uri = urlRequest.url?.absoluteString ?? ""
    let path = urlRequest.url.map { url -> String in
        url.path
            .components(separatedBy: "/")
            .filter { !$0.isEmpty }
            .joined(separator: "/")
    } ?? ""
    return DefaultRequest(
        headers: urlRequest.allHTTPHeaderFields ?? [:],
        requestMethod: urlRequest.httpMethod ?? "GET",
        uri: uri,
        path: path
    )
}

// MARK: - KMM Response → (HTTPURLResponse?, Data?)

/// Converts a KMM `Response` into a Swift `HTTPURLResponse` and `Data?`.
func toURLResponse(_ response: any Response, originalURL: URL) -> (HTTPURLResponse?, Data?) {
    let statusCode = Int(response.responseCode)
    let flatHeaders = response.headers.mapValues { $0.joined(separator: ", ") }
    let httpResponse = HTTPURLResponse(
        url: originalURL,
        statusCode: statusCode,
        httpVersion: nil,
        headerFields: flatHeaders
    )
    let data = response.byteResponse?.toData()
    return (httpResponse, data)
}

// MARK: - LetSeeResult

/// Swift implementation of the KMM `Result` protocol.
///
/// Holds a completion closure and routes KMM `success(response:)` / `failure(error:)`
/// callbacks into a Swift `Result<(HTTPURLResponse?, Data?), Error>` handler.
///
/// Safety note: marked `@unchecked Sendable` because the closure is set once at
/// construction time and invoked exactly once from an arbitrary KMM coroutine thread.
/// The closure itself is declared `@Sendable` to enforce that its captures are safe.
final class LetSeeResult: LetSeeCore.Result, @unchecked Sendable {

    private let completion: @Sendable (Swift.Result<(HTTPURLResponse?, Data?), Error>) -> Void

    init(completion: @escaping @Sendable (Swift.Result<(HTTPURLResponse?, Data?), Error>) -> Void) {
        self.completion = completion
    }

    func success(response: any Response) {
        let fallbackURL = URL(string: "https://letsee.internal")!
        let (httpResponse, data) = toURLResponse(response, originalURL: fallbackURL)
        completion(.success((httpResponse, data)))
    }

    func failure(error: any Response) {
        let nsError = NSError(
            domain: "LetSee",
            code: Int(error.responseCode),
            userInfo: [
                NSLocalizedDescriptionKey: error.errorMessage ?? "LetSee mock failure"
            ]
        )
        completion(.failure(nsError))
    }
}

// MARK: - Mock sealed class typealiases

/// KMM `Mock` sealed class hierarchy as exported to Swift.
///
/// The Kotlin/Native interop exports them as:
///   `Mock`          — base class
///   `Mock.SUCCESS`  — success variant
///   `Mock.FAILURE`  — failure variant
///   `Mock.ERROR`    — error variant
///   `Mock.LIVE`     — live (passthrough) variant
///   `Mock.CANCEL`   — cancel variant
///
/// These typealiases provide more idiomatic Swift names where needed.
typealias LetSeeMock = Mock
typealias LetSeeMockSuccess = Mock.SUCCESS
typealias LetSeeMockFailure = Mock.FAILURE
typealias LetSeeMockError = Mock.ERROR
typealias LetSeeMockLive = Mock.LIVE
typealias LetSeeMockCancel = Mock.CANCEL

// MARK: - Configuration access helper

/// Returns the current `Configuration` value from the KMM `DefaultLetSee` singleton.
/// The KMM `config` property is a `StateFlow` whose `.value` is typed as `Any?` in Obj-C/Swift.
/// We cast it to `Configuration`; if the cast fails we fall back to `Configuration.default_`.
func currentLetSeeConfig() -> Configuration {
    let letSee = DefaultLetSee.Companion.shared.letSee
    if let cfg = letSee.config.value as? Configuration {
        return cfg
    }
    return Configuration.Companion.shared.default_
}
