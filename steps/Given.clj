(ns Given
  (:require
   [cucumber.by :as by]
   [cucumber.element :as e]
   [cucumber.steps :refer [Given]]
   [cucumber.webdriver :as w]))

(def ^:private worksheet-modules
  {[:surface]            "Surface Only"
   [:surface :contain]   "Surface and Contain"
   [:surface :crown]     "Surface and Crown"
   [:surface :mortality] "Surface and Mortality"
   [:mortality]          "Mortality Only"})

(defn- select-independent-worksheet
  [modules {:keys [driver url]}]
  (w/maximize driver)

  (if (= "https://behave-dev.sig-gis.com" (w/execute-script! driver "window.location.href"))
    (w/execute-script! driver "window.location.href = window.location.href")
    (w/goto driver url))

  (let [wait (w/wait driver 5000)]
    (.until wait (w/presence-of (by/css ".card__header"))))

  ;; Select Standard Workflow
  (-> (e/find-el driver (by/attr= :text "Standard Workflow"))
      (e/click!))

  (-> (e/find-el driver (by/css ".button--highlight"))
      (e/click!))

  (Thread/sleep 1000)
  ;; Select Surface Only worksheet
  (-> (e/find-el driver (by/attr= :text (get worksheet-modules (or modules [:surface]))))
      (e/click!))

  (let [el (e/find-el driver (by/css ".button--highlight"))]
    (w/execute-script! driver "arguments[0].scrollIntoView(true)" el))

  (-> (e/find-el driver (by/css ".button--highlight"))
      (e/click!))

  (w/execute-script! driver "window.scrollTo(0,0)")
  {:driver driver})

(Given "I have started a Surface Worksheet" (partial select-independent-worksheet [:surface]))

(Given "I have started a Surface and Crown Worksheet" (partial select-independent-worksheet [:surface :crown]))

(Given "I have started a Surface and Contain Worksheet" (partial select-independent-worksheet [:surface :contain]))

(Given "I have started a Surface and Mortality Worksheet" (partial select-independent-worksheet [:surface :mortality]))

(Given "I have started a Mortality Worksheet" (partial select-independent-worksheet [:mortality]))
