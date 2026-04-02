import UIKit
import LetSeeCore
import LetSeeUI

/// Transparent overlay window that floats a circular "LS" debug button above all app content.
///
/// The window sits at `.alert + 1` with a clear background and passes through all touches
/// except those landing on the button itself (via `point(inside:with:)`).
final class LetSeeWindow: UIWindow {

    // MARK: - Subviews

    private let button: UIButton = {
        let btn = UIButton(type: .system)
        btn.setTitle("LS", for: .normal)
        btn.titleLabel?.font = .boldSystemFont(ofSize: 18)
        btn.setTitleColor(.white, for: .normal)
        btn.backgroundColor = UIColor(red: 0.24, green: 0.36, blue: 0.99, alpha: 1)
        btn.layer.cornerRadius = 28
        btn.layer.shadowColor = UIColor.black.cgColor
        btn.layer.shadowOpacity = 0.3
        btn.layer.shadowOffset = CGSize(width: 0, height: 2)
        btn.layer.shadowRadius = 4
        btn.clipsToBounds = false
        btn.isAccessibilityElement = true
        btn.accessibilityLabel = "LetSee debug button"
        btn.accessibilityTraits = .button
        btn.accessibilityIdentifier = "letsee_floating_button"
        return btn
    }()

    private let badgeLabel: UILabel = {
        let lbl = UILabel()
        lbl.font = .boldSystemFont(ofSize: 10)
        lbl.textColor = .white
        lbl.backgroundColor = .systemRed
        lbl.textAlignment = .center
        lbl.layer.cornerRadius = 10
        lbl.clipsToBounds = true
        lbl.isHidden = true
        lbl.isAccessibilityElement = false
        return lbl
    }()

    // MARK: - State

    private var letSee: LetSeeCore.LetSee?
    private var pollTimer: Timer?

    // MARK: - Init

    override init(windowScene: UIWindowScene) {
        super.init(windowScene: windowScene)
        windowLevel = .alert + 1
        backgroundColor = .clear
        isUserInteractionEnabled = true

        let rootVC = PassthroughViewController()
        rootViewController = rootVC
        rootVC.view.addSubview(button)
        rootVC.view.addSubview(badgeLabel)

        button.translatesAutoresizingMaskIntoConstraints = false
        badgeLabel.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            button.widthAnchor.constraint(equalToConstant: 56),
            button.heightAnchor.constraint(equalToConstant: 56),
            button.trailingAnchor.constraint(equalTo: rootVC.view.safeAreaLayoutGuide.trailingAnchor, constant: -16),
            button.bottomAnchor.constraint(equalTo: rootVC.view.safeAreaLayoutGuide.bottomAnchor, constant: -16),

            badgeLabel.widthAnchor.constraint(greaterThanOrEqualToConstant: 20),
            badgeLabel.heightAnchor.constraint(equalToConstant: 20),
            badgeLabel.centerXAnchor.constraint(equalTo: button.trailingAnchor, constant: -4),
            badgeLabel.centerYAnchor.constraint(equalTo: button.topAnchor, constant: 4),
        ])

        button.addTarget(self, action: #selector(buttonTapped), for: .touchUpInside)

        let pan = UIPanGestureRecognizer(target: self, action: #selector(handlePan(_:)))
        button.addGestureRecognizer(pan)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }

    // MARK: - Configuration

    func configure(letSee: LetSeeCore.LetSee) {
        self.letSee = letSee
        startPollingBadge()
        animateButtonAppearance()
    }

    private func animateButtonAppearance() {
        button.transform = CGAffineTransform(scaleX: 0.01, y: 0.01)
        button.alpha = 0
        UIView.animate(
            withDuration: 0.5,
            delay: 0.2,
            usingSpringWithDamping: 0.6,
            initialSpringVelocity: 0.8,
            options: .curveEaseOut
        ) {
            self.button.transform = .identity
            self.button.alpha = 1
        }
    }

    // MARK: - Touch passthrough

    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        guard let rootView = rootViewController?.view else { return false }
        let buttonFrame = button.convert(button.bounds, to: rootView)
        let hitArea = buttonFrame.insetBy(dx: -8, dy: -8)
        return hitArea.contains(point)
    }

    // MARK: - Button actions

    @objc private func buttonTapped() {
        guard let letSee = letSee else { return }
        let composeVC = ComposeEntryPointKt.LetSeeDebugViewController(letSee: letSee)
        composeVC.modalPresentationStyle = .fullScreen
        rootViewController?.present(composeVC, animated: true)
    }

    @objc private func handlePan(_ gesture: UIPanGestureRecognizer) {
        let translation = gesture.translation(in: button.superview)
        button.center = CGPoint(
            x: button.center.x + translation.x,
            y: button.center.y + translation.y
        )
        gesture.setTranslation(.zero, in: button.superview)

        if gesture.state == .ended {
            snapToNearestEdge()
        }
    }

    // MARK: - Edge snapping

    private func snapToNearestEdge() {
        guard let container = rootViewController?.view else { return }
        let safeArea = container.safeAreaLayoutGuide.layoutFrame
        let margin: CGFloat = 16
        let halfW = button.bounds.width / 2

        var target = button.center
        let leftEdge = safeArea.minX + margin + halfW
        let rightEdge = safeArea.maxX - margin - halfW
        target.x = (target.x < safeArea.midX) ? leftEdge : rightEdge
        target.y = max(safeArea.minY + margin + halfW, min(target.y, safeArea.maxY - margin - halfW))

        // Deactivate trailing/bottom constraints during drag — position is manual now.
        UIView.animate(withDuration: 0.25, delay: 0, usingSpringWithDamping: 0.7, initialSpringVelocity: 0.5) {
            self.button.center = target
        }
    }

    // MARK: - Badge polling

    private func startPollingBadge() {
        pollTimer?.invalidate()
        pollTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            self?.refreshBadge()
        }
    }

    private func refreshBadge() {
        guard let letSee = letSee else { return }
        let replayCache = letSee.requestsManager.requestsStack.replayCache
        let count: Int
        if let currentList = replayCache.first as? NSArray {
            count = currentList.count
        } else {
            count = 0
        }

        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            if count > 0 {
                self.badgeLabel.text = count > 99 ? "99+" : "\(count)"
                self.badgeLabel.isHidden = false
                self.button.accessibilityLabel = "LetSee debug button, \(count) pending requests"
            } else {
                self.badgeLabel.isHidden = true
                self.button.accessibilityLabel = "LetSee debug button"
            }
        }
    }

    deinit {
        pollTimer?.invalidate()
    }
}

// MARK: - PassthroughViewController

/// Bare view controller with a clear view that doesn't interfere with the status bar.
private final class PassthroughViewController: UIViewController {
    override func loadView() {
        let v = UIView()
        v.backgroundColor = .clear
        v.isUserInteractionEnabled = true
        self.view = v
    }
}
