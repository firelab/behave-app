(ns When
  (:require [cucumber.steps :refer [When]]
            [steps.outputs :as outputs]
            [steps.inputs :as inputs]))

(When "these output paths are selected"
      outputs/select-outputs)

(When "this output path is selected {submodule} : {group} : {value}"
      outputs/select-output)

(When "these output paths are NOT selected"
      outputs/verify-outputs-not-selected)

(When "these input paths are entered"
      inputs/enter-inputs)

(When "this input path is entered {submodule} : {group} : {value}"
      inputs/enter-input)

(When "this input path is entered {submodule} : {group} : {subgroup} : {value}"
      inputs/enter-input)
