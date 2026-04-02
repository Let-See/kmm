import Foundation

class CatApiClient {

    static let shared = CatApiClient()

    func getBreeds() async throws -> [Breed] {
        let url = URL(string: "https://api.thecatapi.com/v1/breeds")!
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        let (data, _) = try await LetSeeKit.data(for: request)
        return try JSONDecoder().decode([Breed].self, from: data)
    }
}
