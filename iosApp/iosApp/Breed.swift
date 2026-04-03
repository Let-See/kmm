import Foundation

struct Breed: Codable, Identifiable {
    let id: String
    let name: String
    let description: String
    let origin: String
    let temperament: String
    let wikipedia_url: String?
    let life_span: String?
    let weight: Weight?
    let image: BreedImage?
}

struct Weight: Codable {
    let imperial: String?
    let metric: String?
}

struct BreedImage: Codable {
    let id: String?
    let url: String?
}
