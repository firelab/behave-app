package behave;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.function.BiConsumer;

/**
 * Splash-first entry point (see STARTUP.org). Loading the Clojure
 * application tree costs many seconds of class-init on slow hardware —
 * clojure.core alone is ~5s on a weak 2-core VM — so the splash is shown
 * from pure Java (~0.5s) before any Clojure class loads. Keep this class
 * free of Clojure references until after the splash is visible; the first
 * Clojure.var() call triggers the clojure.core load.
 *
 * <p>Startup progress checkpoints (percent of the splash progress bar).
 * The Java side drives the namespace tree load in stages so the bar moves
 * during the long load; behave.core/start-cef! reports the tail end via the
 * BiConsumer handed to it. Percentages come from measured load fractions
 * (see STARTUP.org "Splash-first entry"):
 *
 * <pre>
 *   2  splash shown            (Launcher)
 *  20  Clojure runtime loaded  (Launcher — first Clojure.var call)
 *  42  core libraries          (Launcher — clojure.core.async)
 *  62  database engine         (Launcher — datom-store.main)
 *  65  data schemas            (Launcher — behave.schema.core)
 *  82  application services    (Launcher — behave.handlers)
 *  85  application             (Launcher — behave.core)
 *  87  configuration ready     (behave.core/start-cef!)
 *  90  starting browser        (behave.core/start-cef!)
 * 100  browser window shown    (behave.core/start-cef! :on-shown)
 * </pre>
 *
 * <p>Percentages are calibrated to wall-clock fractions measured on the
 * Windows test box (2026-07-07); if the load profile shifts, re-measure via
 * the [TIMING] stage lines this class prints on stdout.
 *
 * <p>Between checkpoints the bar eases toward the target and then creeps
 * slowly (capped ~8% past the target), so it never looks frozen during a
 * long stage. The splash layout mirrors jcef.loading/create-loader.
 */
public final class Launcher {

    private static final String TITLE = "Behave7";
    private static final String ICON  = "/public/images/android-chrome-512x512.png";

    /** Bar resolution: percent * 10, for smooth easing. */
    private static final int SCALE = 10;

    private static JProgressBar progressBar;
    private static JLabel       loadingLabel;
    private static Timer        animator;
    private static volatile int targetValue = 0;

    private static long uptimeMs() {
        long now = System.currentTimeMillis();
        return ProcessHandle.current().info().startInstant()
                .map(start -> now - start.toEpochMilli())
                .orElse(-1L);
    }

    /**
     * Moves the progress bar to `percent` and updates the status text.
     * Safe to call from any thread; also the target of the BiConsumer handed
     * to behave.core/start-cef!.
     */
    public static void setProgress(int percent, String message) {
        SwingUtilities.invokeLater(() -> {
            targetValue = Math.min(100 * SCALE, percent * SCALE);
            if (message != null && loadingLabel != null) {
                loadingLabel.setText(message);
            }
            if (percent >= 100 && animator != null) {
                if (progressBar != null) progressBar.setValue(100 * SCALE);
                animator.stop();
            }
        });
    }

    /** Eases toward the checkpoint, then creeps so the bar never looks stuck. */
    private static void animateTick() {
        int value = progressBar.getValue();
        int creepCap = Math.min(targetValue + 8 * SCALE, 99 * SCALE);
        if (value < targetValue) {
            progressBar.setValue(value + Math.max(2, (targetValue - value) / 8));
        } else if (value < creepCap) {
            progressBar.setValue(value + 1); // ~0.2%/s at 50ms ticks
        }
    }

    /** Builds and shows the splash frame; mirrors jcef.loading/create-loader. */
    private static JFrame showSplash() {
        JFrame frame = new JFrame("Loading " + TITLE);
        loadingLabel = new JLabel("Loading " + TITLE + "...");
        loadingLabel.setOpaque(true);
        loadingLabel.setBackground(Color.WHITE);
        loadingLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setHorizontalTextPosition(SwingConstants.CENTER);

        progressBar = new JProgressBar(0, 100 * SCALE);
        progressBar.setOpaque(true);
        progressBar.setBackground(Color.WHITE);
        progressBar.setBorder(new EmptyBorder(0, 30, 10, 30));

        frame.setResizable(false);
        frame.setLayout(new BorderLayout());
        URL iconUrl = Launcher.class.getResource(ICON);
        if (iconUrl != null) {
            Image scaled = new ImageIcon(iconUrl).getImage()
                    .getScaledInstance(256, 256, Image.SCALE_DEFAULT);
            frame.add(new JLabel(new ImageIcon(scaled)), BorderLayout.NORTH);
        }
        frame.add(progressBar, BorderLayout.CENTER);
        frame.add(loadingLabel, BorderLayout.SOUTH);
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screen.width - frame.getWidth()) / 2,
                          (screen.height - frame.getHeight()) / 2);
        frame.setVisible(true);

        animator = new Timer(50, e -> animateTick());
        animator.start();
        return frame;
    }

    /**
     * Loads one namespace stage, moving the bar to `percent` first so the
     * user sees what is being loaded. Intermediate stages are best-effort:
     * they are subsets of behave.core's tree, so a rename here must not
     * break launch — the final behave.core require is the load-bearing one.
     */
    private static void loadStage(IFn require, int percent, String message, String ns, boolean required) {
        setProgress(percent, message);
        try {
            require.invoke(Clojure.read(ns));
            System.out.println("[TIMING] stage " + ns + " loaded " + uptimeMs() + "ms after JVM start");
        } catch (RuntimeException e) {
            if (required) throw e;
            System.err.println("[WARN] optional preload stage " + ns + " failed: " + e);
        }
    }

    public static void main(String[] args) {
        System.out.println("[TIMING] launcher entered " + uptimeMs() + "ms after JVM start");
        if (System.getProperty("app.dir") != null) {
            JFrame splash = showSplash();
            setProgress(2, "Starting " + TITLE + "...");
            System.out.println("[TIMING] splash shown " + uptimeMs() + "ms after JVM start");

            IFn require = Clojure.var("clojure.core", "require"); // loads clojure.core (slow)
            setProgress(20, "Clojure runtime ready...");
            System.out.println("[TIMING] clojure runtime loaded " + uptimeMs() + "ms after JVM start");

            loadStage(require, 42, "Loading core libraries...",      "clojure.core.async",  false);
            loadStage(require, 62, "Loading database engine...",     "datom-store.main",    false);
            loadStage(require, 65, "Loading data schemas...",        "behave.schema.core",  false);
            loadStage(require, 82, "Loading application services...", "behave.handlers",    false);
            loadStage(require, 85, "Loading application...",         "behave.core",         true);

            IFn startCef = Clojure.var("behave.core", "start-cef!");
            BiConsumer<Integer, String> progress = Launcher::setProgress;
            startCef.invoke(splash, progress);
        } else {
            IFn require = Clojure.var("clojure.core", "require");
            require.invoke(Clojure.read("behave.core"));
            IFn main = Clojure.var("behave.core", "-main");
            main.applyTo(clojure.lang.RT.seq(args));
        }
    }
}
