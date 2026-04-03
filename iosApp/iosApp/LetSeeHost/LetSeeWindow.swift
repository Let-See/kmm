import UIKit
import LetSeeUI

/// Transparent overlay window that floats the LetSee debug button — and an optional
/// quick-access mock panel — above all app content.
///
/// ## Overlay strategy
/// Rather than a full-screen Compose view (which renders an opaque background), this window
/// uses a **native UIKit** layout for the floating widget so the app behind is never obscured.
/// The full Compose debug panel is still presented modally on tap via
/// `ComposeEntryPointKt.LetSeeDebugViewController`.
///
/// ## Quick-access panel
/// When `getQuickAccessData` returns non-nil the container expands leftward to show:
/// - the request path as a small label
/// - a horizontal, scrollable row of mock pill buttons
///
/// Tapping a pill calls `respondToRequestWithMock` which dispatches back to the Kotlin
/// coroutine layer and resolves the request immediately.
///
/// ## Touch passthrough
/// `point(inside:with:)` is overridden to forward only touches that land within the
/// interactive area (the container view's frame) to this window; everything else reaches
/// the app's window hierarchy below.
final class LetSeeWindow: UIWindow {

    // MARK: - Layout constants

    private enum Layout {
        static let buttonSize: CGFloat = 56
        static let cornerRadius: CGFloat = 16
        static let spacing: CGFloat = 8
        static let edgeMargin: CGFloat = 24
        static let pillHeight: CGFloat = 28
        static let pillCornerRadius: CGFloat = 8
        static let pillHPad: CGFloat = 10
        static let pathLabelFont = UIFont.systemFont(ofSize: 11, weight: .semibold)
        static let pillFont = UIFont.boldSystemFont(ofSize: 12)
    }

    // MARK: - Views

