(ns string-utils.core
  (:require [clojure.string :as str]))

(defn- remove-punctuation [s]
  (str/replace s #"[^\w\s\d-_]" ""))

(defn ->str
  "Converts `s` to a string. Removes the colon in front of keywords."
  [s]
  (cond
    (string? s)
    s

    (keyword? s)
    (-> s (str) (subs 1))

    :else
    (str s)))

(defn ->snake
  "String to snake_case"
  [s]
  (-> s
      (str/lower-case)
      (remove-punctuation)
      (str/replace #"[\s-]" "_")))

(defn ->kebab
  "String to kebab-case"
  [s]
  (-> s
      (str/lower-case)
      (remove-punctuation)
      (str/replace #"[\s_]" "-")))

(defn snake-key
  [& xs]
  (str/join ":" (map #(-> % (->str) (->snake)) xs)))

(defn kebab-key
  [& xs]
  (str/join ":" (map #(-> % (->str) (->kebab)) xs)))

(defn end-with
  "Appends `end` to `s` as long as `s` doesn't already end with `end`."
  [s end]
  (str s
       (when-not (str/ends-with? s end)
         end)))

