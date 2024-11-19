(ns cucumber
  (:require [clojure.java.io :as io]
            [clojure.string  :as str]
            [tegere.loader :refer [load-feature-files]]
            [tegere.steps  :refer [registry Given When Then]]
            [tegere.runner :refer [run]]
            [clojure.test  :refer [is]]))

(def features (load-feature-files (io/file "features")))

(reset! registry {})

(Given "I have started a Surface Worksheet" identity)

(When "I select the output {output} in the {submodule} submodule"
      (fn [_ output submodule]
        {:surface   true
         :submodule submodule
         :output    output}))

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
         (let [submodule-groups (extract-submodule-groups submodule-groups)
               incorrect-groups (filter (fn [[_submodule group]] (= "Slope" group)) submodule-groups)]
           (is (= 0 (count incorrect-groups) (format "Found %d incorrect groups: %s"
                                                     (count incorrect-groups)
                                                     (pr-str (map pr-str incorrect-groups))))))))

(run features @registry)
