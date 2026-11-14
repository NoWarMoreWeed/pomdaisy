import SwiftUI
import UIKit

/// The on-screen buttons. One UIView takes every finger itself (several at once, sliding from button to button), so
/// D-pad + A + B can be held together. Touches that hit no button fall through to the game below (overlay mode).
final class TouchControlsView: UIView {
    /// `.below` / `.overlay`: a Game Boy game upright / sideways. `.dsBelow` / `.dsSide`: a Nintendo DS game, whose
    /// buttons go into `panel` (the room DSLayout leaves beside its two screens). `.dsOverlay`: a DS game sideways with
    /// EXPAND, the buttons over the game like a Game Boy game's.
    enum Style { case below, overlay, dsBelow, dsSide, dsOverlay }

    struct Button {
        let label: String
        let key: Int32          // PD_KEY_*
        var rect: CGRect = .zero
        let round: Bool
        let small: Bool
        /// The app's own button (melonDS's TOUCH): it calls onSpecial when pressed and never reaches the game.
        var special = false
    }

    /// melonDS: the core has no companion, so the DS's extra button is TOUCH (the touch screen in or out).
    var melon = false {
        didSet { if melon != oldValue { buttons = Self.makeButtons(ds: ds, melon: melon); setNeedsLayout(); setNeedsDisplay() } }
    }
    var onSpecial: (() -> Void)?

    var style: Style = .below {
        didSet {
            if ds != Self.isDS(oldValue) { buttons = Self.makeButtons(ds: ds, melon: melon) }
            setNeedsLayout()
            setNeedsDisplay()
        }
    }
    /// A DS game's room for its buttons, in this view's coordinates (the whole screen).
    var panel = CGRect.zero { didSet { if panel != oldValue { setNeedsLayout() } } }
    /// .below: the controls' area starts here (the game is above it, and touches there pass through to the game).
    var reservedTop: CGFloat = 0 { didSet { if reservedTop != oldValue { setNeedsLayout() } } }

    /// A Nintendo DS game: X and Y are the game's own (four face buttons in a diamond), and the DS core's button is
    /// L2 (a tap: the companion, held: the quick menu).
    private var ds: Bool { Self.isDS(style) }
    private static func isDS(_ s: Style) -> Bool { s == .dsBelow || s == .dsSide || s == .dsOverlay }

    private static func makeButtons(ds: Bool, melon: Bool = false) -> [Button] {
        var b = [
            Button(label: "A", key: Int32(PD_KEY_A), round: true, small: false),
            Button(label: "B", key: Int32(PD_KEY_B), round: true, small: false),
            Button(label: "L", key: Int32(PD_KEY_L), round: false, small: false),
            Button(label: "R", key: Int32(PD_KEY_R), round: false, small: false),
            Button(label: "SELECT", key: Int32(PD_KEY_SELECT), round: false, small: true),
            Button(label: "START", key: Int32(PD_KEY_START), round: false, small: true),
        ]
        if ds {
            b += [
                Button(label: "X", key: Int32(PD_KEY_X), round: true, small: false),
                Button(label: "Y", key: Int32(PD_KEY_Y), round: true, small: false),
                melon ? Button(label: "TOUCH", key: Int32(PD_KEY_L2), round: false, small: true, special: true)
                    : Button(label: "BEGLEITER", key: Int32(PD_KEY_L2), round: false, small: true),
            ]
        } else {
            // The Game Boy core's own buttons: X opens the companion, Y the quick menu.
            b += [
                Button(label: "BEGLEITER", key: Int32(PD_KEY_X), round: false, small: true),
                Button(label: "MENÜ", key: Int32(PD_KEY_Y), round: false, small: true),
            ]
        }
        return b
    }

