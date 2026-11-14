import Foundation

/// Where PomDaisy keeps its files: all under the app's Documents folder, which the Files app shows
/// ("On My iPhone > PomDaisy"), so ROMs can be dropped in there directly.
enum Storage {
    static let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    static let roms = documents.appendingPathComponent("roms", isDirectory: true)
    static let saves = documents.appendingPathComponent("saves", isDirectory: true)
    static let backups = saves.appendingPathComponent("backups", isDirectory: true)
    static let states = documents.appendingPathComponent("states", isDirectory: true)
    static let system = documents.appendingPathComponent("system", isDirectory: true)

    /// The Nintendo DS core's own files (its settings; see `dsSaveHint`).
    static let dsCore = system.appendingPathComponent("ds", isDirectory: true)

    static let romExtensions: Set<String> = ["gba", "gb", "gbc", "nds"]
    static func isDS(_ rom: URL) -> Bool { rom.pathExtension.lowercased() == "nds" }

    static func prepare() {
        for dir in [roms, saves, backups, states, system, dsCore] {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
    }

    static func baseName(_ rom: URL) -> String { rom.deletingPathExtension().lastPathComponent }
    static func saveFile(for rom: URL) -> URL { saves.appendingPathComponent(baseName(rom) + ".sav") }
    static func resumeState(for rom: URL) -> URL { states.appendingPathComponent(baseName(rom) + ".resume.state") }
    static func slotState(for rom: URL, slot: Int) -> URL {
        states.appendingPathComponent(baseName(rom) + ".slot\(slot).state")
    }

    /// A DS game the core has no save type for takes its save's size from `<name>.srm` in its save folder while it
    /// loads: the save is copied there for the load.
    static func dsSaveHint(for rom: URL) -> URL { dsCore.appendingPathComponent(baseName(rom) + ".srm") }

    /// Writes through a temporary file, so a crash mid-write never leaves half a save.
    static func writeAtomically(_ data: Data, to url: URL) throws {
        try data.write(to: url, options: .atomic)
    }

    /// Save files are sacred: every game start copies the save first (newest 10 distinct copies per game kept).
    static func backupSave(for rom: URL) {
        let save = saveFile(for: rom)
        guard let data = try? Data(contentsOf: save), !data.isEmpty else { return }
        let fm = FileManager.default
        let prefix = baseName(rom) + " "
        let existing = ((try? fm.contentsOfDirectory(at: backups, includingPropertiesForKeys: [.contentModificationDateKey])) ?? [])
            .filter { $0.lastPathComponent.hasPrefix(prefix) }
            .sorted { modified($0) > modified($1) }
        if let newest = existing.first, (try? Data(contentsOf: newest)) == data { return }
        let stamp = ISO8601DateFormatter.string(from: Date(), timeZone: .current,
                                                formatOptions: [.withFullDate, .withTime, .withColonSeparatorInTime])
            .replacingOccurrences(of: ":", with: "-")
        try? writeAtomically(data, to: backups.appendingPathComponent(prefix + stamp + ".sav"))
        for old in existing.dropFirst(9) { try? fm.removeItem(at: old) }
    }

    static func modified(_ url: URL) -> Date {
        (try? url.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? .distantPast
    }

    /// Copies a picked / shared file into roms/ (a ROM already there with the same bytes is kept as it is).
    @discardableResult
    static func importRom(from source: URL) throws -> URL {
        let access = source.startAccessingSecurityScopedResource()
        defer { if access { source.stopAccessingSecurityScopedResource() } }
        let data = try Data(contentsOf: source)
        var target = roms.appendingPathComponent(source.lastPathComponent)
        if FileManager.default.fileExists(atPath: target.path) {
            if (try? Data(contentsOf: target)) == data { return target }
            let base = source.deletingPathExtension().lastPathComponent, ext = source.pathExtension
            var n = 2
            repeat {
                target = roms.appendingPathComponent("\(base) (\(n)).\(ext)")
                n += 1
            } while FileManager.default.fileExists(atPath: target.path)
        }
        try writeAtomically(data, to: target)
        return target
    }
}
