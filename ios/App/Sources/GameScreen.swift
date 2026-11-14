import SwiftUI
import UIKit

/// Shows the core's newest frame (nearest-neighbour, kept in its shape) and passes touches on it to the core as the
/// libretro pointer (the companion takes taps). Tells the core its size, so it picks its canvas for it.
///
/// A Nintendo DS game has two of these: the game's frame (the top screen, with the HUD, the companion and the quick
/// menu) and `.dsTouch`, the DS's touch screen, where the finger is the pen.
final class GameScreenView: UIView {
    enum Kind { case game, dsTouch }
    var kind = Kind.game

    private static let maxW = 1280, maxH = 960
    private let buffer = UnsafeMutableRawPointer.allocate(byteCount: maxW * maxH * 4, alignment: 16)
    private var serial: UInt64 = 0
    private var frameSize = CGSize(width: 240, height: 160)
    private var link: CADisplayLink?
    private let colorSpace = CGColorSpace(name: CGColorSpace.sRGB)!
    private var lastPanel = CGSize.zero

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .black
        isMultipleTouchEnabled = false
        layer.magnificationFilter = .nearest
        layer.minificationFilter = .linear
        layer.contentsGravity = .resizeAspect
        isUserInteractionEnabled = true
        // The finger as the libretro pointer, through a recognizer that sees every touch from down to up (inside a
        // SwiftUI hierarchy, plain touchesBegan didn't reach the core). Pressed for 0 s = it starts at the touch.
        let press = UILongPressGestureRecognizer(target: self, action: #selector(pressed(_:)))
        press.minimumPressDuration = 0
        press.allowableMovement = .greatestFiniteMagnitude
        press.cancelsTouchesInView = false
        press.delaysTouchesBegan = false
        addGestureRecognizer(press)
    }

    /// A faint dot under the finger while it's down (a test build's aid: shows the touch reached the game).
    private let touchDot: CALayer = {
        let l = CALayer()
        l.bounds = CGRect(x: 0, y: 0, width: 22, height: 22)
        l.cornerRadius = 11
        l.backgroundColor = UIColor(white: 1, alpha: 0.35).cgColor
        l.isHidden = true
        return l
    }()

    @objc private func pressed(_ g: UILongPressGestureRecognizer) {
        let p = g.location(in: self)
        if touchDot.superlayer == nil { layer.addSublayer(touchDot) }
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        switch g.state {
        case .began, .changed:
            pointer(at: p, down: true)
            touchDot.position = p
            touchDot.isHidden = false
        default:
            pointer(at: p, down: false)
            touchDot.isHidden = true
        }
        CATransaction.commit()
    }

    required init?(coder: NSCoder) { fatalError() }

    deinit { buffer.deallocate() }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        link?.invalidate()
        link = nil
        guard window != nil else { return }
        let l = CADisplayLink(target: self, selector: #selector(tick))
        l.preferredFrameRateRange = CAFrameRateRange(minimum: 30, maximum: 60, preferred: 60)
        l.add(to: .main, forMode: .common)
        link = l
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let scale = window?.screen.scale ?? UIScreen.main.scale
        let px = CGSize(width: (bounds.width * scale).rounded(), height: (bounds.height * scale).rounded())
        if kind == .game, px != lastPanel, px.width > 0, px.height > 0 {
            lastPanel = px
            Emulator.shared.setPanel(width: Int(px.width), height: Int(px.height))
        }
    }

