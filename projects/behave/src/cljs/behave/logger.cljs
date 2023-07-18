(ns behave.logger
  (:require [clojure.string :as str]))

(defn log [& s]
  (when js/goog.DEBUG
    (println (apply str ">> [Log - Debug] " (str/join " " s)))))
