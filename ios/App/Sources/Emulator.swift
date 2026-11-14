import AVFoundation
import Foundation
import QuartzCore

/// One running game: the core on its own thread, its sound, its battery save and its states.
/// Only the emulation thread touches the core (pd_*); everything else goes through `perform`.
final class Emulator: ObservableObject {
    static let shared = Emulator()

    @Published private(set) var rom: URL?
    @Published private(set) var running = false
    /// A Nintendo DS game is running (the DS core: X / Y are the game's, the companion is on L2).
    @Published private(set) var isDS = false
    /// The running DS game is on melonDS (EXPAND, no companion), not DSperate.
    @Published private(set) var dsMelon = false
    /// Which core the next DS game starts on: 0 DSperate (with the companion), 1 melonDS (with EXPAND).
    @Published var dsCore = UserDefaults.standard.integer(forKey: "dsCore") {
        didSet { UserDefaults.standard.set(dsCore, forKey: "dsCore") }
    }
    /// melonDS's EXPAND: the overworld drawn past the top screen's edges to fill the screen.
    @Published var dsExpand = UserDefaults.standard.object(forKey: "dsExpand") as? Bool ?? true {
        didSet {
            UserDefaults.standard.set(dsExpand, forKey: "dsExpand")
            pd_set_ds_expand(dsExpand)
        }
    }
    @Published var paused = false {
        didSet { if paused { flushSave() } }
    }
    @Published var fastForward = false {
        didSet { pd_set_fast_forward(fastForward, Int32(fastForwardSpeed)) }
    }
    @Published var fastForwardSpeed = 3 {
        didSet { pd_set_fast_forward(fastForward, Int32(fastForwardSpeed)) }
    }
    @Published var message: String?

    private var thread: Thread?
    private let jobsLock = NSLock()
    private var jobs: [() -> Void] = []
    private var stopRequested = false
    private var lastSave = Data()
    private var frameCount = 0
    private let audio = AudioOutput()

    // ---- input, from any thread ----
    private let inputLock = NSLock()
    private var touchKeys: UInt32 = 0
    private var padKeys: UInt32 = 0

    func setTouchKeys(_ k: UInt32) { inputLock.lock(); touchKeys = k; pushKeys(); inputLock.unlock() }
    func setPadKeys(_ k: UInt32) { inputLock.lock(); padKeys = k; pushKeys(); inputLock.unlock() }
    private func pushKeys() { pd_set_keys(touchKeys | padKeys) }

    // ---- the game ----
    func start(_ url: URL) {
        stop()
        // A DS game (up to 512 MB) is read by its core straight from the file, never copied into memory.
        let ds = Storage.isDS(url)
        var data = Data()
        if ds {
            guard FileManager.default.isReadableFile(atPath: url.path) else {
                message = "Die Datei ließ sich nicht lesen."
                return
            }
        } else {
            guard let bytes = try? Data(contentsOf: url) else {
                message = "Die Datei ließ sich nicht lesen."
                return
            }
            data = bytes
        }
        Storage.backupSave(for: url)
        stopRequested = false
        rom = url
        isDS = ds
        dsMelon = ds && dsCore == 1
        Storage.melonStates = dsMelon
        pd_set_ds_core(Int32(dsMelon ? PD_DS_CORE_MELONDS : PD_DS_CORE_DSPERATE))
        pd_set_ds_expand(dsExpand)
        running = true
        paused = false
        fastForward = false
        let t = Thread { [weak self] in self?.loop(url: url, data: data) }
        t.name = "pomdaisy-emu"
        t.qualityOfService = .userInteractive
        thread = t
        t.start()
        audio.start(sampleRate: 32768)
    }

    /// Ends the game: the battery save and the resume state are written first.
    func stop() {
        guard running else { return }
        let done = DispatchSemaphore(value: 0)
        perform { [weak self] in
            self?.writeSaveIfChanged()
            self?.writeResumeState()
            done.signal()
        }
        _ = done.wait(timeout: .now() + 2)
        stopRequested = true
        while thread?.isExecuting == true { usleep(2000) }
        thread = nil
        audio.stop()
        running = false
        rom = nil
    }

