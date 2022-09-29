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
                               '[{:group/_group-variables [{:submodule/_groups [:submodule/io]}]}]
                               gv])]

    {:io       (get-in result [:group/_group-variables 0 :submodule/_groups 0 :submodule/io])
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
                  values   (str/split #"," (get v accessor))]
              (map-indexed [idx val]
                [[:inputs (:group-id gv-info) idx gv-id] val]))))))))

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

(def previous-file (atom nil))

(defn import-worksheet [file]
  (when file
    (let [reader (js/FileReader.)
          on-read     #(->> (.-result reader)
                            (reset! previous-file)
                            (parse-worksheet)
                            (create-worksheet)
                            (conj [:state/merge [:worksheet]])
                            (rf/dispatch))]
      (.addEventListener reader "load" on-read)
      (.readAsText reader file))))


(comment

  @previous-file

  (def run-1-data (parse-worksheet @previous-file))

  run-1-data
  (let [[properties variables] (partition-by :tag run-1-data)
        inputs (->> variables (map bp6-input->worksheet-input) (filter some?) (flatten))
        outputs (->> properties (filter output-var?) (bp6-output->worksheet-output) (filter some?))]
    (reduce (fn [acc [path value]] (assoc-in acc path value)) {} (concat inputs outputs)))

  (map :name (:outputs *1))

  (->> run-1-data
       (filter output-var?))
       (map (fn [{:keys [name value]}]
              (let [[module var-name] (str/split name #"Calc")]
                {:name (str "v" (str/capitalize module) var-name)
                 :value value
                 :io :output}))))

  (def test-name (-> *1 (first) :name))


  )
