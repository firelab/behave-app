(ns When
  (:require [cucumber.steps :refer [When]]
            [steps.outputs :as outputs]))

(When "these outputs are selected Submodule > Group > Output: {outputs}"
      outputs/select-outputs)

(When "these outputs are NOT selected Submodule > Group > Output: {outputs}"
      outputs/verify-outputs-not-selected)
