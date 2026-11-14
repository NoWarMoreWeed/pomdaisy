import SwiftUI
import UniformTypeIdentifiers

@main
struct PomDaisyApp: App {
    @StateObject private var emulator = Emulator.shared
    @StateObject private var gamepad = Gamepad.shared
    @StateObject private var library = Library()
    @Environment(\.scenePhase) private var phase
    @State private var pausedByBackground = false

    init() { Storage.prepare() }

    var body: some Scene {
        WindowGroup {
            Group {
                if emulator.running {
                    GameView()
                } else {
                    LibraryView()
                }
            }
            .environmentObject(emulator)
            .environmentObject(gamepad)
            .environmentObject(library)
            .preferredColorScheme(.dark)
            .onOpenURL { url in
                if let rom = try? Storage.importRom(from: url) {
                    library.refresh()
                    emulator.start(rom)
                }
            }
        }
        .onChange(of: phase) { now in
            switch now {
            case .background:
                if emulator.running && !emulator.paused {
                    pausedByBackground = true
                    emulator.suspend()
                }
            case .active:
                if pausedByBackground {
                    pausedByBackground = false
                    emulator.paused = false
                }
                library.refresh()
            default:
                break
            }
        }
    }
}

// ---- the library ----

final class Library: ObservableObject {
    @Published private(set) var roms: [URL] = []

    init() { refresh() }

    func refresh() {
        let files = (try? FileManager.default.contentsOfDirectory(at: Storage.roms, includingPropertiesForKeys: nil)) ?? []
        roms = files.filter { Storage.romExtensions.contains($0.pathExtension.lowercased()) }
            .sorted { Self.title($0).localizedCaseInsensitiveCompare(Self.title($1)) == .orderedAscending }
    }

    func delete(_ rom: URL) {
        try? FileManager.default.removeItem(at: rom) // its saves and states stay
        refresh()
    }

    /// The file name without its dump tags: "Pokemon - Emerald Version (USA, Europe).gba" -> "Pokemon - Emerald Version".
    static func title(_ rom: URL) -> String {
        var name = rom.deletingPathExtension().lastPathComponent
        while let r = name.range(of: #"\s*[\(\[][^\)\]]*[\)\]]\s*$"#, options: .regularExpression) {
            name.removeSubrange(r)
        }
        return name.isEmpty ? rom.lastPathComponent : name
    }

    static func system(_ rom: URL) -> String {
        switch rom.pathExtension.lowercased() {
        case "gba": return "Game Boy Advance"
        case "gbc": return "Game Boy Color"
        case "nds": return "Nintendo DS"
        default: return "Game Boy"
        }
    }
}

struct LibraryView: View {
    @EnvironmentObject var library: Library
    @EnvironmentObject var emulator: Emulator
    @State private var importing = false

    private static var romTypes: [UTType] {
        ["gba", "gb", "gbc", "nds"].compactMap { UTType(filenameExtension: $0) } + [.data]
    }

    var body: some View {
        NavigationStack {
            Group {
                if library.roms.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "gamecontroller").font(.system(size: 48))
                        Text("Noch keine Spiele").font(.title2.bold())
                        Text("Tippe auf + und wähle deine ROM-Dateien (.gba, .gb, .gbc, .nds). Du kannst sie auch in der Dateien-App unter „Auf meinem iPhone > PomDaisy > roms“ ablegen.")
                            .multilineTextAlignment(.center)
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 32)
                        Button("Spiele hinzufügen") { importing = true }
                            .buttonStyle(.borderedProminent)
                    }
                } else {
                    List {
                        ForEach(library.roms, id: \.self) { rom in
                            Button {
                                emulator.start(rom)
                            } label: {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(Library.title(rom)).font(.headline).foregroundStyle(.primary)
                                    Text(Library.system(rom)).font(.caption).foregroundStyle(.secondary)
                                }
                                .padding(.vertical, 4)
                            }
                            .swipeActions {
                                Button(role: .destructive) { library.delete(rom) } label: {
                                    Label("Löschen", systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("PomDaisy")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { importing = true } label: { Image(systemName: "plus") }
                }
            }
            .fileImporter(isPresented: $importing, allowedContentTypes: Self.romTypes, allowsMultipleSelection: true) { result in
                if case .success(let urls) = result {
                    for url in urls where Storage.romExtensions.contains(url.pathExtension.lowercased()) {
                        try? Storage.importRom(from: url)
                    }
                    library.refresh()
                }
            }
            .refreshable { library.refresh() }
            .alert(emulator.message ?? "", isPresented: Binding(
                get: { emulator.message != nil && !emulator.running },
                set: { if !$0 { emulator.message = nil } })) {
                Button("OK", role: .cancel) {}
            }
        }
    }
}

