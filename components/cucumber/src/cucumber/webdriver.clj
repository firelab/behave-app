(ns cucumber.webdriver
  (:require [cucumber.remote :as remote])
  (:import [org.openqa.selenium By WebDriver]
           [org.openqa.selenium.safari SafariDriver]
           [org.openqa.selenium.chrome ChromeDriver ChromeOptions]
           [org.openqa.selenium.firefox FirefoxDriver]
           [org.openqa.selenium JavascriptExecutor]
           [org.openqa.selenium.support.ui WebDriverWait ExpectedConditions]
           [java.time Duration]))

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

(defn chrome-driver
  "Instatiate a Chrome WebDriver."
  [{:keys [browser-path]}]
  (let [options (ChromeOptions.)]
    (when browser-path (.setBinary options browser-path))
    (.addArguments options (into-array
                            ["start-maximized"         ; // open Browser in maximized mode
                             "disable-infobars"        ; // disabling infobars
                             "--disable-extensions"    ; // disabling extensions
                             "--disable-gpu"           ; // applicable to windows os only
                             "--disable-dev-shm-usage" ; // overcome limited resource problems
                             "--no-sandbox"            ; // Bypass OS security model
                             "--remote-debugging-port=9222"]))
    (System/setProperty "webdriver.chrome.driver" "/usr/local/bin/chromedriver")
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
