(ns behave.importer
  (:require [clojure.data.xml :as xml]
            [clojure.string   :as str]
            [re-frame.core    :as rf]))

(defn bp6-name->variable [bp6-name]
  (first @(rf/subscribe [:query '[:find [?v ...]
                                  :in $ ?code-name
                                  :where [?v :variable/bp6-code-name ?code-name]]
                         [bp6-name]])))

(defn group-var-info [gv]
  (let [result @(rf/subscribe [:pull
                               '[{:group/_group-variables
                                  [:group/repeat? {:submodule/_groups [:submodule/io]}]}]
                               gv])]

    {:io       (get-in result [:group/_group-variables 0 :submodule/_groups 0 :submodule/io])
     :repeat?  (get-in result [:group/_group-variables 0 :group/repeat?])
     :group-id (get-in result [:group/_group-variables 0 :db/id])}))

(defn output-var? [{:keys [tag name]}]
  (and (= tag :property) (str/includes? name "Calc")))

(defn value-accessor [kind]
  (condp = kind
    "continuous" :value
    "discrete"   :code
    "text"       :text))

(defn bp6-input->worksheet-input [v]
  (when-let [var-id (bp6-name->variable (:name v))]
    (when-let [var-info @(rf/subscribe [:pull '[*] var-id])]
      (when-let [gv-id (get-in var-info [:variable/group-variables 0 :db/id])]
        (let [gv-info (group-var-info gv-id)]
          (when (= :input (:io gv-info))
            (let [accessor (value-accessor (:variable/kind var-info))
                  value    (get v accessor)]
              (cond
                (str/blank? value)
                nil

                (:repeat? gv-info)
                (map-indexed [idx val]
                             [[:inputs (:group-id gv-info) idx gv-id] val] (str/split #"," value))

                :else
                [[[:inputs (:group-id gv-info) 0 gv-id] value]]))))))))

(defn bp6-output->worksheet-output [v]
  (when-let [var-id (bp6-name->variable (:name v))]
    (when-let [var-info @(rf/subscribe [:pull '[*] var-id])]
      (when-let [gv-id (get-in var-info [:variable/group-variables 0 :db/id])]
        (let [gv-info (group-var-info gv-id)]
          (when (= :output (:io gv-info))
            [[:outputs gv-id] (= "true" (:value v))]))))))

(defn create-worksheet [parsed-vars]
  (let [group-vars (filter some? (map bp6-input->worksheet-input parsed-vars))]
    (reduce (fn [acc [path value]] (assoc-in acc path value)) {} group-vars)))

(defn parse-worksheet [contents]
  (->> contents
       (xml/parse-str)
       (:content)
       (filter #(contains? #{:variable :property} (:tag %)))
       (mapv (fn [{:keys [tag attrs]}]
               (merge {:tag tag}
                      (select-keys attrs [:tag :name :code :decimals :units :value :text]))))))

(defn count-repeat-groups [worksheet]
  (let [repeats @(rf/subscribe [:vms/query '[:find [?e ...] :where [?e :group/repeat? true]]])]
    (reduce (fn [{:keys [outputs] :as worksheet} group-id]
              (assoc-in worksheet [:repeat-groups group-id] (count (get outputs group-id))))
            worksheet
            repeats)))

(defn import-worksheet [file]
  (when file
    (let [reader  (js/FileReader.)
          on-read #(->> (.-result reader)
                            (reset! previous-file)
                            (parse-worksheet)
                            (create-worksheet)
                            (count-repeat-groups)
                            (conj [:state/merge [:worksheet]])
                            (rf/dispatch))]
      (.addEventListener reader "load" on-read)
      (.readAsText reader file))))