    @objc private func tick() {
        var w: Int32 = 0, h: Int32 = 0
        let s: UInt64
        if kind == .dsTouch {
            s = pd_touch_copy(serial, buffer)
            w = 256
            h = 192
        } else {
            s = pd_frame_copy(serial, buffer, Int32(Self.maxW), Int32(Self.maxH), &w, &h)
        }
        guard s != 0, s != serial, w > 0, h > 0 else { return }
        serial = s
        let bytes = Int(w) * Int(h) * 4
        guard let provider = CGDataProvider(data: Data(bytes: buffer, count: bytes) as CFData),
              let image = CGImage(width: Int(w), height: Int(h), bitsPerComponent: 8, bitsPerPixel: 32,
                                  bytesPerRow: Int(w) * 4, space: colorSpace,
                                  bitmapInfo: CGBitmapInfo(rawValue: CGBitmapInfo.byteOrder32Little.rawValue
                                      | CGImageAlphaInfo.noneSkipFirst.rawValue),
                                  provider: provider, decode: nil, shouldInterpolate: false,
                                  intent: .defaultIntent) else { return }
        frameSize = CGSize(width: Int(w), height: Int(h))
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        layer.contents = image
        CATransaction.commit()
    }

    /// Where the frame is drawn inside the view (resizeAspect).
    private var frameRect: CGRect {
        guard frameSize.width > 0, frameSize.height > 0, bounds.width > 0, bounds.height > 0 else { return bounds }
        let s = min(bounds.width / frameSize.width, bounds.height / frameSize.height)
        let w = frameSize.width * s, h = frameSize.height * s
        return CGRect(x: (bounds.width - w) / 2, y: (bounds.height - h) / 2, width: w, height: h)
    }

    private func pointer(at p: CGPoint, down: Bool) {
        let r = frameRect
        guard r.width > 0, r.height > 0 else { return }
        if kind == .dsTouch { // the pen, in the touch screen's own pixels
            let px = Int((p.x - r.minX) * 256 / r.width), py = Int((p.y - r.minY) * 192 / r.height)
            pd_set_pen(Int32(max(0, min(255, px))), Int32(max(0, min(191, py))), down)
            return
        }
        let x = Int((p.x - r.minX) * 0xFFFE / r.width) - 0x7FFF
        let y = Int((p.y - r.minY) * 0xFFFE / r.height) - 0x7FFF
        pd_set_pointer(Int32(x), Int32(y), down)
    }

}

struct GameScreen: UIViewRepresentable {
    var kind = GameScreenView.Kind.game
    func makeUIView(context: Context) -> GameScreenView {
        let v = GameScreenView()
        v.kind = kind
        return v
    }
    func updateUIView(_ view: GameScreenView, context: Context) {}
}

/// Where a Nintendo DS game's two screens and its buttons go, in the coordinates of the whole screen.
/// Upright: the two screens one over the other at the top, as on the console, the buttons under them. Sideways: the
/// top screen large on the left, the touch screen beside it top right, the buttons under the touch screen (the
/// D-pad and L lie over the top screen's left side).
struct DSLayout {
    let top: CGRect, touch: CGRect, controls: CGRect

    init(full: CGSize, insets: EdgeInsets, portrait: Bool) {
        if portrait {
            let room = full.height - insets.top - insets.bottom
            let h = max(60, min(full.width * 0.75, (room - 190) / 2))
            let w = h * 4 / 3, x = (full.width - w) / 2
            top = CGRect(x: x, y: insets.top, width: w, height: h)
            touch = CGRect(x: x, y: top.maxY, width: w, height: h)
            controls = CGRect(x: 0, y: touch.maxY, width: full.width, height: full.height - insets.bottom - touch.maxY)
        } else {
            // Half the side insets: the picture may reach a little under the rounded corners, not far.
            let left = insets.leading * 0.5, right = insets.trailing * 0.5
            let room = full.width - left - right
            let topH = min(full.height, room * 0.62 * 0.75), topW = topH * 4 / 3
            let touchW = room - topW, touchH = touchW * 0.75
            let y = (full.height - topH) / 2
            top = CGRect(x: left, y: y, width: topW, height: topH)
            touch = CGRect(x: top.maxX, y: y, width: touchW, height: touchH)
            controls = CGRect(x: touch.minX + 6, y: touch.maxY + 6, width: touchW - 6,
                              height: full.height - insets.bottom - touch.maxY - 10)
        }
    }
}