    /// The app goes to the background: iOS may end it there without warning, so save everything now.
    func suspend() {
        guard running else { return }
        let done = DispatchSemaphore(value: 0)
        perform { [weak self] in
            self?.writeSaveIfChanged()
            self?.writeResumeState()
            done.signal()
        }
        _ = done.wait(timeout: .now() + 2)
        paused = true
    }

    func flushSave() {
        perform { [weak self] in self?.writeSaveIfChanged() }
    }

    /// Starts the running game again on the other DS core (`dsCore`): saved and stopped first, like leaving it.
    func restartOnChosenCore() {
        guard let rom else { return }
        stop()
        start(rom)
    }

    func reset() {
        perform { pd_reset() }
        paused = false
    }

    func saveState(slot: Int) {
        guard let rom else { return }
        perform { [weak self] in
            let ok = Self.writeState(to: Storage.slotState(for: rom, slot: slot))
            DispatchQueue.main.async { self?.message = ok ? "Gespeichert (Platz \(slot))" : "Speichern fehlgeschlagen" }
        }
    }

    func loadState(slot: Int) {
        guard let rom else { return }
        perform { [weak self] in
            let ok = Self.readState(from: Storage.slotState(for: rom, slot: slot))
            DispatchQueue.main.async { self?.message = ok ? "Geladen (Platz \(slot))" : "Kein Spielstand in Platz \(slot)" }
        }
        paused = false
    }

    func hasState(slot: Int) -> Bool {
        guard let rom else { return false }
        return FileManager.default.fileExists(atPath: Storage.slotState(for: rom, slot: slot).path)
    }

    /// How busy the core is: its time for a frame against the time a frame has (1 = just fast enough).
    var load: Double { pd_frame_ms() * pd_fps() / 1000 }

    func setPanel(width: Int, height: Int) { pd_set_panel(Int32(width), Int32(height)) }

    func perform(_ job: @escaping () -> Void) {
        jobsLock.lock()
        jobs.append(job)
        jobsLock.unlock()
    }

    // ---- the emulation thread ----
    private func runJobs() {
        jobsLock.lock()
        let now = jobs
        jobs.removeAll()
        jobsLock.unlock()
        now.forEach { $0() }
    }

    private func loop(url: URL, data: Data) {
        Storage.prepare()
        romForThread = url
        let ds = Storage.isDS(url)
        let ok: Bool
        if ds && Storage.melonStates {
            // melonDS reads and writes the battery save itself: saves/<game>.sav, the same file DSperate uses.
            pd_set_dirs(Storage.system.path, Storage.saves.path)
            ok = pd_load(url.path, nil, 0)
        } else if ds {
            let hint = Storage.dsSaveHint(for: url)
            try? FileManager.default.removeItem(at: hint)
            try? FileManager.default.copyItem(at: Storage.saveFile(for: url), to: hint)
            pd_set_dirs(Storage.system.path, Storage.dsCore.path)
            ok = pd_load(url.path, nil, 0)
            try? FileManager.default.removeItem(at: hint)
        } else {
            pd_set_dirs(Storage.system.path, Storage.saves.path)
            ok = data.withUnsafeBytes { pd_load(url.path, $0.baseAddress, data.count) }
        }
        guard ok else {
            DispatchQueue.main.async {
                self.message = "Dieses Spiel ließ sich nicht starten."
                self.running = false
                self.rom = nil
            }
            return
        }
        if let save = try? Data(contentsOf: Storage.saveFile(for: url)) {
            save.withUnsafeBytes { pd_save_load($0.baseAddress, save.count) }
        }
        lastSave = currentSaveImage()
        // A state never carries the save (both cores load them without it), so resuming is safe.
        _ = Self.readState(from: Storage.resumeState(for: url))

        var next = CACurrentMediaTime()
        while !stopRequested {
            runJobs()
            if paused {
                usleep(10_000)
                next = CACurrentMediaTime()
                continue
            }
            let fps = pd_fps()
            let frame = 1.0 / fps
            if fastForward {
                for _ in 0..<max(1, fastForwardSpeed) { pd_run() }
            } else {
                pd_run()
            }
            frameCount += 1
            if frameCount % 60 == 0 { writeSaveIfChanged() }

            // Pacing: the frame time, nudged by how much sound is queued (keeps the sound from running dry or late).
            var step = frame
            if !fastForward {
                let target = pd_sample_rate() * 0.08
                let queued = Double(pd_audio_queued())
                let d = max(-1, min(1, (queued - target) / target))
                step *= 1 + 0.01 * d
            }
            next += step
            let now = CACurrentMediaTime()
            if next > now {
                usleep(useconds_t((next - now) * 1_000_000))
            } else if now - next > 0.25 {
                next = now // far behind (a hiccup): don't race to catch up
            }
        }
        runJobs()
        pd_unload()
    }

