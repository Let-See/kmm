import SwiftUI
import LetSeeCore

struct ContentView: View {
    @State private var statusText = "Tap to run request"

    var body: some View {
        VStack(spacing: 20) {
            Text(statusText)
                .multilineTextAlignment(.center)
                .padding()

            Button("Run via LetSeeURLProtocol") {
                runSampleRequest()
            }

            Button("Run async via LetSeeKit") {
                Task { await runAsyncRequest() }
            }
        }
        .onAppear {
            setupLetSee()
        }
    }

    // MARK: - Setup

    private func setupLetSee() {
        var letSee = DefaultLetSee.Companion.shared.letSee

        letSee.setMocks(path: Bundle.main.bundlePath + "/Mocks")
        letSee.setScenarios(path: Bundle.main.bundlePath + "/Scenarios")

        letSee.setConfigurations(
            config: Configuration(isMockEnabled: true, shouldCutBaseURLFromURLsTitle: false, baseURL: "")
        )

        // Wire the live request handler so "Live" forwards to the real server
        // via a plain URLSession (bypasses LetSeeURLProtocol).
        configureLetSeeLiveHandler()

        if let scenario = letSee.scenarios.first {
            letSee.requestsManager.scenarioManager.activate(scenario: scenario) { _ in }
        }
    }

    // MARK: - URLProtocol-based request (callback style via LetSeeKit.runDataTask)

    private func runSampleRequest() {
        let url = URL(string: "https://google.com/api/arrangement-manager/client-api/v2/productsummary/context/arrangements")!
        var request = URLRequest(url: url)
        request.httpMethod = "GET"

        let task = LetSeeKit.runDataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error {
                    statusText = "Error: \(error.localizedDescription)"
                } else if let data, let body = String(data: data, encoding: .utf8) {
                    statusText = "Success: \(body.prefix(80))"
                } else if let response = response as? HTTPURLResponse {
                    statusText = "HTTP \(response.statusCode) — no body"
                } else {
                    statusText = "Completed (no data)"
                }
            }
        }
        task.resume()
        statusText = "Request in flight…"
    }

    // MARK: - async convenience overload

    private func runAsyncRequest() async {
        let url = URL(string: "https://google.com/api/arrangement-manager/client-api/v2/productsummary/context/arrangements")!
        var request = URLRequest(url: url)
        request.httpMethod = "GET"

        do {
            let (data, response) = try await LetSeeKit.data(for: request)
            let body = String(data: data, encoding: .utf8) ?? "(binary)"
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            await MainActor.run {
                statusText = "Async HTTP \(code): \(body.prefix(60))"
            }
        } catch {
            await MainActor.run {
                statusText = "Async error: \(error.localizedDescription)"
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

// MARK: - Path normalisation helper (used by DefaultRequestToMockMapper)

struct DefaultRequestToMockMapper {
    static func transform(request: String, using mocks: [String: [Mock]]) -> [Mock]? {
        let components = request
            .components(separatedBy: "/")
            .filter { !$0.isEmpty }
            .joined(separator: "/")

        if let requestMocks = mocks[components.mockKeyNormalised] {
            return Array(requestMocks)
        } else {
            return nil
        }
    }
}

extension String {
    static var empty: String { "" }

    /// Lowercases and wraps the string between two forward slashes.
    var mockKeyNormalised: String {
        var folder = self.lowercased()
        folder = folder.starts(with: "/") ? folder : "/" + folder
        folder = folder.last == "/" ? folder : folder + "/"
        return folder
    }
}
