import SwiftUI
import UIKit

/// Shows the core's newest frame (nearest-neighbour, kept in its shape) and passes touches on it to the core as the
/// libretro pointer (the companion takes taps). Tells the core its size, so it picks its canvas for it.
final class GameScreenView: UIView {
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
        if px != lastPanel, px.width > 0, px.height > 0 {
            lastPanel = px
            Emulator.shared.setPanel(width: Int(px.width), height: Int(px.height))
        }
    }

    @objc private func tick() {
        var w: Int32 = 0, h: Int32 = 0
        let s = pd_frame_copy(serial, buffer, Int32(Self.maxW), Int32(Self.maxH), &w, &h)
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

    private func pointer(_ touch: UITouch, down: Bool) {
        let p = touch.location(in: self), r = frameRect
        guard r.width > 0, r.height > 0 else { return }
        let x = Int((p.x - r.minX) * 0xFFFE / r.width) - 0x7FFF
        let y = Int((p.y - r.minY) * 0xFFFE / r.height) - 0x7FFF
        pd_set_pointer(Int32(x), Int32(y), down)
    }

    override func touchesBegan(_ t: Set<UITouch>, with e: UIEvent?) { if let f = t.first { pointer(f, down: true) } }
    override func touchesMoved(_ t: Set<UITouch>, with e: UIEvent?) { if let f = t.first { pointer(f, down: true) } }
    override func touchesEnded(_ t: Set<UITouch>, with e: UIEvent?) { if let f = t.first { pointer(f, down: false) } }
    override func touchesCancelled(_ t: Set<UITouch>, with e: UIEvent?) { if let f = t.first { pointer(f, down: false) } }
}

struct GameScreen: UIViewRepresentable {
    func makeUIView(context: Context) -> GameScreenView { GameScreenView() }
    func updateUIView(_ view: GameScreenView, context: Context) {}
}
