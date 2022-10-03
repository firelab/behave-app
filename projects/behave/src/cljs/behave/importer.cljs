(ns behave.importer
  (:require [clojure.data.xml :as xml]
            [clojure.string   :as str]
            [re-frame.core    :as rf]))

;;; Variable Cache

(defonce ^:private variable-cache (atom {}))

(defn- bp6-name->variable [bp6-name]
  (get @variable-cache bp6-name))

(defn- cache-variables! []
  (let [bp6-variables @(rf/subscribe [:vms/pull-with-attr
                                      :variable/bp6-code-name
                                      [:variable/bp6-code-name
                                       :variable/kind
                                       {:variable/group-variables
                                        [{:group/_group-variables
                                          [:group/repeat?
                                           {:submodule/_groups
                                            [:submodule/io]}]}]}]])]
    (->> bp6-variables
         (filter :variable/group-variables)
         (reduce (fn [acc cur]
                   (let [group-var (get-in cur [:variable/group-variables 0])
                         group     (get-in group-var [:group/_group-variables 0])]
                     (assoc! acc
                             (:variable/bp6-code-name cur)
                             {:group-id      (:db/id group)
                              :group-var-id  (:db/id group-var)
                              :group-repeat? (:group/repeat? group)
                              :id            (:db/id cur)
                              :io            (get-in group [:submodule/_groups 0 :submodule/io])
                              :kind          (:variable/kind cur)})))
                 (transient {}))
         (persistent!)
         (reset! variable-cache))))

;;; Helper Methods

(defn- output-var? [{:keys [tag name]}]
  (and (= tag :property) (str/includes? name "Calc")))

(defn- bp6-property->bp6-code-name [s]
  (str/replace s #"(.*)Calc(.*)" #(str "v"
                                       (str/capitalize (second %))
                                       (last %))))

(defn- value-accessor [kind]
  (condp = kind
    "continuous" :value
    "discrete"   :code
    "text"       :text))

;;; Worksheet Processing

(defn- bp6-input->worksheet-input [{:keys [name] :as v}]
  (when-let [variable (bp6-name->variable name)]
    (when (= :input (:io variable))
      (let [accessor (value-accessor (:kind variable))
            value    (get v accessor)]
        (cond
          (str/blank? value)
          nil

          (:group-repeat? variable)
          (map-indexed (fn [idx val]
                         [[:inputs (:group-id variable) idx (:group-var-id variable)] val])
                       (str/split value #","))

          :else
          [[[:inputs (:group-id variable) 0 (:group-var-id variable)] value]])))))

(defn- bp6-output->worksheet-output [{:keys [name] :as v}]
  (when-let [variable (bp6-name->variable name)]
    (when (= :output (:io variable))
      [[:outputs (:group-id variable)] true])))


(defn- create-worksheet [parsed-worksheet]
  (let [[outputs inputs] (partition-by :tag parsed-worksheet)
        inputs           (->> inputs
                              (map bp6-input->worksheet-input)
                              (filter some?)
                              (reduce concat))
        outputs          (->> outputs
                              (filter output-var?)
                              (map #(assoc % :name (bp6-property->bp6-code-name (:name %))))
                              (map bp6-output->worksheet-output)
                              (filter some?))]
    (reduce (fn [acc [path value]] (assoc-in acc path value)) {} (concat inputs outputs))))

(defn- parse-worksheet [contents]
  (->> contents
       (xml/parse-str)
       (:content)
       (filter (comp not string?))
       (filter #(contains? #{:variable :property} (:tag %)))
       (mapv (fn [{:keys [tag attrs]}]
               (merge {:tag tag}
                      (select-keys attrs [:tag :name :code :decimals :units :value :text]))))))

(defn- count-repeat-groups [worksheet]
  (let [repeats @(rf/subscribe [:vms/query '[:find [?e ...] :where [?e :group/repeat? true]]])]
    (reduce (fn [{:keys [inputs] :as worksheet} group-id]
              (assoc-in worksheet [:repeat-groups group-id] (count (get inputs group-id))))
            worksheet
            repeats)))

;;; Public Fns

(defn import-worksheet [file]
  (when file
    (cache-variables!)
    (let [reader  (js/FileReader.)
          on-read #(->> (.-result reader)
                        (parse-worksheet)
                        (create-worksheet)
                        (count-repeat-groups)
                        (conj [:state/merge [:worksheet]])
                        (rf/dispatch))]
      (.addEventListener reader "load" on-read)
      (.readAsText reader file))))
