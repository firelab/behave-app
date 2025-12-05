(ns Then
  (:require [cucumber.steps :refer [Then]]
            [steps.inputs :as inputs]))

(Then "the following input Submodule -> Groups are displayed: {submodule-groups}"
      inputs/verify-input-groups-are-displayed)

(Then "the following input Submodule -> Groups are NOT displayed: {submodule-groups}"
      inputs/verify-input-groups-not-displayed)
