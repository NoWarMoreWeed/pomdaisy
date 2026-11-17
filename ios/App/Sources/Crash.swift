import Foundation
import SwiftUI
import UIKit

/// PomDaisy's own crash reports. At launch: the last run's crash file, if it holds a crash, becomes a report in
/// Documents/crashlogs (with that run's log), which the app offers to share; then this run's catchers start: fatal
/// signals (pd_crash.c), Objective-C exceptions here, uncaught Kotlin exceptions in the Android view (Main.kt writes to
/// the same file). The app's console goes to Documents/logs/run.log, the run before's to previous.log.
enum Crash {
    static let dir = Storage.documents.appendingPathComponent("crashlogs", isDirectory: true)
    static let logs = Storage.documents.appendingPathComponent("logs", isDirectory: true)
    /// Written while the app runs; holds a crash only if the run ended in one.
    static let pending = dir.appendingPathComponent("pending.txt")
    /// The report found at this launch (the last run crashed).
    static private(set) var lastReport: URL?

    static var buildInfo: String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "?"
        var sys = utsname()
        uname(&sys)
        let model = withUnsafeBytes(of: &sys.machine) { String(decoding: $0.prefix { $0 != 0 }, as: UTF8.self) }
        return "PomDaisy \(v) build \(b), \(model), iOS \(UIDevice.current.systemVersion)"
    }

    static func start() {
        let fm = FileManager.default
        try? fm.createDirectory(at: dir, withIntermediateDirectories: true)
        try? fm.createDirectory(at: logs, withIntermediateDirectories: true)
        let run = logs.appendingPathComponent("run.log")
        let previous = logs.appendingPathComponent("previous.log")
        try? fm.removeItem(at: previous)
        try? fm.moveItem(at: run, to: previous)

        if let text = try? String(contentsOf: pending, encoding: .utf8), text.contains("=== ") {
            let stamp = ISO8601DateFormatter().string(from: Date()).replacingOccurrences(of: ":", with: "-")
            let report = dir.appendingPathComponent("crash-\(stamp).txt")
            let log = (try? String(contentsOf: previous, encoding: .utf8)) ?? ""
            let tail = log.split(separator: "\n", omittingEmptySubsequences: false).suffix(300).joined(separator: "\n")
            try? (text + "\n\n=== the run's last log lines\n" + tail).write(to: report, atomically: true, encoding: .utf8)
            lastReport = report
        }
        try? "\(buildInfo), started \(Date())\n".write(to: pending, atomically: true, encoding: .utf8)

        // The console into run.log, except under CI's simulator test (it reads the console itself).
        if ProcessInfo.processInfo.environment["PD_ANDROID"] == nil {
            freopen(run.path, "a", stdout)
            freopen(run.path, "a", stderr)
            setvbuf(stdout, nil, _IOLBF, 0)
        }
        pd_crash_install(pending.path)
        setenv("PD_CRASH_FILE", pending.path, 1)
        NSSetUncaughtExceptionHandler { e in
            pd_crash_note("\n=== Objective-C exception: \(e.name.rawValue): \(e.reason ?? "")\n\(e.callStackSymbols.joined(separator: "\n"))\n")
        }
    }
}

/// Shown once after a crash: what happened, and the report to share (Files, Mail, Messages, AirDrop ...).
struct CrashNotice: ViewModifier {
    @State private var report = Crash.lastReport

    func body(content: Content) -> some View {
        content.sheet(isPresented: Binding(get: { report != nil }, set: { if !$0 { report = nil } })) {
            if let report {
                VStack(spacing: 16) {
                    Text("PomDaisy ist abgestürzt").font(.title2.bold())
                    Text("Ein Bericht wurde gespeichert (Dateien > Auf meinem iPhone > PomDaisy > crashlogs). Teile ihn, damit der Fehler behoben werden kann. Er enthält Gerät, iOS-Version, Build und das Protokoll, keine Spiele und keine Spielstände.")
                        .font(.callout).multilineTextAlignment(.center)
                    ShareLink(item: report) { Label("Bericht teilen", systemImage: "square.and.arrow.up") }
                        .buttonStyle(.borderedProminent)
                    Button("Schließen") { self.report = nil }
                }
                .padding(24)
                .presentationDetents([.medium])
            }
        }
    }
}