    /// Container that holds the quick-access card + FAB side-by-side and is the sole
    /// draggable element.  Its frame determines the touch-passthrough area.
    private let containerView: UIView = {
        let v = UIView()
        v.backgroundColor = .clear
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let button: UIButton = {
        let btn = UIButton(type: .system)
        btn.setTitle("LS", for: .normal)
        btn.titleLabel?.font = .boldSystemFont(ofSize: 18)
        btn.setTitleColor(.white, for: .normal)
        btn.backgroundColor = UIColor(red: 0.30, green: 0.69, blue: 0.31, alpha: 1) // LetSee green
        btn.layer.cornerRadius = Layout.buttonSize / 2
        btn.layer.shadowColor = UIColor.black.cgColor
        btn.layer.shadowOpacity = 0.25
        btn.layer.shadowOffset = CGSize(width: 0, height: 2)
        btn.layer.shadowRadius = 4
        btn.clipsToBounds = false
        btn.isAccessibilityElement = true
        btn.accessibilityLabel = "LetSee debug button"
        btn.accessibilityTraits = .button
        btn.accessibilityIdentifier = "letsee_floating_button"
        btn.translatesAutoresizingMaskIntoConstraints = false
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
        lbl.translatesAutoresizingMaskIntoConstraints = false
        return lbl
    }()

    // Quick-access card — hidden until a pending request with specific mocks appears.

    private let quickAccessCard: UIView = {
        let v = UIView()
        v.backgroundColor = .systemBackground
        v.layer.cornerRadius = Layout.cornerRadius
        v.layer.shadowColor = UIColor.black.cgColor
        v.layer.shadowOpacity = 0.15
        v.layer.shadowOffset = CGSize(width: 0, height: 2)
        v.layer.shadowRadius = 6
        v.clipsToBounds = false
        v.isHidden = true
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()

    private let requestPathLabel: UILabel = {
        let lbl = UILabel()
        lbl.font = Layout.pathLabelFont
        lbl.textColor = .secondaryLabel
        lbl.lineBreakMode = .byTruncatingHead
        lbl.translatesAutoresizingMaskIntoConstraints = false
        return lbl
    }()

    private let pillScrollView: UIScrollView = {
        let sv = UIScrollView()
        sv.showsHorizontalScrollIndicator = false
        sv.showsVerticalScrollIndicator = false
        sv.alwaysBounceHorizontal = true
        sv.translatesAutoresizingMaskIntoConstraints = false
        return sv
    }()

    private let pillStack: UIStackView = {
        let sv = UIStackView()
        sv.axis = .horizontal
        sv.spacing = 6
        sv.alignment = .center
        sv.translatesAutoresizingMaskIntoConstraints = false
        return sv
    }()

    // MARK: - State

    private var letSee: (any LetSee)?
    private var pollTimer: Timer?
    private var lastQuickAccessRequestPath: String?

    // Container positioning (relative to rootVC.view top-left, updated by drag).
    private var containerXConstraint: NSLayoutConstraint?
    private var containerYConstraint: NSLayoutConstraint?

    // MARK: - Init

    override init(windowScene: UIWindowScene) {
        super.init(windowScene: windowScene)
        windowLevel = .alert + 1
        backgroundColor = .clear
        isUserInteractionEnabled = true

        let rootVC = PassthroughViewController()
        rootViewController = rootVC

        buildLayout(in: rootVC.view)
        animateButtonAppearance()
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }

    // MARK: - Layout construction

    private func buildLayout(in parent: UIView) {
        // Add container to parent.
        parent.addSubview(containerView)

        // Anchor container: starts at bottom-leading so the card can open rightward.
        let initialX = Layout.edgeMargin
        let initialY = UIScreen.main.bounds.height - Layout.buttonSize - Layout.edgeMargin * 3

        containerXConstraint = containerView.leadingAnchor.constraint(
            equalTo: parent.leadingAnchor, constant: initialX)
        containerYConstraint = containerView.topAnchor.constraint(
            equalTo: parent.topAnchor, constant: initialY)
        containerXConstraint?.isActive = true
        containerYConstraint?.isActive = true

        // --- FAB ---
        containerView.addSubview(button)
        NSLayoutConstraint.activate([
            button.widthAnchor.constraint(equalToConstant: Layout.buttonSize),
            button.heightAnchor.constraint(equalToConstant: Layout.buttonSize),
            button.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            button.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            button.topAnchor.constraint(equalTo: containerView.topAnchor),
            button.bottomAnchor.constraint(equalTo: containerView.bottomAnchor),
        ])

        // --- Badge ---
        containerView.addSubview(badgeLabel)
        NSLayoutConstraint.activate([
            badgeLabel.widthAnchor.constraint(greaterThanOrEqualToConstant: 20),
            badgeLabel.heightAnchor.constraint(equalToConstant: 20),
            badgeLabel.centerXAnchor.constraint(equalTo: button.trailingAnchor, constant: -4),
            badgeLabel.centerYAnchor.constraint(equalTo: button.topAnchor, constant: 4),
        ])

        // --- Quick-access card ---
        // The card lives directly in parent (sibling of containerView) so it can span all the
        // space to the RIGHT of the button up to the screen's right margin.
        // Its leading edge tracks containerView's trailing edge; its trailing edge is pinned to
        // the screen right margin at lower priority so it collapses gracefully when the button
        // is dragged to the right side of the screen.
        parent.addSubview(quickAccessCard)
        let cardTrailing = quickAccessCard.trailingAnchor.constraint(
            equalTo: parent.trailingAnchor, constant: -Layout.edgeMargin)
        cardTrailing.priority = .defaultHigh          // breakable so width never goes negative
        NSLayoutConstraint.activate([
            quickAccessCard.leadingAnchor.constraint(
                equalTo: containerView.trailingAnchor, constant: Layout.spacing),
            cardTrailing,
            quickAccessCard.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            quickAccessCard.widthAnchor.constraint(greaterThanOrEqualToConstant: 0),
        ])

        // Card internals: path label + scroll view with pill stack.
        quickAccessCard.addSubview(requestPathLabel)
        quickAccessCard.addSubview(pillScrollView)
        pillScrollView.addSubview(pillStack)

        NSLayoutConstraint.activate([
            requestPathLabel.topAnchor.constraint(
                equalTo: quickAccessCard.topAnchor, constant: 8),
            requestPathLabel.leadingAnchor.constraint(
                equalTo: quickAccessCard.leadingAnchor, constant: 10),
            requestPathLabel.trailingAnchor.constraint(
                equalTo: quickAccessCard.trailingAnchor, constant: -10),

            pillScrollView.topAnchor.constraint(
                equalTo: requestPathLabel.bottomAnchor, constant: 6),
            pillScrollView.leadingAnchor.constraint(
                equalTo: quickAccessCard.leadingAnchor, constant: 10),
            pillScrollView.trailingAnchor.constraint(
                equalTo: quickAccessCard.trailingAnchor, constant: -10),
            pillScrollView.bottomAnchor.constraint(
                equalTo: quickAccessCard.bottomAnchor, constant: -8),
            pillScrollView.heightAnchor.constraint(equalToConstant: Layout.pillHeight),

            pillStack.topAnchor.constraint(equalTo: pillScrollView.topAnchor),
            pillStack.bottomAnchor.constraint(equalTo: pillScrollView.bottomAnchor),
            pillStack.leadingAnchor.constraint(equalTo: pillScrollView.leadingAnchor),
            pillStack.trailingAnchor.constraint(equalTo: pillScrollView.trailingAnchor),
            pillStack.heightAnchor.constraint(equalTo: pillScrollView.heightAnchor),
        ])

        // Gestures.
        button.addTarget(self, action: #selector(buttonTapped), for: .touchUpInside)
        let pan = UIPanGestureRecognizer(target: self, action: #selector(handlePan(_:)))
        containerView.addGestureRecognizer(pan)
    }

    // MARK: - Configuration

    func configure(letSee: any LetSee) {
        self.letSee = letSee
        startPolling()
    }

    // MARK: - Touch passthrough

    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        if rootViewController?.presentedViewController != nil { return true }
        guard let parent = rootViewController?.view else { return false }
        let containerFrame = containerView.convert(containerView.bounds, to: parent)
        if containerFrame.insetBy(dx: -8, dy: -8).contains(point) { return true }
        if !quickAccessCard.isHidden {
            let cardFrame = quickAccessCard.convert(quickAccessCard.bounds, to: parent)
            if cardFrame.insetBy(dx: -8, dy: -8).contains(point) { return true }
        }
        return false
    }

    // MARK: - Button actions

    @objc private func buttonTapped() {
        guard let letSee else { return }
        let panelVC = ComposeEntryPointKt.LetSeeDebugViewController(letSee: letSee)
        panelVC.modalPresentationStyle = .pageSheet
        if let sheet = panelVC.sheetPresentationController {
            sheet.detents = [.medium(), .large()]
            sheet.prefersGrabberVisible = true
            sheet.prefersScrollingExpandsWhenScrolledToEdge = true
        }
        rootViewController?.present(panelVC, animated: true)
    }

    // MARK: - Drag

    @objc private func handlePan(_ gesture: UIPanGestureRecognizer) {
        guard let parent = rootViewController?.view else { return }
        let translation = gesture.translation(in: parent)
        let safeArea = parent.safeAreaLayoutGuide.layoutFrame

        let minX = safeArea.minX + Layout.edgeMargin
        let maxX = safeArea.maxX - Layout.buttonSize - Layout.edgeMargin
        let minY = safeArea.minY + Layout.edgeMargin
        let maxY = safeArea.maxY - Layout.buttonSize - Layout.edgeMargin

        let currentX = containerXConstraint?.constant ?? minX
        let currentY = containerYConstraint?.constant ?? minY

        let nextX = max(minX, min(currentX + translation.x, maxX))
        let nextY = max(minY, min(currentY + translation.y, maxY))

        containerXConstraint?.constant = nextX
        containerYConstraint?.constant = nextY
        gesture.setTranslation(.zero, in: parent)
    }

    // MARK: - Appearance animation

    private func animateButtonAppearance() {
        containerView.transform = CGAffineTransform(scaleX: 0.01, y: 0.01)
        containerView.alpha = 0
        UIView.animate(
            withDuration: 0.5,
            delay: 0.2,
            usingSpringWithDamping: 0.6,
            initialSpringVelocity: 0.8,
            options: .curveEaseOut
        ) {
            self.containerView.transform = .identity
            self.containerView.alpha = 1
        }
    }

    // MARK: - Polling

    private func startPolling() {
        pollTimer?.invalidate()
        pollTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            self?.refreshState()
        }
    }

    private func refreshState() {
        guard let letSee else { return }

        let replayCache = letSee.requestsManager.requestsStack.replayCache
        let pendingCount: Int
        if let currentList = replayCache.first as? NSArray {
            pendingCount = currentList.count
        } else {
            pendingCount = 0
        }

        let quickAccess = LetSeeIOSHelpersKt.getQuickAccessData(letSee: letSee)

        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.updateBadge(count: pendingCount)
            self.updateQuickAccess(data: quickAccess, letSee: letSee)
        }
    }

    // MARK: - Badge

    private func updateBadge(count: Int) {
        if count > 0 {
            badgeLabel.text = count > 99 ? "99+" : "\(count)"
            badgeLabel.isHidden = false
            button.accessibilityLabel = "LetSee debug button, \(count) pending requests"
        } else {
            badgeLabel.isHidden = true
            button.accessibilityLabel = "LetSee debug button"
        }
    }

    // MARK: - Quick access

    private func updateQuickAccess(data: QuickAccessData?, letSee: any LetSee) {
        guard let data else {
            if !quickAccessCard.isHidden { hideQuickAccess() }
            return
        }

        // Only rebuild pills when the request changes.
        if data.requestPath == lastQuickAccessRequestPath && !quickAccessCard.isHidden { return }
        lastQuickAccessRequestPath = data.requestPath

        requestPathLabel.text = data.requestPath
        rebuildPills(mocks: data.mocks, request: data.request, letSee: letSee)

        if quickAccessCard.isHidden { showQuickAccess() }
    }

    private func rebuildPills(mocks: [QuickAccessMock], request: any Request, letSee: any LetSee) {
        pillStack.arrangedSubviews.forEach { $0.removeFromSuperview() }

        for qaMock in mocks {
            let pill = makePill(for: qaMock)
            pill.accessibilityIdentifier = "quick_access_mock_\(qaMock.displayName)"

            let action = MockPillAction(letSee: letSee, request: request, mock: qaMock.mock)
            pill.addTarget(action, action: #selector(MockPillAction.tapped), for: .touchUpInside)
            // Retain action via associated object so it lives exactly as long as the button.
            objc_setAssociatedObject(pill, &MockPillAction.key, action, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
            pillStack.addArrangedSubview(pill)
        }
    }

    private func makePill(for qaMock: QuickAccessMock) -> UIButton {
        let btn = UIButton(type: .system)
        btn.setTitle(qaMock.displayName, for: .normal)
        btn.titleLabel?.font = Layout.pillFont
        btn.layer.cornerRadius = Layout.pillCornerRadius
        btn.contentEdgeInsets = UIEdgeInsets(
            top: 0, left: Layout.pillHPad, bottom: 0, right: Layout.pillHPad)

        switch qaMock.styleCategory {
        case .success:
            btn.backgroundColor = UIColor(red: 0.30, green: 0.69, blue: 0.31, alpha: 1)
            btn.setTitleColor(.white, for: .normal)
        case .failure:
            btn.backgroundColor = UIColor(red: 0.83, green: 0.18, blue: 0.18, alpha: 1)
            btn.setTitleColor(.white, for: .normal)
        case .live:
            btn.backgroundColor = UIColor(red: 0.00, green: 0.54, blue: 0.48, alpha: 1)
            btn.setTitleColor(.white, for: .normal)
        case .cancel:
            btn.backgroundColor = .secondarySystemFill
            btn.setTitleColor(.label, for: .normal)
        default:
            btn.backgroundColor = .secondarySystemFill
            btn.setTitleColor(.label, for: .normal)
        }

        btn.translatesAutoresizingMaskIntoConstraints = false
        btn.heightAnchor.constraint(equalToConstant: Layout.pillHeight).isActive = true
        return btn
    }

    private func showQuickAccess() {
        quickAccessCard.alpha = 0
        quickAccessCard.transform = CGAffineTransform(scaleX: 0.8, y: 0.8)
        quickAccessCard.isHidden = false
        UIView.animate(withDuration: 0.25, delay: 0, usingSpringWithDamping: 0.7, initialSpringVelocity: 0.5) {
            self.quickAccessCard.alpha = 1
            self.quickAccessCard.transform = .identity
        }
    }

    private func hideQuickAccess() {
        UIView.animate(withDuration: 0.2) {
            self.quickAccessCard.alpha = 0
            self.quickAccessCard.transform = CGAffineTransform(scaleX: 0.8, y: 0.8)
        } completion: { _ in
            self.quickAccessCard.isHidden = true
            self.quickAccessCard.alpha = 1
            self.quickAccessCard.transform = .identity
            self.lastQuickAccessRequestPath = nil
        }
    }

    // MARK: - Teardown

    deinit {
        pollTimer?.invalidate()
    }
}

// MARK: - PassthroughViewController

private final class PassthroughViewController: UIViewController {
    override func loadView() {
        let v = UIView()
        v.backgroundColor = .clear
        v.isUserInteractionEnabled = true
        self.view = v
    }
}

// MARK: - MockPillAction

/// Tiny object that holds the Kotlin references for a single mock pill tap action.
/// Retained by the pill button via an associated object so it lives exactly as long as needed.
private final class MockPillAction: NSObject {
    static var key: UInt8 = 0

    private let letSee: any LetSee
    private let request: any Request
    private let mock: Mock

    init(letSee: any LetSee, request: any Request, mock: Mock) {
        self.letSee = letSee
        self.request = request
        self.mock = mock
    }

    @objc func tapped() {
        LetSeeIOSHelpersKt.respondToRequestWithMock(letSee: letSee, request: request, mock: mock)
    }
}