// ---- the game ----

struct GameView: View {
    @EnvironmentObject var emulator: Emulator
    @EnvironmentObject var gamepad: Gamepad
    @State private var menu = false

    var body: some View {
        GeometryReader { geo in
            let portrait = geo.size.height > geo.size.width
            let gameH = portrait ? min(geo.size.width * 0.9, geo.size.height * 0.52) : geo.size.height
            // One structure for both orientations (upright, a VStack of game + controls kept touches from the game):
            // the game, then the controls over everything, which pass through every touch that isn't theirs.
            ZStack(alignment: .top) {
                Color.black.ignoresSafeArea()
                if portrait {
                    GameScreen()
                        .frame(width: geo.size.width, height: gameH)
                        .frame(maxHeight: .infinity, alignment: .top)
                } else {
                    GameScreen().ignoresSafeArea()
                }
                if !gamepad.connected {
                    TouchControls(style: portrait ? .below : .overlay,
                                  reservedTop: portrait ? geo.safeAreaInsets.top + gameH : 0,
                                  ds: emulator.isDS)
                        .ignoresSafeArea()
                }
            }
            .overlay(alignment: portrait ? .topTrailing : .top) {
                menuButton.padding(.top, 6).padding(.trailing, portrait ? 10 : 0)
            }
            .overlay(alignment: .top) {
                if let m = emulator.message {
                    Text(m)
                        .font(.callout.bold())
                        .padding(.horizontal, 14).padding(.vertical, 8)
                        .background(.ultraThinMaterial, in: Capsule())
                        .padding(.top, 12)
                        .task {
                            try? await Task.sleep(for: .seconds(2))
                            emulator.message = nil
                        }
                }
            }
        }
        .statusBarHidden()
        .persistentSystemOverlays(.hidden)
        .sheet(isPresented: $menu, onDismiss: { emulator.paused = false }) {
            GameMenu(close: { menu = false })
                .presentationDetents([.medium, .large])
        }
    }

    private var menuButton: some View {
        HStack(spacing: 10) {
            Button {
                emulator.paused = true
                menu = true
            } label: {
                Image(systemName: "pause.circle.fill").font(.system(size: 28))
            }
            Button {
                emulator.fastForward.toggle()
            } label: {
                Image(systemName: emulator.fastForward ? "forward.fill" : "forward").font(.system(size: 22))
            }
        }
        .foregroundStyle(.white.opacity(0.6))
        .fixedSize()
    }
}

struct GameMenu: View {
    @EnvironmentObject var emulator: Emulator
    let close: () -> Void

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Button { close() } label: { Label("Weiterspielen", systemImage: "play.fill") }
                }
                Section("Spielstände") {
                    ForEach(1...3, id: \.self) { slot in
                        HStack {
                            Text("Platz \(slot)")
                            Spacer()
                            Button("Speichern") { emulator.saveState(slot: slot); close() }
                                .buttonStyle(.bordered)
                            Button("Laden") { emulator.loadState(slot: slot); close() }
                                .buttonStyle(.bordered)
                                .disabled(!emulator.hasState(slot: slot))
                        }
                    }
                }
                Section("Vorspulen") {
                    Toggle("Vorspulen an", isOn: $emulator.fastForward)
                    Picker("Tempo", selection: $emulator.fastForwardSpeed) {
                        ForEach([2, 3, 4, 6, 8], id: \.self) { Text("\($0)x").tag($0) }
                    }
                }
                Section {
                    // The core's time for a frame against the time a frame has: over 100 % the game runs slow.
                    LabeledContent("Auslastung", value: "\(Int((emulator.load * 100).rounded())) %")
                } header: {
                    Text("Leistung")
                } footer: {
                    Text(emulator.isDS
                        ? "Nintendo DS läuft ohne JIT. Bleibt der Wert unter 100 %, läuft das Spiel in voller Geschwindigkeit."
                        : "Unter 100 % läuft das Spiel in voller Geschwindigkeit.")
                }
                Section {
                    Button { emulator.reset(); close() } label: {
                        Label("Neu starten", systemImage: "arrow.counterclockwise")
                    }
                    Button(role: .destructive) { close(); emulator.stop() } label: {
                        Label("Spiel beenden", systemImage: "xmark.circle")
                    }
                } footer: {
                    Text("Dein Spielstand im Spiel wird automatisch gesichert. Beim nächsten Start geht es dort weiter, wo du aufgehört hast.")
                }
            }
            .navigationTitle(emulator.rom.map(Library.title) ?? "Pause")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
