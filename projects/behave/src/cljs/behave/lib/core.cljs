(ns behave.lib.core
  (:require [behave.lib.contain :as contain]
            [behave.lib.enums :as enums]))

(defn get-enum [enum member]
  (get-in @enums/all-enums [enum member]))
