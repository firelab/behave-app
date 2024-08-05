(ns When
  (:require
   [cucumber.steps :refer [When]]))

(When "I select the output {output} in the {submodule} submodule"
      (fn [{:keys [driver]} output submodule]
        ()
        {:surface   true
         :submodule submodule
         :output    output}))
