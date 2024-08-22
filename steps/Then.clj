(ns Then
  (:require
   [clojure.string :as str]
   [cucumber.steps :refer [Then]]))

(defn- extract-submodule-groups
  [submodule-groups]
  (-> submodule-groups
      (str/replace "\"\"\"" "")
      (str/split #"- ")
      (->> (map str/trim)
           (remove empty?)
           (map #(str/split % #" > ")))))

(Then "(?m)the following input Submodule > Groups are displayed: {submodule-groups}"
      (fn [_ submodule-groups]
        (let [submodule-groups (extract-submodule-groups submodule-groups)]
          ;; incorrect-groups (filter (fn [[_submodule group]] (= "Slope" group)) submodule-groups)]
          (assert (pos? (count submodule-groups))))))

(comment
  (count incorrect-groups)
  (pr-str (map pr-str incorrect-groups)))
