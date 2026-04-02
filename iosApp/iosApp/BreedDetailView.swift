import SwiftUI

struct BreedDetailView: View {
    let breed: Breed

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let urlString = breed.image?.url, let url = URL(string: urlString) {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(maxWidth: .infinity, maxHeight: 300)
                                .clipped()
                        case .failure:
                            imagePlaceholder(systemName: "photo.slash", text: "Failed to load image")
                        case .empty:
                            ProgressView()
                                .frame(maxWidth: .infinity, minHeight: 200)
                        @unknown default:
                            EmptyView()
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 12) {
                    Text(breed.description)
                        .font(.body)

                    InfoRow(label: "Origin", value: breed.origin)
                    InfoRow(label: "Temperament", value: breed.temperament)

                    if let lifeSpan = breed.life_span {
                        InfoRow(label: "Life Span", value: "\(lifeSpan) years")
                    }

                    if let weight = breed.weight {
                        if let metric = weight.metric {
                            InfoRow(label: "Weight", value: "\(metric) kg")
                        }
                    }

                    if let wikiURL = breed.wikipedia_url, let url = URL(string: wikiURL) {
                        Link(destination: url) {
                            HStack {
                                Image(systemName: "globe")
                                Text("Wikipedia")
                            }
                            .font(.body)
                        }
                        .padding(.top, 4)
                    }
                }
                .padding(.horizontal)
            }
        }
        .navigationTitle(breed.name)
        .navigationBarTitleDisplayMode(.large)
    }

    @ViewBuilder
    private func imagePlaceholder(systemName: String, text: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: systemName)
                .font(.largeTitle)
                .foregroundColor(.secondary)
            Text(text)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, minHeight: 200)
        .background(Color(.systemGray6))
    }
}

private struct InfoRow: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
                .textCase(.uppercase)
            Text(value)
                .font(.body)
        }
    }
}
