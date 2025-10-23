(ns Then
  (:require [cucumber.steps :refer [Then]]
            [cucumber.by :as by]
            [steps.helpers :as h]
            [steps.inputs :as inputs]))

(Then "(?m)the following input Submodule > Groups are displayed: {submodule-groups}"
      inputs/verify-input-groups)

(Then "the element with text {string} should be visible"
      (fn [{:keys [driver]} text]
        (h/wait-for-nested-element driver (by/css "body") text 5000)
        {:driver driver}))

(Then "the {string} tab should be active"
      (fn [{:keys [driver]} tab-name]
        {:driver driver}))
