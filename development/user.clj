(ns user
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.data.xml :as xml]
            [clojure.set :as set]
            [clojure.string :as str]
            [datascript.core :as d]
            [datahike.api :as dh]
            [datahike.core :as dc]
            [string-utils.interface :refer [->kebab ->snake]]
            [config.interface :refer [load-config get-config]]
            [ds-schema-utils.interface :refer [->ds-schema]]
            [datom-utils.interface :as du]
            [datom-compressor.interface :as c]
            [datom-store.main :as ds]
            [ajax.core :refer [GET]]
            [ajax.protocols :as pr]
            [ajax.edn :refer [edn-response-format edn-request-format]]
            [behave.schema.core :refer [all-schemas]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(comment
  (require '[behave.core :as core])
  (core/init!)

  (require '[fig-repl :as r])
  (require '[behave.core :as b])

  (r/start-figwheel!)

  ;; Connect to 1337
  (r/start-repl!)


  latest-datoms

  (spit "prod-config.edn" token :append true)

  (defn num-str? [s]
    (number? (edn/read-string s)))

  (defn remove-quotes [s]
    (when-not (empty? s)
      (str/replace s #"\"" "")))

  (defn remove-leading-zeros [s]
    (if (str/starts-with? s "0")
      (recur (str/replace-first s #"^0" ""))
      s))

  (defn ->num [s]
    (if (and (string? s) (num-str? s))
      (edn/read-string s)
      s))

  (defn convert-key [m k f]
    (when (get m k)
      (assoc m k (f (get m k)))))

  (defn csv->vmap [f]
    (let [rows   (str/split-lines (slurp f))
          header (as-> (first rows) % (str/split % #",") (map (comp keyword remove-quotes) %))]
      (mapv (fn [row] (->> (str/split row #",") (map remove-quotes) (interleave header) (apply hash-map))) (rest rows))))

    (d/q '[:find [?w] :where [?w :worksheet/name]] @conn)

    (d/transact conn [{:db/id 1 :worksheet/notes [{:note/content "This is a note."}]}])
    (d/transact conn [{:db/id 1 :worksheet/notes [{:note/content "A new note that is much much longer."}]}])

    (d/q '[:find [?note ...]
          :where [?w :worksheet/name]
                  [?w :worksheet/notes ?n]
                  [?n :note/content ?note]] @conn)

    (d/transact conn [{:db/id 1 :worksheet/inputs [{:input/value 15 :input/units "m"}]}])

    (d/transact conn [{:db/id 1 :worksheet/inputs [{:input/value 15 :input/units "m"}]}])

    (d/q '[:find ?v ?units :where [?e :input/value ?v][?e :input/units ?units]] @conn)

    (d/q '[:find ?v ?units :where [?e :input/value ?v][?e :input/units ?units]] @conn)

    (d/datoms @conn :eavt)


    (require '[clojure.java.io :as io])
    (require '[me.raynes.fs :as fs])

    (load-config "projects/behave/resources/config.edn")

    (def ws-path "/Users/rsheperd/Code/sig/behave-polylith/worksheets/first-worksheet/")
    (def ws-cfg (assoc-in (get-config :database :config) [:store :path] ws-path))

    (io/make-parents "/Users/rsheperd/Code/sig/behave-polylith/worksheets/first-worksheet/.do_not_delete")
    (dh/delete-database ws-cfg)
    (dh/create-database ws-cfg)

    (def ws-conn (dh/connect ws-cfg))
    (def dh-conn (dh/connect (get-config :database :config)))

    (dh/q '[:find ?e ?name :where [?e :application/name ?name]] @dh-conn)

    (def unsafe-attrs (du/unsafe-attrs all-schemas))
    (import '[java.util Base64]
            '[java.io FileOutputStream])


    (def modules (dh/pull @dh-conn '[{:application/modules [*]}] 78))
    modules
    (map #(select-keys % [:db/id :module/name :module/order]) (:application/modules modules))

    (def surface   [:db/add 66 :module/order 0])
    (def crown     [:db/add 64 :module/order 1])
    (def contain   [:db/add 65 :module/order 2])
    (def mortality [:db/add 67 :module/order 3])

    (def contain 65)

    (dh/pull @dh-conn '[{:module/submodules [*]}] contain)
    (dh/transact dh-conn {:tx-data [{:module/_submodules        contain
                                    :db/id                     83
                                    :submodule/name            "Fire"
                                    :submodule/io              :output
                                    :submodule/order           0
                                    :submodule/translation-key "behaveplus:contain:outputs:fire"
                                    :submodule/help-key        "behaveplus:contain:outputs:fire:help"}]})



    (dh/transact dh-conn {:tx-data [{:db/id 83 :submodule/order 0}]})

    (dh/pull-many @dh-conn '[*] (dh/q '[:find [?e ...] :where [?e :submodule/name "Fire"]] @dh-conn))

    (dh/pull @dh-conn '[* {:submodule/groups [*]}] 83)

    (dh/transact dh-conn {:tx-data
                          [{:group/name "Fire Size"
                            :submodule/_groups 83
                            :group/order 0
                            :group/translation-key "behaveplus:contain:outputs:fire:fire_size"
                            :group/help-key "behaveplus:contain:outputs:fire:fire_size:help"}
                          {:group/name "Containment"
                            :submodule/_groups 83
                            :group/order 1
                            :group/translation-key "behaveplus:contain:outputs:fire:containment"
                            :group/help-key "behaveplus:contain:outputs:fire:containment:help"}
                          {:group/name "Resources"
                            :submodule/_groups 83
                            :group/order 2
                            :group/translation-key "behaveplus:contain:outputs:fire:resources"
                            :group/help-key "behaveplus:contain:outputs:fire:resources:help"}]})


    (dh/transact dh-conn {:tx-data [{:module/_submodules        contain
                                    :db/id                     84
                                    :submodule/name            "Fire"
                                    :submodule/io              :input
                                    :submodule/order           0
                                    :submodule/translation-key "behaveplus:contain:input:fire"
                                    :submodule/help-key        "behaveplus:contain:input:fire:help"}]})

    (dh/pull @dh-conn '[* {:submodule/groups [*]}] 84)

    (dh/transact dh-conn {:tx-data
                          [{:group/name "Surface Rate of Spread (maximum)"
                            :submodule/_groups 84
                            :group/order 0
                            :group/translation-key "behaveplus:contain:input:fire:surface_rate_of_spread"
                            :group/help-key "behaveplus:contain:input:fire:surface_rate_of_spread:help"}
                          {:group/name "Fire Size at Report"
                            :submodule/_groups 84
                            :group/order 1
                            :group/translation-key "behaveplus:contain:input:fire:fire_size_at_report"
                            :group/help-key "behaveplus:contain:input:fire:fire_size_at_report:help"}
                          {:group/name "Length-to-Width Ratio"
                            :submodule/_groups 84
                            :group/order 2
                            :group/translation-key "behaveplus:contain:input:fire:length_to_width_ratio"
                            :group/help-key "behaveplus:contain:input:fire:length_to_width_ratio:help"}]})

    (dh/transact dh-conn {:tx-data [{:module/_submodules        contain
                                    :db/id                     85
                                    :submodule/name            "Suppression"
                                    :submodule/io              :input
                                    :submodule/order           1
                                    :submodule/translation-key "behaveplus:contain:input:suppression"
                                    :submodule/help-key        "behaveplus:contain:input:suppression:help"}]})

    (dh/pull @dh-conn '[* {:submodule/groups [*]}] 85)

    (dh/transact dh-conn {:tx-data
                          [{:group/name "Suppression Tactic"
                            :submodule/_groups 85
                            :group/order 0
                            :group/translation-key "behaveplus:contain:input:suppression:suppression_tactic"
                            :group/help-key "behaveplus:contain:input:suppression:suppression_tactic:help"}
                          {:group/name "Line Construction Offset"
                            :submodule/_groups 85
                            :group/order 1
                            :group/translation-key "behaveplus:contain:input:suppression:line_construction_offset"
                            :group/help-key "behaveplus:contain:input:suppression:line_construction_offset:help"}
                          {:group/name "Resources"
                            :submodule/_groups 85
                            :group/order 2
                            :group/translation-key "behaveplus:contain:input:suppression:resources"
                            :group/help-key "behaveplus:contain:input:suppression:resources:help"}]})

    (dh/pull @dh-conn '[* {:group/variables [*]}] 90)

  (dh/transact dh-conn {:tx-data [{:db/ident       :group/variables
                                  :db/doc         "Group's variables."
                                  :db/valueType   :db.type/ref
                                  :db/cardinality :db.cardinality/many}]})

    (dh/transact dh-conn {:tx-data
                          [{:variable/name "Fire Area - at Initial Attack"
                            :group/_variables 90
                            :variable/order 0
                            :variable/translation-key "behaveplus:variable:fire_area_at_initial_attack"
                            :variable/help-key "behaveplus:variable:fire_area_at_initial_attack:help"}
                          {:variable/name "Fire Perimeter - at Initial Attack"
                            :group/_variables 90
                            :variable/order 1
                            :variable/translation-key "behaveplus:variable:fire_perimeter_at_initial_attack"
                            :variable/help-key "behaveplus:variable:fire_perimeter_at_initial_attack:help"}]})

    (dh/pull @dh-conn '[* {:group/variables [*]}] 91)

    (->snake "Derp Herp")

    (dh/pull @dh-conn '[* {:group/variables [*]}] 91)

    (defn add-output-var-tx [group-id var-name order]
      (let [{t-key :group/translation-key} (dh/pull @dh-conn '[*] group-id)
            var-t-key (str t-key ":" (->snake var-name))
            var-h-key (str var-t-key ":help")]
        {:variable/name var-name
        :group/_variables group-id
        :variable/order order
        :variable/translation-key var-t-key
        :variable/help-key var-h-key}))

    (dh/transact dh-conn {:tx-data [(add-output-var-tx 91 "Contain Status" 0)
                                    (add-output-var-tx 91 "Time from Report" 1)
                                    (add-output-var-tx 91 "Contained Area" 1)
                                    (add-output-var-tx 91 "Fireline Constructed" 2)
                                    (add-output-var-tx 91 "Containment Diagram" 3)]})

    (dh/pull @dh-conn '[* {:group/variables [*]}] 92)

    (dh/transact dh-conn {:tx-data [(add-output-var-tx 92 "Number of Resources Used" 0)]})




    (dh/transact dh-conn {:tx-data
                          [{:variable/name "Fire Area - at Initial Attack"
                            :group/_variables 90
                            :variable/order 0
                            :variable/translation-key "behaveplus:variable:fire_area_at_initial_attack"
                            :variable/help-key "behaveplus:variable:fire_area_at_initial_attack:help"}
                          {:variable/name "Fire Perimeter - at Initial Attack"
                            :group/_variables 90
                            :variable/order 1
                            :variable/translation-key "behaveplus:variable:fire_perimeter_at_initial_attack"
                            :variable/help-key "behaveplus:variable:fire_perimeter_at_initial_attack:help"}]})

    (defn add-input-var-tx [group-id var-name kind order]
      (let [{t-key :group/translation-key} (dh/pull @dh-conn '[*] group-id)
            var-t-key (str t-key ":" (->snake var-name))
            var-h-key (str var-t-key ":help")]
        {:variable/name var-name
        :group/_variables group-id
        :variable/kind kind
        :variable/order order
        :variable/translation-key var-t-key
        :variable/help-key var-h-key}))

    (dh/pull @dh-conn '[* {:group/variables [*]}] 93)
    (dh/transact dh-conn {:tx-data [(add-input-var-tx 93 "Surface Rate of Spread" :continuous 0)]})

    (dh/pull @dh-conn '[* {:group/variables [*]}] 94)
    (dh/transact dh-conn {:tx-data [(add-input-var-tx 94 "Fire Size at Report" :continuous 0)]})

    (dh/pull @dh-conn '[* {:group/variables [*]}] 95)
    (dh/transact dh-conn {:tx-data [(add-input-var-tx 95 "Length-to-Width Ratio" :continuous 0)]})

    (dh/pull @dh-conn '[* {:group/variables [*]}] (first (dh/q '[:find [?e] :where [?e :group/name "Suppression Tactic"]] @dh-conn)))

    (dh/pull @dh-conn '[* {:group/variables [*]}] 96)
    (dh/transact dh-conn {:tx-data [(add-input-var-tx 96 "Supression Tactic" :discrete 0)]})

    (dh/pull @dh-conn '[* {:group/variables [*]}] 97)
    (dh/transact dh-conn {:tx-data [(add-input-var-tx 97 "Line Construction Offset" :continuous 0)]})

    (dh/pull @dh-conn '[* {:group/variables [*]}] 98)

    (dh/transact dh-conn {:tx-data [(add-input-var-tx 98 "Resource Name" :text 0)
                                    (add-input-var-tx 98 "Resource Arrival Time" :continuous 1)
                                    (add-input-var-tx 98 "Resource Line Production Rate" :continuous 2)
                                    (add-input-var-tx 98 "Resource Duration" :continuous 3)]})




    (dh/pull-many @dh-conn '[*] (dh/q '[:find [?e ...] :where [?e :module/name ?name]] @dh-conn))
    (dh/transact dh-conn {:tx-data [[:db.fn/retractEntity 89]]})
    (dh/transact dh-conn {:tx-data [[:db.fn/retractAttribute 25 :module/name]]})

    (dh/transact dh-conn {:tx-data [surface crown contain mortality]})

    (dh/transact dh-conn {:tx-data [[:db/add contain :module/submodules 83]]})


    (dh/transact dh-conn {:tx-data [{:db/ident       :group/repeat?
                                    :db/doc         "Whether a Group repeats."
                                    :db/valueType   :db.type/boolean
                                    :db/cardinality :db.cardinality/one}
                                    {:db/ident       :group/max-repeat
                                    :db/doc         "Group's maximum number of repeats."
                                    :db/valueType   :db.type/long
                                    :db/cardinality :db.cardinality/one}]})


    (dh/transact dh-conn {:tx-data [{:db/id 98
                                    :group/repeat? true
                                    :group/max-repeat 5}]})


    (def out-file "projects/behave/resources/public/layout.msgpack")
    (def os (FileOutputStream. (io/file out-file)))

    (def packed-in-str (c/pack (filter #(du/safe-attr? unsafe-attrs %) (dc/datoms @dh-conn :eavt))))


    (type packed-in-str)

    (.write os packed-in-str)

    (filter #(= :variable/maximum (:db/ident %)) all-schemas)
    (filter #(= :variable/metric-decimals (:db/ident %)) all-schemas)
    (filter #(= :list/options (:db/ident %)) all-schemas)
    (filter #(= :list-option/translation-key (:db/ident %)) all-schemas)
    (filter #(= :list-option/index (:db/ident %)) all-schemas)
    (dh/transact ws-conn all-schemas)


    ;; Export SQL to datoms

    (db/pg-db (select-keys (get-config :database) [:dbname :host :port :user :password]))
    (db/exec-one! {:select [:*] :from :users})


    (def dh-cont-vars (dh/q '[:find [?v ...] :where [?v :variable/kind :continuous]] @ws-conn))
    (dh/pull-many @ws-conn '[*] dh-cont-vars)

    ;;; Lists

    (defn ->list-option [i {:keys [attrs]}]
      (-> attrs
          (select-keys [:name :default :index])
          (convert-key :index ->num)
          (assoc :default (= "true" (:default attrs)))
          (assoc :sort i)
          (set/rename-keys {:name    :list-option/name
                            :sort    :list-option/order
                            :default :list-option/default
                            :index   :list-option/index})))

    (def lists (->> (xml/parse-str (slurp "projects/behave_cms/src/xml/lists.xml"))
                    (:content)
                    (filter #(= :itemList (:tag %)))
                    (map (fn [l]
                                  {:list/name    (-> l :attrs :name)
                                    :db/id        (str (dc/squuid))
                                    :list/options (vec (map-indexed ->list-option (:content l)))}))))

    (def list-ids (into {} (map (fn [m] [(:list/name m) (:db/id m)]) lists)))

    lists
    list-ids


    ;;;


    (def v-translations (atom {}))
    (def items (->> (slurp "projects/behave_cms/src/xml/variables.xml")
                        (xml/parse-str)
                        (:content)))

    (doseq [{:keys [tag attrs]} items]
      (when (= tag :translate)
        (let [[variable k] (-> (:key attrs) (str/split #"\:" 2))]
          (swap! v-translations assoc-in [variable (str/lower-case k)]
                 {:en-US (:en_US attrs) :pt-PT (:pt_PT attrs)}))))

    (first @v-translations)

    (defn i18n [label k default]
      (let [res (get label k default)]
        (if (empty? res)
          default
          res)))

    (def var-labels (map (fn [[k {:strs [label]}]]
                           (str/join "\t" [k
                                           (i18n label :en-US (str "(en-US) " k))
                                           (i18n label :pt-PT (str "(pt-PT) " k))])) @v-translations))

    (spit "projects/behave_cms/src/csv/var-labels.tsv" (str/join "\n" (concat ["variable\ten_us\tpt_pt"] (sort var-labels))))

    ;;; Discrete Vars

    (def disc-vars (->> (csv->vmap "projects/behave_cms/src/csv/disc_vars.csv")
                        (mapv (fn [m]
                                (-> m
                                    (assoc :variable/kind :discrete)
                                    (assoc :variable/list (get list-ids (:list m)))
                                    (dissoc :list)
                                    (set/rename-keys {:name :variable/name}))))))

    disc-vars

    ;;; Text Vars

    (def text-vars (->> (csv->vmap "projects/behave_cms/src/csv/text_vars.csv")
                        (mapv (fn [m] (assoc m :variable/kind :text)))))

    ;;; Continuous Variables

    (def cont-rows (str/split-lines (slurp "projects/behave_cms/src/csv/cont_vars.csv")))

    (def cont-header (->> (str/split (first cont-rows) #",") (mapv ->kebab) (map #(keyword (str "variable/" %)))))

    (def cont-vars (mapv (fn [row]
                          (let [cells (->> (str/split row #",") (map (comp ->num remove-quotes)))
                                m     (apply hash-map (interleave cont-header cells))
                                clean (into {} (filter (fn [[_ v]] (some? v))) m)]
                            (-> clean
                                (assoc :variable/kind :continuous)
                                (convert-key :variable/maximum float)
                                (convert-key :variable/minimum float)
                                (convert-key :variable/default-value float)))) (rest cont-rows)))

    (concat lists disc-vars text-vars cont-vars)

    (dh/transact ws-conn (concat lists disc-vars text-vars cont-vars))
    (d/transact conn (concat lists disc-vars text-vars cont-vars))

    (def dh-disc-vars (dh/q '[:find [?v ...] :where [?v :variable/kind :discrete]] @ws-conn))

    dh-disc-vars

    (dh/pull-many @ws-conn '[* {:variable/list [*]}] dh-disc-vars)

    (dh/datoms @ws-conn :eavt)

    (def contain-vars (filter #(str/starts-with? (second %) "Contain") (dh/q '[:find ?e ?name :where [?e :variable/name ?name]] @ws-conn)))

    ;;; Worksheet

    (d/q '[:find ?e ?name :where [?e :variable/name ?name]] @conn)

    (dh/transact ws-conn [{:worksheet/name "UUID Worksheet"}])

    (def ws-id (first (dh/q '[:find [?w] :where [?w :worksheet/name]] @ws-conn)))

    ;; Notes

    (dh/transact ws-conn [{:db/id ws-id :worksheet/notes [{:note/content "This is a note."}]}])
    (dh/transact ws-conn [{:db/id ws-id :worksheet/notes [{:note/content "A new note that is much much longer."}]}])
    (dh/q '[:find [?note ...]
          :where [?w :worksheet/name]
                  [?w :worksheet/notes ?n]
                  [?n :note/content ?note]] @ws-conn)

    ;; Inputs

    (def ws-input-vars (->> contain-vars
                            (take 4)
                            (map first)
                            (dh/pull-many @ws-conn '[*])))


    (defn kind->input-value [kind]
      (get {:continuous :input/continuous-value
            :discrete   :input/discrete-value
            :text       :input/text-value} kind))

    (defn add-ws-input [conn ws-id variable value units]
      (let [kind (:variable/kind variable)
            data {:worksheet/_inputs ws-id
                  :input/variable (:db/id variable)
                  :input/units    units
                  :input/kind     (:variable/kind variable)}]

        (dh/transact conn (vector (assoc data (kind->input-value kind) value)))))

    (defn add-ws-output [conn ws-id {var-id :db/id}]
      (dh/transact conn [{:worksheet/_outputs ws-id
                          :output/variable var-id}]))

    (defn add-ws-result [conn ws-id {var-id :db/id}]
      (dh/transact conn [{:worksheet/_outputs ws-id
                          :output/variable var-id}]))

    (dh/pull @ws-conn '[* {:worksheet/inputs [* {:input/variable [*]}]}] 890)

    (add-ws-input ws-conn ws-id (first ws-input-vars) 5.0 "resources")
    (add-ws-input ws-conn ws-id (second ws-input-vars) 500.0 "ac")
    (dh/q '[:find ?w ?name ?i ?value
          :where [?w :worksheet/name ?name]
                  [?w :worksheet/inputs ?i]
                  [?i :input/continuous-value ?value]] @ws-conn)

    (add-ws-output ws-conn ws-id (last ws-input-vars))
    (dh/q '[:find ?w ?name ?o ?vname
          :where [?w :worksheet/name ?name]
                  [?w :worksheet/outputs ?o]
                  [?o :output/variable ?v]
                  [?v :variable/name ?vname]] @ws-conn)

    (add-ws-output ws-conn ws-id (last ws-input-vars))
    (dh/q '[:find ?w ?name ?o ?vname
          :where [?w :worksheet/name ?name]
                  [?w :worksheet/outputs ?o]
                  [?o :output/variable ?v]
                  [?v :variable/name ?vname]] @ws-conn)

    (dh/pull @ws-conn '[* {:worksheet/outputs [* {:output/variable [*]}]}] 890)

    (dh/transact ws-conn [{:worksheet/_result-table ws-id
                            :result-table/header [{:db/id -1
                                                  :result-header/variable (:db/id (last ws-input-vars))
                                                  :result-header/units "ch"}]
                            :result-table/row [{:result-cell/header -1
                                                :result-cell/continuous-value 1000.0}]}])

    (dh/pull @ws-conn '[{:worksheet/result-table [* {:result-table/header [:result-header/units {:result-header/variable [*]}] :result-table/row [:result-cell/continuous-value]}]}] 890)

    (dh/pull @ws-conn '[{:worksheet/result-table [* {:result-table/header [:result-header/units {:result-header/variable [*]}] :result-table/row [:result-cell/continuous-value]}]}] 890)


    )
