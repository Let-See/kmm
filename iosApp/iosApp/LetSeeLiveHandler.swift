import Foundation
import LetSeeCore

/// Configures the KMM `liveRequestHandler` so that selecting "Live" for a request
/// forwards it to the real server using a plain `URLSession`.
///
/// The `URLSession` uses `URLSessionConfiguration.default` — **not** the LetSee
/// ephemeral configuration — so requests bypass `LetSeeURLProtocol` entirely,
/// preventing an infinite interception loop.
///
/// Call this once during app startup (e.g. in `ContentView.onAppear` or `iOSApp.init`).
func configureLetSeeLiveHandler() {
    let session = URLSession(configuration: .default)
    let letSee = DefaultLetSee.Companion.shared.letSee

    LiveHandlerBridgeKt.setLiveRequestHandler(letSee: letSee) { kmmRequest, completion in
        guard let url = URL(string: kmmRequest.uri) else {
            let error = NSError(
                domain: "LetSeeLiveHandler",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Invalid URL: \(kmmRequest.uri)"]
            )
            completion(nil, error)
            return
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = kmmRequest.requestMethod
        for (key, value) in kmmRequest.headers {
            urlRequest.setValue(value as? String, forHTTPHeaderField: key as? String ?? key.description)
        }

        Task {
            do {
                let (data, urlResponse) = try await session.data(for: urlRequest)
                guard let httpResponse = urlResponse as? HTTPURLResponse else {
                    let error = NSError(
                        domain: "LetSeeLiveHandler",
                        code: -2,
                        userInfo: [NSLocalizedDescriptionKey: "Response is not HTTPURLResponse"]
                    )
                    completion(nil, error)
                    return
                }

                let headers = convertResponseHeaders(httpResponse.allHeaderFields)
                let response = LiveHandlerBridgeKt.createLiveResponse(
                    statusCode: Int32(httpResponse.statusCode),
                    headers: headers,
                    bodyData: data,
                    statusText: HTTPURLResponse.localizedString(forStatusCode: httpResponse.statusCode)
                )
                completion(response, nil)
            } catch {
                completion(nil, error as NSError)
            }
        }
    }
}

/// Converts `[AnyHashable: Any]` header dictionary to `[String: [String]]` for KMM.
private func convertResponseHeaders(_ allHeaderFields: [AnyHashable: Any]) -> [String: [String]] {
    var result: [String: [String]] = [:]
    for (key, value) in allHeaderFields {
        let name = "\(key)"
        let valueStr = "\(value)"
        result[name] = [valueStr]
    }
    return result
}
