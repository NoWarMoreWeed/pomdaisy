import PomDaisyCompose
import SwiftUI
import UIKit

/// The Android app itself (its Kotlin / Compose code, ios/compose): its Library first, its other screens from there.
struct AndroidApp: UIViewControllerRepresentable {
    var onMissing: (String) -> Void = { _ in }

    func makeUIViewController(context: Context) -> UIViewController {
        MainKt.AppViewController(onMissing: { name in onMissing(name) })
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}

/// A test page: the Android app on the iPhone, from the same code. Screens iOS hasn't got yet say so.
struct AndroidPreview: View {
    @Environment(\.dismiss) private var dismiss
    @State private var missing: String?

    var body: some View {
        ZStack(alignment: .topTrailing) {
            AndroidApp(onMissing: { missing = $0 })
                .ignoresSafeArea()
            Button("Schließen") { dismiss() }
                .buttonStyle(.borderedProminent)
                .padding(10)
        }
        .statusBarHidden()
        .alert("Noch nicht auf iOS", isPresented: Binding(get: { missing != nil }, set: { if !$0 { missing = nil } })) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(missing ?? "")
        }
    }
}
