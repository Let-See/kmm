import SwiftUI
import LetSeeShared

struct ContentView: View {
    var body: some View {
        Text("greet")
            .onAppear(perform: {

                let path = Bundle.main.url(forResource: "somejson", withExtension: "json")?.absoluteString

                let fileNameProcessor = JSONFileNameProcessor(cleaner: JSONFileNameCleaner())
                //                print(GlobalMockDirectoryConfig.Companion().exists(inDirectory: Bundle.main.bundlePath + "/Mocks"))
                let x = MocksDirectoryProcessor(fileNameProcessor: fileNameProcessor,
                                                mockProcessor: DefaultMockProcessor(fileDataFetcher: DefaultFileFetcher()),
                                                directoryFilesFetcher: DefaultDirectoryFilesFetcher(),
                                                globalMockDirectoryConfig: nil)

                var result = x.process(path: Bundle.main.bundlePath + "/Mocks")
                print("@@",String(data: result.first!.value[0].response!.byteResponse!.toData(), encoding: .utf8))

                let scenarios = DefaultScenariosDirectoryProcessor(directoryFilesFetcher: DefaultDirectoryFilesFetcher(), fileNameProcessor: fileNameProcessor,
                                                                   scenarioFileInformationProcessor: DefaultScenarioFileInformation(),
                                                                   globalMockDirectoryConfig: DefaultGlobalMockDirectoryConfiguration(maps: [.init(folder: "/arrangements", to: "/api/arrangement-manager/client-api/v2/productsummary/context")])) { i in
                    DefaultRequestToMockMapper.transform(request: i, using: result) ?? []
                }

                let scenarioResults = scenarios.process(path:  Bundle.main.bundlePath + "/Scenarios")
                print(scenarioResults)
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
