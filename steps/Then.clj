(ns Then
  (:require [cucumber.steps :refer [Then]]
            [steps.inputs   :as inputs]
            [steps.outputs  :as outputs]))

(Then "the following input paths are displayed"
      inputs/verify-input-groups-are-displayed)

(Then "the following input paths are NOT displayed"
      inputs/verify-input-groups-not-displayed)

(Then "the following outputs are displayed in the results page"
      outputs/verify-outputs-in-results)

