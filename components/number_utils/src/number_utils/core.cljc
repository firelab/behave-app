(ns number-utils.core)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility Functions - Numbers and Calculations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-int [s]
  #?(:clj  (Integer/parseInt s)
     :cljs (js/parseInt s)))

(defn parse-long [s]
  #?(:clj  (Long/parseLong s)
     :cljs (js/parseLong s)))

(defn parse-float [s]
  #?(:clj  (Float/parseFloat s)
     :cljs (js/parseFloat s)))

(defn clean-units
  "Cleans units by adding/not adding a space when needed for units."
  [units]
  (if (#{"%" "\u00B0F" "\u00B0"} units)
    units
    (str " " units)))

(defn is-numeric? [v]
  (if (string? v)
    (some? (re-matches #"^-?([\d]*[\d\,]*\.*[\d]+)$|^-?([\d]+)$" v))
    (number? v)))

(defn num-str-compare
  "Compares two strings as numbers if they are numeric."
  [asc x y]
  (let [both-numbers? (and (is-numeric? x) (is-numeric? y))
        sort-x        (if both-numbers? (parse-float x) x)
        sort-y        (if both-numbers? (parse-float y) y)]
    (if asc
      (compare sort-x sort-y)
      (compare sort-y sort-x))))

(defn to-precision
  "Rounds a double to n significant digits."
  [dbl n]
  (let [factor (.pow #?(:clj Math :cljs js/Math) 10 n)]
    (/ (Math/round (* dbl factor)) factor)))

(defn decimal-precision
  "Sets a decimal to precision specific."
  [n precision]
  #?(:clj  (-> n
               (BigDecimal.)
               (.setScale precision BigDecimal/ROUND_HALF_UP))
     :cljs (-> n
               (.toPrecision precision)
               (js/Number))))
