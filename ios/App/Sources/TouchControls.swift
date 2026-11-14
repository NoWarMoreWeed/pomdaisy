import SwiftUI
import UIKit

/// The on-screen buttons. One UIView takes every finger itself (several at once, sliding from button to button), so
/// D-pad + A + B can be held together. Touches that hit no button fall through to the game below (overlay mode).
final class TouchControlsView: UIView {
    enum Style { case below, overlay }

    struct Button {
        let label: String
        let key: Int32          // PD_KEY_*
        var rect: CGRect = .zero
        let round: Bool
        let small: Bool
    }

    var style: Style = .below { didSet { setNeedsLayout(); setNeedsDisplay() } }

    private var buttons: [Button] = [
        Button(label: "A", key: Int32(PD_KEY_A), round: true, small: false),
        Button(label: "B", key: Int32(PD_KEY_B), round: true, small: false),
        Button(label: "L", key: Int32(PD_KEY_L), round: false, small: false),
        Button(label: "R", key: Int32(PD_KEY_R), round: false, small: false),
        Button(label: "SELECT", key: Int32(PD_KEY_SELECT), round: false, small: true),
        Button(label: "START", key: Int32(PD_KEY_START), round: false, small: true),
        // The core's own buttons: X opens the companion, Y the quick menu.
        Button(label: "BEGLEITER", key: Int32(PD_KEY_X), round: false, small: true),
        Button(label: "MENÜ", key: Int32(PD_KEY_Y), round: false, small: true),
    ]
    private var dpad = CGRect.zero
    private var pressed: UInt32 = 0
    private var fingers: [ObjectIdentifier: UInt32] = [:]
    private let haptic = UIImpactFeedbackGenerator(style: .light)

    override init(frame: CGRect) {
        super.init(frame: frame)
        isMultipleTouchEnabled = true
        isOpaque = false
        backgroundColor = .clear
        contentMode = .redraw
    }

    required init?(coder: NSCoder) { fatalError() }

    // ---- layout ----
    override func layoutSubviews() {
        super.layoutSubviews()
        let b = bounds.inset(by: safeAreaInsets)
        let unit = min(b.width, b.height) * (style == .below ? 0.16 : 0.2)
        let pad = unit * 2.6
        func set(_ key: Int32, _ r: CGRect) {
            if let i = buttons.firstIndex(where: { $0.key == key }) { buttons[i].rect = r }
        }
        let face = unit * 1.1, shoulderW = unit * 1.6, shoulderH = unit * 0.55
        let smallW = unit * 1.45, smallH = unit * 0.45
        switch style {
        case .below:
            let midY = b.minY + b.height * 0.48
            dpad = CGRect(x: b.minX + unit * 0.4, y: midY - pad / 2, width: pad, height: pad)
            let ax = b.maxX - unit * 0.4 - face, ay = midY - face * 0.95
            set(Int32(PD_KEY_A), CGRect(x: ax, y: ay, width: face, height: face))
            set(Int32(PD_KEY_B), CGRect(x: ax - face * 1.25, y: ay + face * 0.8, width: face, height: face))
            set(Int32(PD_KEY_L), CGRect(x: b.minX + unit * 0.3, y: b.minY + unit * 0.15, width: shoulderW, height: shoulderH))
            set(Int32(PD_KEY_R), CGRect(x: b.maxX - unit * 0.3 - shoulderW, y: b.minY + unit * 0.15, width: shoulderW, height: shoulderH))
            let by = b.maxY - smallH - unit * 0.35
            set(Int32(PD_KEY_SELECT), CGRect(x: b.midX - smallW - unit * 0.15, y: by, width: smallW, height: smallH))
            set(Int32(PD_KEY_START), CGRect(x: b.midX + unit * 0.15, y: by, width: smallW, height: smallH))
            let ty = b.minY + unit * 0.2
            set(Int32(PD_KEY_X), CGRect(x: b.midX - smallW - unit * 0.15, y: ty, width: smallW, height: smallH))
            set(Int32(PD_KEY_Y), CGRect(x: b.midX + unit * 0.15, y: ty, width: smallW, height: smallH))
        case .overlay:
            let midY = b.minY + b.height * 0.58
            dpad = CGRect(x: b.minX + unit * 0.3, y: midY - pad / 2, width: pad, height: pad)
            let ax = b.maxX - unit * 0.3 - face, ay = midY - face * 0.95
            set(Int32(PD_KEY_A), CGRect(x: ax, y: ay, width: face, height: face))
            set(Int32(PD_KEY_B), CGRect(x: ax - face * 1.25, y: ay + face * 0.8, width: face, height: face))
            set(Int32(PD_KEY_L), CGRect(x: b.minX + unit * 0.2, y: b.minY + unit * 0.2, width: shoulderW, height: shoulderH))
            set(Int32(PD_KEY_R), CGRect(x: b.maxX - unit * 0.2 - shoulderW, y: b.minY + unit * 0.2, width: shoulderW, height: shoulderH))
            let by = b.maxY - smallH - unit * 0.15
            set(Int32(PD_KEY_SELECT), CGRect(x: b.minX + unit * 0.3, y: by, width: smallW, height: smallH))
            set(Int32(PD_KEY_START), CGRect(x: b.maxX - unit * 0.3 - smallW, y: by, width: smallW, height: smallH))
            set(Int32(PD_KEY_X), CGRect(x: b.minX + unit * 0.3, y: b.minY + unit * 0.95, width: smallW, height: smallH))
            set(Int32(PD_KEY_Y), CGRect(x: b.maxX - unit * 0.3 - smallW, y: b.minY + unit * 0.95, width: smallW, height: smallH))
        }
        setNeedsDisplay()
    }

