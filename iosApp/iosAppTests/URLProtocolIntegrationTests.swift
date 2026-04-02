import XCTest
@testable import iosApp
import LetSeeCore

/// Integration tests for the iOS `LetSeeURLProtocol` bridge.
///
/// These tests verify that `LetSeeURLProtocol` correctly intercepts `URLSession`
/// requests, routes them through the KMM `DefaultLetSee` engine, and delivers
/// mock responses back to the caller.
///
/// **Setup**: A test target does not yet exist in Xcode. See the accompanying
/// `README.md` for instructions on adding one.
final class URLProtocolIntegrationTests: XCTestCase {

    private var letSee: LetSee!
    private var session: URLSession!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        letSee = DefaultLetSee.Companion.shared.letSee

        letSee.setConfigurations(
            config: Configuration(
                isMockEnabled: true,
                shouldCutBaseURLFromURLsTitle: false,
                baseURL: "https://test.example.com"
            )
        )

        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 3600
        config.timeoutIntervalForResource = 3600
        config.protocolClasses = [LetSeeURLProtocol.self]
        session = URLSession(configuration: config)
    }

    override func tearDown() {
        session.invalidateAndCancel()
        session = nil
        drainRequestQueue()

        letSee.setConfigurations(
            config: Configuration(
                isMockEnabled: true,
                shouldCutBaseURLFromURLsTitle: false,
                baseURL: "https://test.example.com"
            )
        )

        super.tearDown()
    }

    // MARK: - Helpers

    /// Returns the current list of pending requests from the KMM requests stack.
    ///
    /// The `replayCache` on a Kotlin `SharedFlow<List<AcceptedRequest>>` is exported
    /// as `[Any]` in Swift. The first element is an `NSArray` of `AcceptedRequest`.
    private func currentRequestQueue() -> [AcceptedRequest] {
        let replayCache = letSee.requestsManager.requestsStack.replayCache
        guard let currentList = replayCache.first as? NSArray else { return [] }
        return currentList.compactMap { $0 as? AcceptedRequest }
    }

    /// Polls `requestsStack.replayCache` until at least `expectedCount` requests
    /// are present, or until `timeout` elapses.
    @discardableResult
    private func waitForRequestCount(
        _ expectedCount: Int,
        timeout: TimeInterval = 5.0,
        file: StaticString = #filePath,
        line: UInt = #line
    ) -> [AcceptedRequest] {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            let queue = currentRequestQueue()
            if queue.count >= expectedCount { return queue }
            RunLoop.current.run(until: Date().addingTimeInterval(0.05))
        }
        let queue = currentRequestQueue()
        XCTAssertGreaterThanOrEqual(
            queue.count, expectedCount,
            "Timed out waiting for \(expectedCount) request(s); found \(queue.count)",
            file: file, line: line
        )
        return queue
    }

    /// Removes all pending requests from the KMM queue via `finish`.
    /// Kotlin `suspend fun finish(request:)` is exported with a completion handler.
    private func drainRequestQueue() {
        let requests = currentRequestQueue()
        guard !requests.isEmpty else { return }
        for accepted in requests {
            letSee.requestsManager.finish(request: accepted.request) { _ in }
        }
        Thread.sleep(forTimeInterval: 0.3)
    }

    // MARK: - Test: Request appears in queue when intercepted

    func testRequestAppearsInQueueWhenIntercepted() {
        let url = URL(string: "https://test.example.com/api/test-intercept")!
        let task = session.dataTask(with: URLRequest(url: url)) { _, _, _ in }
        task.resume()

        let queue = waitForRequestCount(1)
        XCTAssertEqual(queue.count, 1)
        XCTAssertTrue(
            queue[0].request.uri.contains("test-intercept"),
            "Intercepted request URI should contain the path segment"
        )
    }

    // MARK: - Test: Mock response is delivered through URLSession

    func testMockResponseDelivery() {
        let url = URL(string: "https://test.example.com/api/test-mock")!
        let responseExpectation = expectation(description: "URLSession receives mock response")

        var receivedData: Data?
        var receivedResponse: URLResponse?
        var receivedError: Error?

        let task = session.dataTask(with: URLRequest(url: url)) { data, response, error in
            receivedData = data
            receivedResponse = response
            receivedError = error
            responseExpectation.fulfill()
        }
        task.resume()

        let queue = waitForRequestCount(1)
        let acceptedRequest = queue[0].request

        // Build a response using the KMM bridge helper (avoids KotlinByteArray construction).
        let mockBody = #"{"mocked":true}"#.data(using: .utf8)!
        let mockResponse = LiveHandlerBridgeKt.createLiveResponse(
            statusCode: 200,
            headers: ["Content-Type": ["application/json"]],
            bodyData: mockBody,
            statusText: "OK"
        )

        // `respond(request:withResponse:)` is a Kotlin suspend function exported with
        // a trailing completion handler.
        letSee.requestsManager.respond(
            request: acceptedRequest,
            withResponse: mockResponse
        ) { _ in }

        wait(for: [responseExpectation], timeout: 10.0)

        XCTAssertNil(receivedError, "Expected no error, got: \(receivedError?.localizedDescription ?? "")")

        if let data = receivedData, let body = String(data: data, encoding: .utf8) {
            XCTAssertTrue(body.contains("mocked"), "Body should contain mock data; got: \(body)")
        } else {
            XCTFail("Expected non-nil data from mock response")
        }

        if let httpResponse = receivedResponse as? HTTPURLResponse {
            XCTAssertEqual(httpResponse.statusCode, 200)
        } else {
            XCTFail("Expected HTTPURLResponse")
        }
    }

    // MARK: - Test: LetSeeKit.sessionConfiguration includes LetSeeURLProtocol

    func testSessionConfigurationContainsLetSeeURLProtocol() {
        let config = LetSeeKit.sessionConfiguration
        let protocolClasses = config.protocolClasses ?? []

        XCTAssertTrue(
            protocolClasses.contains { $0 == LetSeeURLProtocol.self },
            "LetSeeKit.sessionConfiguration must include LetSeeURLProtocol"
        )
    }

    // MARK: - Test: Mocks disabled bypasses interception

    func testMocksDisabledBypassesInterception() {
        letSee.setConfigurations(
            config: Configuration(
                isMockEnabled: false,
                shouldCutBaseURLFromURLsTitle: false,
                baseURL: "https://test.example.com"
            )
        )

        let url = URL(string: "https://test.example.com/api/test-bypass")!
        let task = session.dataTask(with: URLRequest(url: url)) { _, _, _ in }
        task.resume()

        // Allow enough time for any potential interception to take place.
        let waitExpectation = expectation(description: "Grace period for interception")
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            waitExpectation.fulfill()
        }
        wait(for: [waitExpectation], timeout: 3.0)

        let queue = currentRequestQueue()
        XCTAssertTrue(
            queue.isEmpty,
            "Queue should be empty when mocks are disabled; found \(queue.count) request(s)"
        )

        task.cancel()
    }

    // MARK: - Test: Multiple concurrent requests

    func testMultipleConcurrentRequests() {
        let urls = [
            URL(string: "https://test.example.com/api/concurrent-1")!,
            URL(string: "https://test.example.com/api/concurrent-2")!,
            URL(string: "https://test.example.com/api/concurrent-3")!,
        ]

        for url in urls {
            let task = session.dataTask(with: URLRequest(url: url)) { _, _, _ in }
            task.resume()
        }

        let queue = waitForRequestCount(3)
        XCTAssertEqual(queue.count, 3, "All 3 concurrent requests should appear in the queue")

        let uris = Set(queue.map { $0.request.uri })
        for url in urls {
            XCTAssertTrue(
                uris.contains(url.absoluteString),
                "Queue should contain request for \(url.absoluteString)"
            )
        }
    }
}
