(ns When
  (:require [cucumber.steps :refer [When]]
            [steps.helpers :as h]
            [steps.outputs :as outputs]))

(When "I select these outputs Submodule > Group > Output: {outputs}"
      outputs/select-outputs)

(When "I navigate to the {string} tab"
      (fn [{:keys [driver]} tab-name]
        (h/navigate-to-tab driver tab-name)
        {:driver driver}))

(When "I click {string}"
      (fn [{:keys [driver]} button-text]
        (h/click-button-with-text driver button-text)
        {:driver driver}))
