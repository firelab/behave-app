(ns cucumber.steps
  (:require
   [tegere.steps :as s]))

;; Steps
(def ^{:doc      "Step definition."
       :argslist '([step-text perform])}
  Given s/Given)

(def ^{:doc      "Step definition."
       :argslist '([step-text perform])}
  Then s/Then)

(def ^{:doc      "Step definition."
       :argslist '([step-text perform])}
  When s/When)
