import Foundation

/// Thin namespace enum for LetSee session configuration helpers.
///
/// Using a caseless enum prevents accidental instantiation and provides a clean
/// Swift namespace for the static helpers.
public enum LetSeeKit {

    /// Creates and returns a new ephemeral `URLSessionConfiguration` with `LetSeeURLProtocol`
    /// injected as the first protocol class and extended timeouts so mock selection never races.
    public static var sessionConfiguration: URLSessionConfiguration {
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 3600
        config.timeoutIntervalForResource = 3600
        config.protocolClasses = [LetSeeURLProtocol.self]
        return config
    }

    /// Prepends `LetSeeURLProtocol` to an existing session configuration's protocol classes.
    ///
    /// - Parameter config: The `URLSessionConfiguration` to modify.
    /// - Returns: The same configuration instance with `LetSeeURLProtocol` prepended.
    @discardableResult
    public static func addLetSeeProtocol(to config: URLSessionConfiguration) -> URLSessionConfiguration {
        config.protocolClasses = [LetSeeURLProtocol.self] + (config.protocolClasses ?? [])
        return config
    }
}