    private func currentSaveImage() -> Data {
        let n = pd_save_image(nil)
        guard n > 0 else { return Data() }
        var d = Data(count: n)
        d.withUnsafeMutableBytes { _ = pd_save_image($0.baseAddress) }
        return d
    }

    private func writeSaveIfChanged() {
        guard let rom = currentRom() else { return }
        let now = currentSaveImage()
        guard !now.isEmpty, now != lastSave else { return }
        do {
            try Storage.writeAtomically(now, to: Storage.saveFile(for: rom))
            lastSave = now
        } catch {
            NSLog("PomDaisy: save failed: \(error)")
        }
    }

    private func writeResumeState() {
        guard let rom = currentRom() else { return }
        _ = Self.writeState(to: Storage.resumeState(for: rom))
    }

    /// The game the emulation thread runs (set when it starts; never read through the main thread, which may be
    /// waiting on this thread in stop() / suspend()).
    private var romForThread: URL?
    private func currentRom() -> URL? { romForThread }

    private static func writeState(to url: URL) -> Bool {
        let n = pd_state_size()
        guard n > 0 else { return false }
        var d = Data(count: n)
        let ok = d.withUnsafeMutableBytes { pd_state_save($0.baseAddress, n) }
        guard ok else { return false }
        return (try? Storage.writeAtomically(d, to: url)) != nil
    }

    private static func readState(from url: URL) -> Bool {
        guard let d = try? Data(contentsOf: url), !d.isEmpty else { return false }
        return d.withUnsafeBytes { pd_state_load($0.baseAddress, d.count) }
    }
}

/// The core's sound through AVAudioEngine: a source node pulls stereo samples from the bridge's ring.
final class AudioOutput {
    private var engine: AVAudioEngine?
    private var scratch = [Int16](repeating: 0, count: 8192 * 2)

    func start(sampleRate: Double) {
        stop()
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.ambient, mode: .default, options: [])
        try? session.setPreferredIOBufferDuration(0.01)
        try? session.setActive(true)

        let engine = AVAudioEngine()
        guard let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 2) else { return }
        let node = AVAudioSourceNode(format: format) { [weak self] _, _, frameCount, bufferList -> OSStatus in
            guard let self else { return noErr }
            let frames = Int(frameCount)
            let abl = UnsafeMutableAudioBufferListPointer(bufferList)
            guard abl.count >= 2,
                  let left = abl[0].mData?.assumingMemoryBound(to: Float.self),
                  let right = abl[1].mData?.assumingMemoryBound(to: Float.self) else { return noErr }
            let want = min(frames, self.scratch.count / 2)
            let got = self.scratch.withUnsafeMutableBufferPointer { pd_audio_read($0.baseAddress, want) }
            for i in 0..<frames {
                if i < got {
                    left[i] = Float(self.scratch[i * 2]) / 32768
                    right[i] = Float(self.scratch[i * 2 + 1]) / 32768
                } else {
                    left[i] = 0
                    right[i] = 0
                }
            }
            return noErr
        }
        engine.attach(node)
        engine.connect(node, to: engine.mainMixerNode, format: format)
        do {
            try engine.start()
            self.engine = engine
        } catch {
            NSLog("PomDaisy: audio failed: \(error)")
        }
    }

    func stop() {
        engine?.stop()
        engine = nil
    }
}
