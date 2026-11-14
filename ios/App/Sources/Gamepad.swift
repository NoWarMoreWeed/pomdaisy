import Foundation
import GameController

/// MFi / Xbox / PlayStation / Switch controllers through GameController. Positional like a Game Boy: the right face
/// button is A, the bottom one B (as the Linux host maps them); the top one opens the companion (X), the left one the
/// quick menu (Y).
final class Gamepad: ObservableObject {
    static let shared = Gamepad()
    @Published private(set) var connected = false

    private init() {
        NotificationCenter.default.addObserver(forName: .GCControllerDidConnect, object: nil, queue: .main) { [weak self] n in
            self?.attach(n.object as? GCController)
        }
        NotificationCenter.default.addObserver(forName: .GCControllerDidDisconnect, object: nil, queue: .main) { [weak self] _ in
            self?.refresh()
        }
        GCController.startWirelessControllerDiscovery {}
        GCController.controllers().forEach(attach)
        refresh()
    }

    private func refresh() {
        connected = GCController.controllers().contains { $0.extendedGamepad != nil }
        if !connected { Emulator.shared.setPadKeys(0) }
    }

    private func attach(_ controller: GCController?) {
        guard let pad = controller?.extendedGamepad else { refresh(); return }
        pad.valueChangedHandler = { pad, _ in Emulator.shared.setPadKeys(Self.keys(pad)) }
        refresh()
    }

    private static func keys(_ p: GCExtendedGamepad) -> UInt32 {
        var k: UInt32 = 0
        func on(_ pressed: Bool, _ key: Int32) { if pressed { k |= 1 << UInt32(key) } }
        let stickX = p.leftThumbstick.xAxis.value, stickY = p.leftThumbstick.yAxis.value
        on(p.dpad.up.isPressed || stickY > 0.5, Int32(PD_KEY_UP))
        on(p.dpad.down.isPressed || stickY < -0.5, Int32(PD_KEY_DOWN))
        on(p.dpad.left.isPressed || stickX < -0.5, Int32(PD_KEY_LEFT))
        on(p.dpad.right.isPressed || stickX > 0.5, Int32(PD_KEY_RIGHT))
        on(p.buttonB.isPressed, Int32(PD_KEY_A))
        on(p.buttonA.isPressed, Int32(PD_KEY_B))
        on(p.buttonY.isPressed, Int32(PD_KEY_X))
        on(p.buttonX.isPressed, Int32(PD_KEY_Y))
        on(p.leftShoulder.isPressed, Int32(PD_KEY_L))
        on(p.rightShoulder.isPressed, Int32(PD_KEY_R))
        on(p.leftTrigger.value > 0.5, Int32(PD_KEY_L2))
        on(p.rightTrigger.value > 0.5, Int32(PD_KEY_R2))
        on(p.buttonMenu.isPressed, Int32(PD_KEY_START))
        on(p.buttonOptions?.isPressed ?? false, Int32(PD_KEY_SELECT))
        on(p.leftThumbstickButton?.isPressed ?? false, Int32(PD_KEY_L3))
        on(p.rightThumbstickButton?.isPressed ?? false, Int32(PD_KEY_R3))
        return k
    }
}
