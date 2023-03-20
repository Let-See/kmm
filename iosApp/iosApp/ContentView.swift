import SwiftUI
import LetSeeShared
class DefaultResult: LetSeeShared.Result {
    func failure(error: Response) {
        print("@@ error", error)
    }

    func success(response: Response) {
        guard let data = response.byteResponse?.toData() else {return}
        print("@@ success", String(data: data, encoding: .utf8))
    }
}
struct ContentView: View {
    var body: some View {
        Text("greet")
            .onAppear(perform: {

                let path = Bundle.main.url(forResource: "somejson", withExtension: "json")?.absoluteString
                var letSee = DefaultLetSee.Companion.shared.letSee
                letSee.setMocks(path: Bundle.main.bundlePath + "/Mocks")
                letSee.setScenarios(path: Bundle.main.bundlePath + "/Scenarios")

                if let scenario = letSee.scenarios.first {
                    letSee.requestsManager.scenarioManager.activate(scenario: scenario) { _ in

                    }
                }

                letSee.addRequest(request: DefaultRequest(headers: [:], requestMethod: "GET", uri: "https://google.com/api/arrangement-manager/client-api/v2/productsummary/context/arrangements", path: "api/arrangement-manager/client-api/v2/productsummary/context/arrangements"), listener: DefaultResult())
                letSee.addRequest(request: DefaultRequest(headers: [:], requestMethod: "GET", uri: "https://google.com/api/arrangement-manager/client-api/v2/productsummary/context/arrangements", path: "api/arrangement-manager/client-api/v2/productsummary/context/arrangements"), listener: DefaultResult())
                letSee.addRequest(request: DefaultRequest(headers: [:], requestMethod: "GET", uri: "https://google.com/api/arrangement-manager/client-api/v2/productsummary/context/arrangements", path: "api/arrangement-manager/client-api/v2/productsummary/context/arrangements"), listener: DefaultResult())
            })
    }

}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

struct DefaultRequestToMockMapper {
    static func transform(request: String, using mocks: [String : [Mock]]) -> [Mock]? {
        let components = request
            .components(separatedBy: "/")
            .filter({!$0.isEmpty})
            .joined(separator: "/")

        if let requestMocks = mocks[components.mockKeyNormalised] {
            return Array(requestMocks)
        } else {
            return nil
        }
    }
}

extension String {
    static var empty: String {
        return ""
    }

    /// LowerCases and wraps the string between two backslash
    var mockKeyNormalised: String {
        var folder = self.lowercased()
        folder = folder.starts(with: "/") ? folder : "/" + folder
        folder = folder.last == "/" ? folder : folder + "/"
        return folder
    }
}
