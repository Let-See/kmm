import SwiftUI
import LetSeeShared

struct ContentView: View {
	var body: some View {
		Text("greet")
            .onAppear(perform: {

                let path = Bundle.main.url(forResource: "somejson", withExtension: "json")?.absoluteString


//                print(GlobalMockDirectoryConfig.Companion().exists(inDirectory: Bundle.main.bundlePath + "/Mocks"))
                let x = MocksDirectoryProcessor(fileNameProcessor: JSONFileNameProcessor(cleaner: JSONFileNameCleaner()), mockProcessor: DefaultMockProcessor(fileDataFetcher: DefaultFileFetcher()), directoryFilesFetcher: DefaultDirectoryFilesFetcher(), globalMockDirectoryConfig: nil)

               var result = x.process(path: Bundle.main.bundlePath + "/Mocks")
                print("@@",String(data: result.first!.value[0].response!.byteResponse!.toData(), encoding: .utf8))
            })
	}

}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}
