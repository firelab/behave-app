(ns Then
  (:require
   [clojure.string :as str]
   [cucumber.element :as e]
   [cucumber.by :as by]
   [cucumber.webdriver :as w]
   [cucumber.steps :refer [Then]]))

(defn- extract-submodule-groups
  [submodule-groups]
  (-> submodule-groups
      (str/replace "\"\"\"" "")
      (str/split #"- ")
      (->> (map str/trim)
           (remove empty?)
           (map #(str/split % #" > ")))))

(defn- select-submodule [driver submodule]
  (-> (e/find-el driver (by/css ".wizard"))
      (e/find-el (by/attr= :text submodule))
      (e/click!)))

(defn- navigate-to-inputs [driver]
  (-> (e/find-el driver (by/css ".wizard-header__io-tabs"))
      (e/find-el (by/attr= :text "Inputs"))
      (e/click!)))

(defn- group-exits? [driver [submodule & groups]]
  (select-submodule driver submodule)
  (let [wait (w/wait driver 5000)]
    (.until wait (w/presence-of (by/css ".wizard-page__body"))))
  (doall (map #(e/find-el driver (by/attr= :text %)) groups)))

(Then "(?m)the following input Submodule > Groups are displayed: {submodule-groups}"
      (fn [{:keys [driver]} submodule-groups]
        (navigate-to-inputs driver)
        (let [submodule-groups (extract-submodule-groups submodule-groups)]
          ;; incorrect-groups (filter (fn [[_submodule group]] (= "Slope" group)) submodule-groups)]
          (doall (map (partial group-exits? driver) submodule-groups))
          (assert (pos? (count submodule-groups))))))

(comment
  (count incorrect-groups)
  (pr-str (map pr-str incorrect-groups)))


(comment
  (do
    (require '[cucumber.runner :as r]
             '[cucumber.webdriver :as w])

    (let [d r/driver-atom]
      (e/find-el @d (by/attr= :text "Wind measured at: ")))))
