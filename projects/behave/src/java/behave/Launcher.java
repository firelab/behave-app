package behave;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

/**
 * Splash-first entry point (see STARTUP.org). Loading the Clojure
 * application tree costs many seconds of class-init on slow hardware —
 * clojure.core alone is ~5s on a weak 2-core VM — so the splash is shown
 * from pure Java (~0.5s) before any Clojure class loads. Keep this class
 * free of Clojure references until after the splash is visible; the first
 * Clojure.var() call triggers the clojure.core load.
 *
 * <p>The splash layout mirrors jcef.loading/create-loader.
 */
public final class Launcher {

    private static final String TITLE = "Behave7";
    private static final String ICON  = "/public/images/android-chrome-512x512.png";

    private static long uptimeMs() {
        long now = System.currentTimeMillis();
        return ProcessHandle.current().info().startInstant()
                .map(start -> now - start.toEpochMilli())
                .orElse(-1L);
    }

    /** Builds and shows the splash frame; mirrors jcef.loading/create-loader. */
    private static JFrame showSplash() {
        JFrame frame = new JFrame("Loading " + TITLE);
        JLabel loadingLabel = new JLabel("Loading " + TITLE + "...");
        loadingLabel.setOpaque(true);
        loadingLabel.setBackground(Color.WHITE);
        loadingLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setHorizontalTextPosition(SwingConstants.CENTER);

        frame.setResizable(false);
        frame.setLayout(new BorderLayout());
        URL iconUrl = Launcher.class.getResource(ICON);
        if (iconUrl != null) {
            Image scaled = new ImageIcon(iconUrl).getImage()
                    .getScaledInstance(256, 256, Image.SCALE_DEFAULT);
            frame.add(new JLabel(new ImageIcon(scaled)), BorderLayout.NORTH);
        }
        frame.add(loadingLabel, BorderLayout.SOUTH);
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screen.width - frame.getWidth()) / 2,
                          (screen.height - frame.getHeight()) / 2);
        frame.setVisible(true);
        return frame;
    }

    public static void main(String[] args) {
        System.out.println("[TIMING] launcher entered " + uptimeMs() + "ms after JVM start");
        if (System.getProperty("app.dir") != null) {
            JFrame splash = showSplash();
            System.out.println("[TIMING] splash shown " + uptimeMs() + "ms after JVM start");
            IFn require = Clojure.var("clojure.core", "require");
            require.invoke(Clojure.read("behave.core"));
            IFn startCef = Clojure.var("behave.core", "start-cef!");
            startCef.invoke(splash);
        } else {
            IFn require = Clojure.var("clojure.core", "require");
            require.invoke(Clojure.read("behave.core"));
            IFn main = Clojure.var("behave.core", "-main");
            main.applyTo(clojure.lang.RT.seq(args));
        }
    }
}
