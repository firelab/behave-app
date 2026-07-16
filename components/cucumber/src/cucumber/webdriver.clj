(ns cucumber.webdriver
  (:require [cucumber.remote :as remote])
  (:import [java.time Duration]
           [org.openqa.selenium By WebDriver]
           [org.openqa.selenium JavascriptExecutor]
           [org.openqa.selenium.chrome ChromeDriver ChromeOptions]
           [org.openqa.selenium.firefox FirefoxDriver]
           [org.openqa.selenium.safari SafariDriver]
           [org.openqa.selenium.support.ui WebDriverWait ExpectedConditions]))

(defn goto
  "Navigate to url."
  [^WebDriver d url]
  (.get d url))

(defn presence-of
  "Expect the presence of an element."
  [^By by]
  (ExpectedConditions/presenceOfElementLocated by));

(defn presence-of-nested-elements
  "Expect the presence of a child element under a parent element."
  [^By parent ^By child]
  (ExpectedConditions/presenceOfNestedElementsLocatedBy parent child))

(defn staleness-of
  "Expect that `el` is no longer attached to the DOM — i.e. the page re-rendered. Lets a
   caller wait for a click to take effect (old element goes stale) instead of a fixed sleep."
  [^org.openqa.selenium.WebElement el]
  (ExpectedConditions/stalenessOf el))

(defn quit
  "Quit the webdriver."
  [^WebDriver driver]
  (.quit driver))

(defn title
  "Get the title of the current website."
  [^WebDriver driver]
  (.getTitle driver))

(defn wait
  "Wait for a given duration."
  [^WebDriver driver duration]
  (WebDriverWait. driver (Duration/ofMillis duration)))

(defn execute-script!
  "Executes JavaScript code."
  [^JavascriptExecutor driver script & args]
  (.executeScript driver script (into-array args)))

(defn add-init-script!
  "Register JS to run on every new document BEFORE the page's own scripts, via CDP
   (Page.addScriptToEvaluateOnNewDocument). Lets us seed localStorage on the app's
   origin without a throwaway load-then-reload. Chrome only: returns true when
   registered, false when the driver doesn't support CDP (caller should fall back)."
  [driver source]
  (when (instance? ChromeDriver driver)
    (.executeCdpCommand ^ChromeDriver driver
                        "Page.addScriptToEvaluateOnNewDocument"
                        {"source" source})
    true))

(defn ready?
  "Returns true if the document is ready."
  [^JavascriptExecutor driver]
  (= "complete" (.executeScript driver "return document.readyState" (into-array []))))

(defn wait-until-page-load
  "Waits until JS returns"
  [^WebDriver driver timeout-ms]
  (.until (WebDriverWait. driver (Duration/ofMillis timeout-ms))
          (reify java.util.function.Function
            (apply [_ web-driver]
              (ready? web-driver)))))

(defn delete-cookies
  "Deletes all cookies."
  [^WebDriver driver]
  (.. driver (manage) (deleteAllCookies)))

(defn maximize
  "Maxmizes the browser window"
  [^WebDriver d]
  (.. d (manage) (window) (maximize)))

(defn set-window-size
  "Set an explicit window size. Headless `maximize` has no real screen to size to, so
   the viewport can end up short and fixed-bottom elements (page__footer) overlap
   content — set a deterministic size instead."
  [^WebDriver d width height]
  (.. d (manage) (window) (setSize (org.openqa.selenium.Dimension. (int width) (int height)))))

(defn chrome-driver
  "Instatiate a Chrome WebDriver."
  [{:keys [browser-path headless?]}]
  (let [options (ChromeOptions.)]
    (when browser-path (.setBinary options browser-path))
    (.addArguments options (into-array
                            (cond-> ["disable-infobars" ; // disabling infobars
                                     "--disable-extensions" ; // disabling extensions
                                     "--disable-gpu" ; // applicable to windows os only
                                     "--disable-dev-shm-usage" ; // overcome limited resource problems
                                     "--no-sandbox" ; // Bypass OS security model
                                     "--remote-debugging-port=0"] ; // auto-assign: a fixed port collides when sharding runs N parallel Chromes
                              headless?       (concat ["--headless=new" ; // run in headless mode
                                                       "--start-maximized"
                                                       "--window-size=2560,1080"]) ; // set window size for headless
                              (not headless?) (conj "--start-maximized")))) ; // maximize when not headless
    (System/setProperty "webdriver.chrome.driver"
                        (or (System/getenv "CHROMEDRIVER_PATH")
                            "/usr/local/bin/chromedriver"))
    (ChromeDriver. options)))

(defn firefox-driver
  "Instatiate a Firefox WebDriver."
  [_]
  (FirefoxDriver.))

(defn safari-driver
  "Instatiate a Safari WebDriver."
  [_]
  (SafariDriver.))

(defn remote-driver
  "Instatiate a remote WebDriver."
  [opts]
  (remote/remote-driver opts))

(defn driver
  "Instantiates a new WebDriver"
  [{:keys [browser remote] :as opts}]
  (println (format "Creating WD -- Remote?: %s Browser: Options: %s" remote opts))
  (if remote
    (remote-driver opts)
    (condp = (keyword browser)
      :chrome (chrome-driver opts)
      :firefox (firefox-driver opts)
      :safari (safari-driver opts))))
