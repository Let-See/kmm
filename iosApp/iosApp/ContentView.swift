import SwiftUI
import LetSeeUI

struct ContentView: View {
    @State private var breeds: [Breed] = []
    @State private var isLoading = false
    @State private var errorMessage: String?

    private let client = CatApiClient.shared

    var body: some View {
        NavigationView {
            Group {
                if isLoading && breeds.isEmpty {
                    ProgressView("Loading breeds…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let errorMessage, breeds.isEmpty {
                    errorView(message: errorMessage)
                } else {
                    breedList
                }
            }
            .navigationTitle("Cat Breeds")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task { await fetchBreeds() }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .disabled(isLoading)
                }
            }
        }
        .navigationViewStyle(.stack)
        .task {
            await fetchBreeds()
        }
    }

    private var breedList: some View {
        List(breeds) { breed in
            NavigationLink(destination: BreedDetailView(breed: breed)) {
                BreedRow(breed: breed)
            }
        }
        .listStyle(.insetGrouped)
        .refreshable {
            await fetchBreeds()
        }
    }

    @ViewBuilder
    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 48))
                .foregroundColor(.orange)
            Text("Failed to Load")
                .font(.headline)
            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            Button("Retry") {
                Task { await fetchBreeds() }
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func fetchBreeds() async {
        isLoading = true
        errorMessage = nil
        do {
            breeds = try await client.getBreeds()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}

private struct BreedRow: View {
    let breed: Breed

    var body: some View {
        HStack(spacing: 12) {
            if let urlString = breed.image?.url, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure:
                        Image(systemName: "cat")
                            .foregroundColor(.secondary)
                    case .empty:
                        ProgressView()
                    @unknown default:
                        EmptyView()
                    }
                }
                .frame(width: 60, height: 60)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            } else {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(.systemGray5))
                    .frame(width: 60, height: 60)
                    .overlay(
                        Image(systemName: "cat")
                            .foregroundColor(.secondary)
                    )
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(breed.name)
                    .font(.headline)
                Text(breed.origin)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
