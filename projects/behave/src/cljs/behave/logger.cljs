(ns behave.logger
  (:require [clojure.string :as str]))

(defonce ^:private DEBUG true)

(defn log [& s]
  (when DEBUG
    (println (apply str ">> [Log - Debug] " (str/join " " s)))))
