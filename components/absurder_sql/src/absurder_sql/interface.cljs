(ns absurder-sql.interface
  (:require [absurder-sql.core :as c]))

(def init! c/init!)
(def connected? c/connected?)

(def connect! c/connect!)
(def close! c/close!)