    private var buttons = TouchControlsView.makeButtons(ds: false)
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
        var b = bounds.inset(by: safeAreaInsets)
        if style == .below && reservedTop > b.minY {
            b.size.height -= reservedTop - b.minY
            b.origin.y = reservedTop
        }
        func set(_ key: Int32, _ r: CGRect) {
            if let i = buttons.firstIndex(where: { $0.key == key }) { buttons[i].rect = r }
        }
        // The DS's four face buttons round (cx, cy).
        func diamond(_ cx: CGFloat, _ cy: CGFloat, _ face: CGFloat) {
            func at(_ dx: CGFloat, _ dy: CGFloat) -> CGRect {
                CGRect(x: cx + dx * face - face / 2, y: cy + dy * face - face / 2, width: face, height: face)
            }
            set(Int32(PD_KEY_A), at(1, 0))
            set(Int32(PD_KEY_B), at(0, 1))
            set(Int32(PD_KEY_X), at(0, -1))
            set(Int32(PD_KEY_Y), at(-1, 0))
        }
        if style == .dsBelow {
            // Under the two screens: L, BEGLEITER, R in a row, then the D-pad, SELECT / START and the face buttons.
            let p = panel.insetBy(dx: 12, dy: 6)
            let rowH = min(30, p.height * 0.18), shoulderW = p.width * 0.22
            set(Int32(PD_KEY_L), CGRect(x: p.minX, y: p.minY, width: shoulderW, height: rowH))
            set(Int32(PD_KEY_R), CGRect(x: p.maxX - shoulderW, y: p.minY, width: shoulderW, height: rowH))
            set(Int32(PD_KEY_L2), CGRect(x: p.midX - p.width * 0.14, y: p.minY, width: p.width * 0.28, height: rowH))
            let top = p.minY + rowH + 8, room = p.maxY - top
            let size = max(60, min(room, p.width * 0.36)), midY = top + room / 2
            dpad = CGRect(x: p.minX, y: midY - size / 2, width: size, height: size)
            let face = size / 3
            diamond(p.maxX - face * 1.5, midY, face)
            let gap = p.width - size * 2
            let smallW = max(40, min(96, gap - 24)), smallH = min(30, room * 0.22)
            set(Int32(PD_KEY_SELECT), CGRect(x: p.midX - smallW / 2, y: midY - smallH - 4, width: smallW, height: smallH))
            set(Int32(PD_KEY_START), CGRect(x: p.midX - smallW / 2, y: midY + 4, width: smallW, height: smallH))
            setNeedsDisplay()
            return
        }
        if style == .dsOverlay {
            // Over the wide picture: the D-pad and L left, the face buttons and R right, SELECT / START and TOUCH low.
            let unit = min(b.width, b.height) * 0.2
            let pad = unit * 2.6, face = unit * 0.9
            let cellW = unit * 1.45, cellH = unit * 0.45
            let midY = b.minY + b.height * 0.6
            dpad = CGRect(x: b.minX + unit * 0.3, y: midY - pad / 2, width: pad, height: pad)
            diamond(b.maxX - unit * 0.3 - face * 1.5, midY, face)
            set(Int32(PD_KEY_L), CGRect(x: b.minX + unit * 0.2, y: b.minY + unit * 0.2, width: cellW * 1.1, height: cellH * 1.2))
            set(Int32(PD_KEY_R), CGRect(x: b.maxX - unit * 0.2 - cellW * 1.1, y: b.minY + unit * 0.2, width: cellW * 1.1, height: cellH * 1.2))
            let by = b.maxY - cellH - unit * 0.1
            set(Int32(PD_KEY_SELECT), CGRect(x: b.midX - cellW * 1.5 - 8, y: by, width: cellW, height: cellH))
            set(Int32(PD_KEY_L2), CGRect(x: b.midX - cellW / 2, y: by, width: cellW, height: cellH))
            set(Int32(PD_KEY_START), CGRect(x: b.midX + cellW / 2 + 8, y: by, width: cellW, height: cellH))
            setNeedsDisplay()
            return
        }
        if style == .dsSide {
            // Under the touch screen: R, BEGLEITER, SELECT, START in two rows, the face buttons right of them. The
            // D-pad and L lie over the top screen's left side.
            let p = panel
            let face = max(36, min(p.height / 3, 56))
            diamond(p.maxX - face * 1.5, p.midY, face)
            let gridW = p.width - face * 3 - 10
            let cellW = (gridW - 8) / 2, cellH = min(36, (p.height - 8) / 2)
            let y0 = p.midY - cellH - 4, y1 = p.midY + 4
            set(Int32(PD_KEY_R), CGRect(x: p.minX, y: y0, width: cellW, height: cellH))
            set(Int32(PD_KEY_L2), CGRect(x: p.minX + cellW + 8, y: y0, width: cellW, height: cellH))
            set(Int32(PD_KEY_SELECT), CGRect(x: p.minX, y: y1, width: cellW, height: cellH))
            set(Int32(PD_KEY_START), CGRect(x: p.minX + cellW + 8, y: y1, width: cellW, height: cellH))
            let left = safeAreaInsets.left * 0.5 + 14
            let size = min(bounds.height * 0.42, 180)
            dpad = CGRect(x: left, y: bounds.maxY - safeAreaInsets.bottom - size - 10, width: size, height: size)
            set(Int32(PD_KEY_L), CGRect(x: left, y: bounds.minY + 16, width: cellW, height: cellH))
            setNeedsDisplay()
            return
        }
        let unit = min(b.width, b.height) * (style == .below ? 0.16 : 0.2)
        let pad = unit * 2.6
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
            // The core's buttons go in a row of their own under the shoulders (beside them they overlapped L / R).
            let ty = b.minY + unit * 0.15 + shoulderH + unit * 0.3
            set(Int32(PD_KEY_X), CGRect(x: b.midX - smallW - unit * 0.15, y: ty, width: smallW, height: smallH))
            set(Int32(PD_KEY_Y), CGRect(x: b.midX + unit * 0.15, y: ty, width: smallW, height: smallH))
        default:
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
        // Over the game the buttons are faint; a DS game sideways has only its D-pad and L there.
        let alpha: CGFloat = style == .overlay || style == .dsOverlay ? 0.35 : style == .dsSide ? 0.5 : 0.85
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
            var size = b.small ? b.rect.height * 0.42 : b.rect.height * 0.45
            let s = NSString(string: b.label)
            var attrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: size, weight: .bold),
                .foregroundColor: stroke,
            ]
            var sz = s.size(withAttributes: attrs)
            let room = b.rect.width * 0.84
            if sz.width > room { // a long label (BEGLEITER) shrinks to fit its button
                size *= room / sz.width
                attrs[.font] = UIFont.systemFont(ofSize: size, weight: .bold)
                sz = s.size(withAttributes: attrs)
            }
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
        // Only the buttons (and, below the game, the controls' own area) take touches; the rest goes to the game.
        keys(at: point) != 0 || (style == .below && point.y > reservedTop) || (ds && style != .dsOverlay && panel.contains(point))
    }

    private func update(_ touches: Set<UITouch>, ended: Bool) {
        for t in touches {
            let id = ObjectIdentifier(t)
            if ended { fingers[id] = nil } else { fingers[id] = keys(at: t.location(in: self)) }
        }
        let now = fingers.values.reduce(0, |)
        if now != pressed {
            if now & ~pressed != 0 { haptic.impactOccurred() }
            let special = buttons.filter(\.special).reduce(UInt32(0)) { $0 | 1 << UInt32($1.key) }
            if now & ~pressed & special != 0 { onSpecial?() }
            pressed = now
            Emulator.shared.setTouchKeys(now & ~special)
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
    var reservedTop: CGFloat = 0
    var panel = CGRect.zero
    var melon = false
    var onSpecial: (() -> Void)? = nil
    func makeUIView(context: Context) -> TouchControlsView {
        let v = TouchControlsView()
        v.melon = melon
        v.style = style
        v.reservedTop = reservedTop
        v.panel = panel
        v.onSpecial = onSpecial
        return v
    }
    func updateUIView(_ view: TouchControlsView, context: Context) {
        if view.style != style { view.releaseAll(); view.style = style }
        if view.melon != melon { view.releaseAll(); view.melon = melon }
        view.reservedTop = reservedTop
        view.panel = panel
        view.onSpecial = onSpecial
    }
    static func dismantleUIView(_ view: TouchControlsView, coordinator: ()) { view.releaseAll() }
}
