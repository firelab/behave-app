(ns Given
  (:require
   [cucumber.by :as by]
   [cucumber.element :as e]
   [cucumber.steps :refer [Given]]
   [cucumber.webdriver :as w]))

(def ^:private worksheet-modules
  {[:surface] "Surface Only"
   [:surface :contain] "Surface and Contain"
   [:surface :crown] "Surface & Crown"
   [:surface :mortality] "Surface and Mortality"
   [:mortality] "Mortality Only"})

(defn- select-new-worksheet-in-guided
  [modules {:keys [driver url]}]
  (w/maximize driver)

  (if (= "https://behave-dev.sig-gis.com" (w/execute-script! driver "window.location.href"))
    (w/execute-script! driver "window.location.href = window.location.href")
    (w/goto driver url))

  (let [wait (w/wait driver 5000)]
    (.until wait (w/presence-of (by/css ".working-area"))))

  (-> (e/find-el driver (by/attr= :text "New Run"))
      (e/click!))

  (Thread/sleep 100)

  (-> (e/find-el driver (by/css ".button--highlight"))
      (e/click!))

  (Thread/sleep 100)

  (-> (e/find-el driver (by/attr= :text "Open using Guided Workflow"))
      (e/click!))

  (Thread/sleep 100)

  (-> (e/find-el driver (by/css ".button--highlight"))
      (e/click!))

  (Thread/sleep 100)

  (-> (e/find-el driver (by/attr= :text (get worksheet-modules (or modules [:surface]))))
      (e/click!))

  (let [el (e/find-el driver (by/css ".button--highlight"))]
    (w/execute-script! driver "arguments[0].scrollIntoView(true)" el))

  (-> (e/find-el driver (by/css ".button--highlight"))
      (e/click!))

  (w/execute-script! driver "window.scrollTo(0,0)")
  {:driver driver})

(Given "I have started a new Surface Worksheet in Guided Mode"
       (partial select-new-worksheet-in-guided [:surface]))

(Given "I have started a new Surface & Mortality Worksheet in Guided Mode"
       (partial select-new-worksheet-in-guided [:surface :mortality]))

(Given "I have started a new Surface & Crown Worksheet in Guided Mode" (partial select-new-worksheet-in-guided [:surface :crown]))
(Given "I have started a new Surface & Contain Worksheet in Guided Mode" (partial select-new-worksheet-in-guided [:surface :contain]))

