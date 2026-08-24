(ns cucumber.report
  "Reporting for the cucumber runner (scripts/cucumber_run_driver.clj): failure
   extraction and org/EDN rendering + persistence. Pure formatting — no Selenium/tegere
   code dependency (only tegere-namespaced keywords are read from the result maps)."
  (:require [clojure.java.io :as io]
            [clojure.string  :as str]))

;;; ---------------------------------------------------------------------------
;;; Formatting / failure extraction
;;; ---------------------------------------------------------------------------

(defn fmt-duration [secs]
  (let [s (long secs) m (quot s 60) r (rem s 60)] (format "%dm %02ds" m r)))

(defn clean-reason [r]
  (-> (str r)
      (str/replace #"\s*\|?\s*(Build info:|System info:|Driver info:|Capabilities \{|Session ID:|For documentation on this error|\(Session info:).*$" "")
      (str/replace #"\s*\n\s*" " ")
      str/trim))

(defn scenario-title [ex]
  (let [sc (:tegere.runner/scenario ex)]
    (or (:tegere.parser/description sc) (:tegere.parser/name sc) "(unnamed scenario)")))

(defn- step-err [step] (get-in step [:tegere.runner/execution :tegere.runner/err]))

(defn ex-failure
  "nil if the executable passed, else {:scenario :type :step :reason}."
  [ex]
  (when-let [bad (some (fn [st] (when-let [e (step-err st)] [st e]))
                       (:tegere.parser/steps ex))]
    (let [[st e] bad
          msg    (or (not-empty (:tegere.runner/message e))
                     (first (:tegere.runner/stack-trace e))
                     (name (:tegere.runner/type e :fail)))
          stept  (str (some-> (:tegere.parser/type st) name str/capitalize) " "
                      (:tegere.parser/text st))]
      {:scenario (scenario-title ex)
       :type     (:tegere.runner/type e)
       :step     (str/trim stept)
       :reason   (str/replace (str/trim msg) #"\s*\n\s*" " | ")})))

(defn counts [all-files status executables]
  (let [state-of (fn [f] (:state (get status f)))]
    {:passed           (count (filter #(= :pass    (state-of %)) all-files))
     :failed           (count (filter #(= :fail    (state-of %)) all-files))
     :skipped          (count (filter #(= :skip    (state-of %)) all-files))
     :pending          (count (filter #(= :pending (state-of %)) all-files))
     :scenarios-passed (- (count executables) (count (filter :failure executables)))
     :scenarios-failed (count (filter :failure executables))
     :failed-files     (->> all-files (filter #(= :fail (state-of %))) vec)}))

;;; ---------------------------------------------------------------------------
;;; Rendering / persistence
;;; ---------------------------------------------------------------------------

(defn render-org
  [{:keys [url query headless stop elapsed all-files status executables]}]
  (let [{:keys [passed failed skipped pending scenarios-passed scenarios-failed]}
        (counts all-files status executables)
        sb                                                                        (StringBuilder.)]
    (.append sb "#+TITLE: Cucumber Test Results\n")
    (.append sb (str "#+DATE: " (java.time.LocalDate/now) "\n"))
    (.append sb (str "# Config: query " query ", :headless? " (boolean headless)
                     ", :stop " (boolean stop) "\n"))
    (.append sb (str "#         url " url "\n"))
    (.append sb (str "# Elapsed" (when (pos? pending) " so far") ": "
                     (fmt-duration elapsed) " (" (format "%.1f" elapsed) "s)\n"))
    (.append sb (str "# Files: " (count all-files) " total — " passed " passed, " failed " failed, "
                     skipped " skipped" (when (pos? pending) (str ", " pending " pending")) "\n"))
    (.append sb (str "# Scenarios: " scenarios-passed " passed, " scenarios-failed " failed\n\n"))
    (.append sb "* Results\n")
    (doseq [file all-files]
      (let [{:keys [state fails]} (get status file)]
        (case state
          :pass    (.append sb (str "- [X] " file "\n"))
          :skip    (.append sb (str "- [-] " file "  (SKIPPED: no matching scenario for query)\n"))
          :pending (.append sb (str "- [ ] " file "  :PENDING:\n"))
          :fail    (do (.append sb (str "- [ ] " file "\n"))
                       (doseq [f fails]
                         (.append sb (str "  - Scenario \"" (:scenario f) "\": "
                                          (str/upper-case (name (:type f)))
                                          " at step [" (:step f) "] — " (clean-reason (:reason f)) "\n")))))))
    (.toString sb)))

(defn write!
  "Render + persist the org report and raw EDN. `:summary? true` (the final write) adds
   `:summary` to the EDN — written last so the incremental writes never clobber it."
  [{:keys [org edn elapsed all-files status executables] :as ctx} & {:keys [summary?]}]
  (io/make-parents org)
  (spit org (render-org ctx))
  (spit edn (pr-str (cond-> {:elapsed-seconds elapsed :executables executables}
                      summary? (assoc :summary (counts all-files status executables))))))
