(ns jcef.setup-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [jcef.setup :as setup])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [me.friwi.jcefmaven CefBuildInfo EnumPlatform]))

;;; Helpers

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "jcef-setup-test" (make-array FileAttribute 0))))

(defn- write-build-meta! [dir release-tag]
  (spit (io/file dir "build_meta.json")
        (format (str "{\"jcef_url\": \"https://example.test/jcef\", "
                     "\"release_url\": \"https://example.test/release\", "
                     "\"release_tag\": \"%s\", \"platform\": \"%s\"}")
                release-tag
                (.getIdentifier (EnumPlatform/getCurrentPlatform)))))

(defn- fake-bundle!
  "Populates `dir` as a fake JCEF bundle; options drop individual pieces."
  [dir & {:keys [natives? lock? release-tag]
          :or   {natives? true lock? true release-tag (.getReleaseTag (CefBuildInfo/fromClasspath))}}]
  (.mkdirs (io/file dir))
  (when natives? (.mkdirs (io/file dir (#'setup/native-marker (#'setup/os)))))
  (when lock? (spit (io/file dir "install.lock") "."))
  (when release-tag (write-build-meta! dir release-tag))
  dir)

(defn- temp-app-dir
  "Fresh `<tmp>/app`, so macOS's `app/../Frameworks` stays per-test."
  []
  (doto (io/file (temp-dir) "app") .mkdirs))

(defn- app-dir-with-bundle! [& opts]
  (let [app-dir (temp-app-dir)]
    (apply fake-bundle! (#'setup/bundle-dir app-dir) opts)
    app-dir))

;;; installation-problem

(deftest ^:parallel installation-problem-test
  (testing "complete bundle"
    (is (nil? (#'setup/installation-problem (fake-bundle! (temp-dir))))))
  (testing "missing pieces are named"
    (is (re-find #"not found" (#'setup/installation-problem (fake-bundle! (temp-dir) :natives? false))))
    (is (= "install.lock not found" (#'setup/installation-problem (fake-bundle! (temp-dir) :lock? false))))
    (is (= "build_meta.json not found" (#'setup/installation-problem (fake-bundle! (temp-dir) :release-tag nil)))))
  (testing "release tag mismatch"
    (is (re-find #"bundle is old-tag but jcefmaven expects"
                 (#'setup/installation-problem (fake-bundle! (temp-dir) :release-tag "old-tag"))))))

;;; installation

(deftest ^:parallel packaged-installation-test
  (testing "complete bundle: skip install, no warning"
    (is (= {:skip-install? true :warning nil}
           (select-keys (#'setup/installation (str (app-dir-with-bundle!)) false)
                        [:skip-install? :warning :error]))))
  (testing "never installs, even with allow-download"
    (is (true? (:skip-install? (#'setup/installation (str (temp-app-dir)) true)))))
  (testing "missing natives is an error"
    (is (:error (#'setup/installation (str (temp-app-dir)) false))))
  (testing "incomplete bundle with natives only warns"
    (let [result (#'setup/installation (str (app-dir-with-bundle! :lock? false)) false)]
      (is (nil? (:error result)))
      (is (= "install.lock not found" (:warning result))))))

(deftest ^:parallel dev-installation-test
  (testing "complete bundle: skip install"
    (is (true? (:skip-install? (#'setup/dev-installation (fake-bundle! (temp-dir)) false)))))
  (testing "missing bundle without allow-download is an error, never an install"
    (let [result (#'setup/dev-installation (temp-dir) false)]
      (is (true? (:skip-install? result)))
      (is (re-find #"behave.jcef.allow-download" (:error result)))))
  (testing "missing bundle with allow-download installs"
    (let [result (#'setup/dev-installation (temp-dir) true)]
      (is (false? (:skip-install? result)))
      (is (nil? (:error result))))))

;;; packaged-app-dir

(deftest packaged-app-dir-test
  (testing "app.dir wins"
    (let [prev (System/getProperty "app.dir")]
      (try
        (System/setProperty "app.dir" "/some/app")
        (is (= "/some/app" (#'setup/packaged-app-dir)))
        (finally
          (if prev
            (System/setProperty "app.dir" prev)
            (System/clearProperty "app.dir")))))))
