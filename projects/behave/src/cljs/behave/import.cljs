(ns behave.import
  (:require [clojure.data.xml :as xml]
            [re-frame.core :as rf]))

(defn bp6-name->variable [bp6-name]
  (first @(rf/subscribe [:query '[:find [?v ...]
                                  :in $ ?code-name
                                  :where [?v :variable/bp6-code-name ?code-name]]
                         [bp6-name]])))

(defn group-var-info [gv]
  (let [result @(rf/subscribe [:pull
                               '[{:group/_group-variables [{:submodule/_groups [:submodule/io]}]}]
                               gv])]

    {:io       (get-in result [:group/_group-variables 0 :submodule/_groups 0 :submodule/io])
     :group-id (get-in result [:group/_group-variables 0 :db/id])}))

(defn create-worksheet [parsed-vars]
  (let [group-vars (filter some? (map (fn [v]
                                        (when-let [var-id (bp6-name->variable (:name v))]
                                          (when-let [var-info @(rf/subscribe [:pull '[*] var-id])]
                                            (when-let [gv-id (get-in var-info [:variable/group-variables 0 :db/id])]
                                              (let [gv-info (group-var-info gv-id)]
                                                [(if (= :input (:io gv-info))
                                                   [:inputs (:group-id gv-info) 0 gv-id]
                                                   [:outputs gv-id])
                                                 (if (= :output (:io gv-info))
                                                   true
                                                   (condp = (:variable/kind var-info)
                                                     "continuous" (:value v)
                                                     "discrete"   (:code v)
                                                     "text"       (:text v)))])))))
                                      parsed-vars))]
    (reduce (fn [acc [path value]] (assoc-in acc path value)) {} group-vars)))

(defn parse-worksheet [contents]
  (->> contents
       (xml/parse-str)
       (:content)
       (filter #(= :variable (:tag %)))
       (mapv (fn [{:keys [attrs]}]
               (select-keys attrs [:name :code :decimals :units :value :text])))))

(defn import-worksheet [file]
  (when file
    (let [reader (js/FileReader.)
          on-read     #(->> (.-result reader)
                            (parse-worksheet)
                            (create-worksheet)
                            (conj [:state/merge [:worksheet]])
                            (rf/dispatch))]
      (.addEventListener reader "load" on-read)
      (.readAsText reader file))))
