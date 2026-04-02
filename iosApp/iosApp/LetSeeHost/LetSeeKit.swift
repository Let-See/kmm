import UIKit
import LetSeeUI

public extension LetSeeKit {

    private(set) static var debugWindow: LetSeeWindow? {
        get { _storage.window }
        set { _storage.window = newValue }
    }

    /// Installs the floating LetSee debug button in the given window scene.
    ///
    /// Call this once from your `SceneDelegate` or `App.init` after the scene is connected.
    /// The button floats above all app content and presents the Compose debug panel on tap.
    ///
    /// - Parameters:
    ///   - letSee: The KMM `LetSee` instance (typically `DefaultLetSee.Companion.shared.letSee`).
    ///   - windowScene: The scene to attach to. Falls back to the first connected `UIWindowScene`.
    static func installDebugUI(letSee: LetSeeCore.LetSee, in windowScene: UIWindowScene? = nil) {
        let scene = windowScene
            ?? UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first
        guard let scene else { return }

        let window = LetSeeWindow(windowScene: scene)
        window.configure(letSee: letSee)
        window.isHidden = false
        debugWindow = window
    }

    /// Removes the floating debug button and tears down its window.
    static func removeDebugUI() {
        debugWindow?.isHidden = true
        debugWindow = nil
    }
}

// MARK: - Associated storage

/// Holder for the stored `LetSeeWindow` reference, since enum extensions
/// cannot have stored properties directly.
private enum _storage {
    static var window: LetSeeWindow?
}
