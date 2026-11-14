import PomDaisyCompose
import SwiftUI
import UIKit

/// One of the Android app's own screens (its Kotlin / Compose code, ios/compose), shown as is.
struct AndroidScreen: UIViewControllerRepresentable {
    enum Screen { case library, settings }
    let screen: Screen

    func makeUIViewController(context: Context) -> UIViewController {
        switch screen {
        case .library: return MainKt.LibraryViewController()
        case .settings: return MainKt.SettingsViewController()
        }
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}

/// A test page: the Android app's Library and Settings on the iPhone, from the same code.
struct AndroidPreview: View {
    @Environment(\.dismiss) private var dismiss
    @State private var screen = AndroidScreen.Screen.library

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Group {
                if screen == .library { AndroidScreen(screen: .library) } else { AndroidScreen(screen: .settings) }
            }
            .ignoresSafeArea()
            HStack(spacing: 8) {
                Button(screen == .library ? "Einstellungen" : "Bibliothek") {
                    screen = screen == .library ? .settings : .library
                }
                Button("Schließen") { dismiss() }
            }
            .buttonStyle(.borderedProminent)
            .padding(10)
        }
        .statusBarHidden()
    }
}
