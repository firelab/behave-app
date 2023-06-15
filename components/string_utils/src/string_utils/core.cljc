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
  (when (seq s)
    (-> s
        (str/lower-case)
        (remove-punctuation)
        (str/replace #"[\s_]" "-"))))

(defn camel->snake
  "camelCase string to snake_case"
  [s]
  (str
    (str/lower-case (first s))
    (str/replace (subs s 1)
                 #"([A-Z])" #(str "_" (str/lower-case (first %))))))

(defn camel->kebab
  "camelCase string to snake_case"
  [s]
  (str
    (str/lower-case (first s))
    (str/replace (subs s 1) #"([A-Z])" #(str "-" (str/lower-case (first %))))))

(defn snake-key
  [& xs]
  (str/join ":" (map #(-> % (->str) (->snake)) xs)))

(defn kebab-key
  [& xs]
  (str/join ":" (map #(-> % (->str) (->kebab)) xs)))

(defn start-with
  "Prepends `start` to `s` as long as `s` doesn't already start with `start`."
  [s start]
  (if-not (str/starts-with? s start)
    (str start s)
    s))

(defn end-with
  "Appends `end` to `s` as long as `s` doesn't already end with `end`."
  [s end]
  (str s
       (when-not (str/ends-with? s end)
         end)))

