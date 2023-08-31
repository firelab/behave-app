(ns behave.schema.utils
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(def many-ref?    (s/and set? #(every? integer? %)))

(def single-ref?  integer?)

(def uuid-string? (s/and string? #(uuid? (parse-uuid %))))

(def valid-key?   (s/and string? #(re-find #"^[a-z\:\-\_]+$" %)))

(def zero-pos?    (s/and integer? (comp not neg?)))

(def valid-io? (s/and keyword? #(#{:input :output} %)))
