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
    @AppStorage("liveStats") private var liveStats = true
    /// melonDS with EXPAND: the touch screen floating in (TOUCH), and whether the game asks for it (its field read).
    @State private var touchShown = false
    @State private var touchWanted = false
    private let touchPoll = Timer.publish(every: 0.2, on: .main, in: .common).autoconnect()

    private var expanded: Bool { emulator.isDS && emulator.dsMelon && emulator.dsExpand }

    var body: some View {
        GeometryReader { geo in
            let portrait = geo.size.height > geo.size.width
            let gameH = portrait ? min(geo.size.width * 0.9, geo.size.height * 0.52) : geo.size.height
            let insets = geo.safeAreaInsets
            let full = CGSize(width: geo.size.width + insets.leading + insets.trailing,
                              height: geo.size.height + insets.top + insets.bottom)
            let ds = DSLayout(full: full, insets: insets, portrait: portrait)
            let wide = DSExpandLayout(full: full, insets: insets, portrait: portrait)
            // One structure for both orientations (upright, a VStack of game + controls kept touches from the game):
            // the game, then the controls over everything, which pass through every touch that isn't theirs.
            ZStack(alignment: .top) {
                Color.black.ignoresSafeArea()
                if expanded {
                    // EXPAND: the top screen as large as the screen allows; the touch screen floats in over it.
                    ZStack {
                        Color.clear
                        GameScreen()
                            .frame(width: wide.top.width, height: wide.top.height)
                            .position(x: wide.top.midX, y: wide.top.midY)
                        if touchShown || touchWanted {
                            Color.black.opacity(0.35)
                                .contentShape(Rectangle())
                                .onTapGesture { touchShown = false }
                            GameScreen(kind: .dsTouch)
                                .frame(width: wide.touch.width, height: wide.touch.height)
                                .overlay(Rectangle().stroke(.white.opacity(0.8), lineWidth: 2))
                                .position(x: wide.touch.midX, y: wide.touch.midY)
                        }
                    }
                    .ignoresSafeArea()
                } else if emulator.isDS {
                    // Both DS screens, placed in the whole screen's coordinates.
                    ZStack {
                        Color.clear
                        GameScreen()
                            .frame(width: ds.top.width, height: ds.top.height)
                            .position(x: ds.top.midX, y: ds.top.midY)
                        GameScreen(kind: .dsTouch)
                            .frame(width: ds.touch.width, height: ds.touch.height)
                            .position(x: ds.touch.midX, y: ds.touch.midY)
                    }
                    .ignoresSafeArea()
                } else if portrait {
                    GameScreen()
                        .frame(width: geo.size.width, height: gameH)
                        .frame(maxHeight: .infinity, alignment: .top)
                } else {
                    GameScreen().ignoresSafeArea()
                }
                if !gamepad.connected {
                    if expanded {
                        TouchControls(style: portrait ? .dsBelow : .dsOverlay, panel: wide.controls, melon: true,
                                      onSpecial: { touchShown.toggle() })
                            .ignoresSafeArea()
                    } else if emulator.isDS {
                        TouchControls(style: portrait ? .dsBelow : .dsSide, panel: ds.controls, melon: emulator.dsMelon)
                            .ignoresSafeArea()
                    } else {
                        TouchControls(style: portrait ? .below : .overlay,
                                      reservedTop: portrait ? geo.safeAreaInsets.top + gameH : 0)
                            .ignoresSafeArea()
                    }
                }
            }
            .overlay(alignment: portrait ? .topTrailing : .top) {
                menuButton.padding(.top, 6).padding(.trailing, portrait ? 10 : 0)
            }
            // Upright: the game's top left corner. Sideways L sits there, so the bottom middle (between SELECT and START).
            // Sideways with EXPAND the bottom middle holds SELECT / TOUCH / START: the bottom left corner then.
            .overlay(alignment: portrait ? .topLeading : expanded ? .bottomLeading : .bottom) {
                if liveStats {
                    LiveStats()
                        .padding(.leading, portrait ? 8 : expanded ? max(insets.leading * 0.5, 8) + 4 : 0)
                        .padding(portrait ? .top : .bottom, 6)
                        .allowsHitTesting(false)
                }
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
        .onReceive(touchPoll) { _ in
            // PK_NDS_FIELD_TOUCH (2048): the game waits on its touch screen (a cart PokeDaisy has field reads for).
            let want = expanded && (pd_ds_field_bits() & 2048) != 0
            if want != touchWanted { touchWanted = want }
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

/// The live reading in a corner of the game: how busy the core is (its time for a frame against the time a frame has;
/// the average, then the slowest frame of the last half second) and the frames per second it really ran.
struct LiveStats: View {
    @State private var text = "…"
    @State private var lastFrames: UInt64 = 0
    @State private var lastTime = Date()
    private let timer = Timer.publish(every: 0.5, on: .main, in: .common).autoconnect()

    var body: some View {
        Text(text)
            .font(.system(size: 11, weight: .semibold, design: .monospaced))
            .foregroundStyle(.white)
            .padding(.horizontal, 6).padding(.vertical, 3)
            .background(.black.opacity(0.55), in: RoundedRectangle(cornerRadius: 5))
            .onReceive(timer) { _ in update() }
    }

    private func update() {
        let now = Date(), frames = pd_frames_run()
        let seconds = now.timeIntervalSince(lastTime)
        let ran = frames >= lastFrames ? frames - lastFrames : frames // a new game starts from 0
        let frame = 1000 / pd_fps() // the time a frame has, in ms
        let average = pd_frame_ms() / frame * 100, peak = pd_frame_ms_peak() / frame * 100
        lastFrames = frames
        lastTime = now
        guard seconds > 0.1 else { return }
        if ran == 0 {
            text = "Pause"
        } else {
            text = String(format: "Ø %.0f %%  max %.0f %%  %.0f fps", average, peak, Double(ran) / seconds)
        }
    }
}

struct GameMenu: View {
    @EnvironmentObject var emulator: Emulator
    @AppStorage("liveStats") private var liveStats = true
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
                if emulator.isDS {
                    Section {
                        Picker("Kern", selection: $emulator.dsCore) {
                            Text("DSperate (Begleiter)").tag(0)
                            Text("melonDS (EXPAND)").tag(1)
                        }
                        if emulator.dsCore != (emulator.dsMelon ? 1 : 0) {
                            Button {
                                close()
                                emulator.restartOnChosenCore()
                            } label: {
                                Label("Mit diesem Kern neu starten", systemImage: "arrow.triangle.2.circlepath")
                            }
                        }
                        Toggle("EXPAND (Breitbild)", isOn: $emulator.dsExpand)
                            .disabled(!emulator.dsMelon)
                    } header: {
                        Text("Nintendo DS")
                    } footer: {
                        Text("melonDS zeigt mit EXPAND mehr von der Spielwelt, hat aber noch keinen Begleiter. EXPAND braucht ein Spiel, das PokeDaisy kennt (z. B. Platin USA, HeartGold / SoulSilver, Schwarz / Weiß); sonst bleibt das Bild 4:3. Der Spielstand im Spiel gilt für beide Kerne, Speicherplätze sind je Kern getrennt.")
                    }
                }
                Section {
                    // The core's time for a frame against the time a frame has: over 100 % the game runs slow.
                    LabeledContent("Auslastung", value: "\(Int((emulator.load * 100).rounded())) %")
                    Toggle("Live-Anzeige im Spiel", isOn: $liveStats)
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
