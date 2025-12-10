(ns Then
  (:require [cucumber.steps :refer [Then]]
            [steps.inputs :as inputs]))

(Then "the following input paths are displayed"
      inputs/verify-input-groups-are-displayed)

(Then "the following input paths are NOT displayed"
      inputs/verify-input-groups-not-displayed)

