import SwiftUI
import LetSeeCore

@main
struct iOSApp: App {

    init() {
        setupLetSee()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
                        LetSeeKit.installDebugUI(
                            letSee: DefaultLetSee.Companion.shared.letSee,
                            in: scene
                        )
                    }
                }
        }
    }

    private func setupLetSee() {
        let letSee = DefaultLetSee.Companion.shared.letSee

        letSee.setMocks(path: Bundle.main.bundlePath + "/Mocks")
        letSee.setScenarios(path: Bundle.main.bundlePath + "/Scenarios")
        letSee.setConfigurations(
            config: Configuration(isMockEnabled: true, shouldCutBaseURLFromURLsTitle: false, baseURL: "")
        )

        configureLetSeeLiveHandler()
    }
}