    // ---- drawing ----
    override func draw(_ rect: CGRect) {
        let alpha: CGFloat = style == .overlay ? 0.35 : 0.85
        let fill = UIColor(white: 1, alpha: alpha * 0.25), stroke = UIColor(white: 1, alpha: alpha)
        let down = UIColor(white: 1, alpha: alpha * 0.7)
        // D-pad: a cross
        let d = dpad, t = d.width / 3
        let cross = UIBezierPath()
        cross.append(UIBezierPath(roundedRect: CGRect(x: d.minX + t, y: d.minY, width: t, height: d.height), cornerRadius: t * 0.2))
        cross.append(UIBezierPath(roundedRect: CGRect(x: d.minX, y: d.minY + t, width: d.width, height: t), cornerRadius: t * 0.2))
        fill.setFill(); cross.fill()
        stroke.setStroke(); cross.lineWidth = 2; cross.stroke()
        let arms: [(Int32, CGRect)] = [
            (Int32(PD_KEY_UP), CGRect(x: d.minX + t, y: d.minY, width: t, height: t)),
            (Int32(PD_KEY_DOWN), CGRect(x: d.minX + t, y: d.maxY - t, width: t, height: t)),
            (Int32(PD_KEY_LEFT), CGRect(x: d.minX, y: d.minY + t, width: t, height: t)),
            (Int32(PD_KEY_RIGHT), CGRect(x: d.maxX - t, y: d.minY + t, width: t, height: t)),
        ]
        for (key, r) in arms where pressed & (1 << UInt32(key)) != 0 {
            down.setFill(); UIBezierPath(roundedRect: r, cornerRadius: t * 0.2).fill()
        }
        for b in buttons {
            let path = b.round ? UIBezierPath(ovalIn: b.rect) : UIBezierPath(roundedRect: b.rect, cornerRadius: b.rect.height / 2)
            (pressed & (1 << UInt32(b.key)) != 0 ? down : fill).setFill()
            path.fill()
            stroke.setStroke(); path.lineWidth = 2; path.stroke()
            let size = b.small ? b.rect.height * 0.42 : b.rect.height * 0.45
            let attrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: size, weight: .bold),
                .foregroundColor: stroke,
            ]
            let s = NSString(string: b.label)
            let sz = s.size(withAttributes: attrs)
            s.draw(at: CGPoint(x: b.rect.midX - sz.width / 2, y: b.rect.midY - sz.height / 2), withAttributes: attrs)
        }
    }

    // ---- touches ----
    private func keys(at p: CGPoint) -> UInt32 {
        let slop: CGFloat = 8
        for b in buttons where b.rect.insetBy(dx: -slop, dy: -slop).contains(p) { return 1 << UInt32(b.key) }
        let area = dpad.insetBy(dx: -dpad.width * 0.15, dy: -dpad.height * 0.15)
        guard area.contains(p) else { return 0 }
        let dx = p.x - dpad.midX, dy = p.y - dpad.midY
        guard hypot(dx, dy) > dpad.width * 0.1 else { return 0 }
        let angle = atan2(dy, dx) * 180 / .pi // 0 = right, 90 = down
        var k: UInt32 = 0
        if angle > -67.5 && angle < 67.5 { k |= 1 << UInt32(PD_KEY_RIGHT) }
        if angle > 22.5 && angle < 157.5 { k |= 1 << UInt32(PD_KEY_DOWN) }
        if angle > 112.5 || angle < -112.5 { k |= 1 << UInt32(PD_KEY_LEFT) }
        if angle > -157.5 && angle < -22.5 { k |= 1 << UInt32(PD_KEY_UP) }
        return k
    }

    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        style == .below || keys(at: point) != 0
    }

    private func update(_ touches: Set<UITouch>, ended: Bool) {
        for t in touches {
            let id = ObjectIdentifier(t)
            if ended { fingers[id] = nil } else { fingers[id] = keys(at: t.location(in: self)) }
        }
        let now = fingers.values.reduce(0, |)
        if now != pressed {
            if now & ~pressed != 0 { haptic.impactOccurred() }
            pressed = now
            Emulator.shared.setTouchKeys(now)
            setNeedsDisplay()
        }
    }

    override func touchesBegan(_ t: Set<UITouch>, with e: UIEvent?) { update(t, ended: false) }
    override func touchesMoved(_ t: Set<UITouch>, with e: UIEvent?) { update(t, ended: false) }
    override func touchesEnded(_ t: Set<UITouch>, with e: UIEvent?) { update(t, ended: true) }
    override func touchesCancelled(_ t: Set<UITouch>, with e: UIEvent?) { update(t, ended: true) }

    func releaseAll() {
        fingers.removeAll()
        pressed = 0
        Emulator.shared.setTouchKeys(0)
        setNeedsDisplay()
    }
}

struct TouchControls: UIViewRepresentable {
    var style: TouchControlsView.Style
    func makeUIView(context: Context) -> TouchControlsView {
        let v = TouchControlsView()
        v.style = style
        return v
    }
    func updateUIView(_ view: TouchControlsView, context: Context) {
        if view.style != style { view.releaseAll(); view.style = style }
    }
    static func dismantleUIView(_ view: TouchControlsView, coordinator: ()) { view.releaseAll() }
}
