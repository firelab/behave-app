(ns browser
  "Shared headless-capable Chrome/Chromium resolution for babashka tasks.
   Lifted from projects/behave/bb.edn's test:ci :init so the cucumber:shard task can
   feed the resolved binary to Selenium as :browser-path."
  (:require [babashka.fs    :as fs]
            [clojure.string :as str]))

(defn find-browser
  "Headless-capable Chrome/Chromium path: $CHROME_BIN, else per-OS defaults."
  []
  (or (System/getenv "CHROME_BIN")
      (let [os    (str/lower-case (System/getProperty "os.name"))
            cands (cond
                    (str/includes? os "mac")
                    ["/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
                     "/Applications/Chromium.app/Contents/MacOS/Chromium"]
                    (str/includes? os "win")
                    ["C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"
                     "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"]
                    :else
                    ["google-chrome" "google-chrome-stable" "chromium" "chromium-browser"])]
        (or (some (fn [c]
                    (cond (fs/exists? c) c
                          (fs/which c)   (str (fs/which c))))
                  cands)
            (throw (ex-info "No Chrome/Chromium found. Set CHROME_BIN." {:tried cands}))))))
