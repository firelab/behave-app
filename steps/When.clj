(ns When
  (:require [cucumber.steps :refer [When]]
            [steps.outputs :as outputs]
            [steps.inputs :as inputs]))

(When "these outputs are selected Submodule -> Group -> Output: {outputs}"
      outputs/select-outputs)

(When "these outputs are NOT selected Submodule -> Group -> Output: {outputs}"
      outputs/verify-outputs-not-selected)

(When "these inputs are entered Submodule -> Group -> Input: {inputs}"
      inputs/enter-inputs)
