(ns When
  (:require
   [cucumber.by :as by]
   [cucumber.element :as e]
   [cucumber.steps :refer [When]]
   [clojure.string :as str]
   [cucumber.runner :as r]
   [cucumber.webdriver :as w]))

(defn- extract-submodule-groups
  [submodule-groups]
  (-> submodule-groups
      (str/replace "\"\"\"" "")
      (str/split #"-- ")
      (->> (map str/trim)
           (remove empty?)
           (map #(str/split % #" > ")))))

(defn- select-submodule [driver submodule]
  (-> (e/find-el driver (by/css ".wizard-header__submodules"))
      (e/find-el (by/attr= :text submodule))
      (e/click!)))

(defn- find-groups [driver groups]
  (doseq [group groups]
    (let [wait (w/wait driver 300)]
      (.until wait (w/presence-of-nested-elements
                    (by/css ".wizard-group__header")
                    (by/attr= :text group))))))

(defn- select-output [driver output]
  (-> (e/find-el driver (by/css ".wizard-group__outputs"))
      (e/find-el (by/attr= :text output))
      (e/click!)))

(defn- select-submodule-and-output [driver [submodule & groups]]
  (select-submodule driver submodule)
  (find-groups driver (butlast groups))
  (select-output driver (last groups)))

(defn- select-submodule-and-outputs
  [{:keys [driver]} submodule-groups]
  (let [wait (w/wait driver 5000)]
    (.until wait (w/presence-of (by/css ".wizard"))))
  (let [submodule-groups (extract-submodule-groups submodule-groups)]
    (doseq [output submodule-groups]
      (select-submodule-and-output driver output))
    {:driver driver}))

(When "I select these outputs Submodule > Group > Output: {outputs}" select-submodule-and-outputs)

(comment
  (do
    (require '[cucumber.runner :as r]
             '[cucumber.webdriver :as w])

    (let [d r/driver-atom]
      (select-submodule @d "Fire Behavior"))

    (let [d r/driver-atom]
      (select-submodule-and-output
       @d
       ["Fire Behavior" "Direction Mode" "Heading"]))))
