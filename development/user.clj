(ns user
  (:require ;[fig-repl :as r]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.data.xml :as xml]
            [clojure.set :as set]
            [clojure.string :as str]
            [datascript.core :as d]
            [datahike.api :as dh]
            [datahike.core :as dc]
            [string-utils.interface :refer [->kebab]]
            [config.interface :refer [load-config get-config]]
            [ds-schema-utils.interface :refer [->ds-schema]]
            [datom-utils.interface :as du]
            [datom-compressor.interface :as c]
            [behave.schema.core :refer [all-schemas]]))

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

(defn- indexed-by [f coll]
  (->> coll
       (map f)
       (set)
       (map-indexed (fn [i attr] [attr i]))
       (into {})))

(comment
  (+ 1 1)

  (def conn (d/create-conn (->ds-schema all-schemas)))

  (d/transact conn [{:worksheet/name "Test Worksheet"}])

  (d/q '[:find [?w] :where [?w :worksheet/name]] @conn)

  (d/transact conn [{:db/id 1 :worksheet/notes [{:note/content "This is a note."}]}])
  (d/transact conn [{:db/id 1 :worksheet/notes [{:note/content "A new note that is much much longer."}]}])

  (d/q '[:find [?note ...]
         :where [?w :worksheet/name]
                [?w :worksheet/notes ?n]
                [?n :note/content ?note]] @conn)

  (d/transact conn )

  (d/transact conn [{:db/id 1 :worksheet/inputs [{:input/value 15 :input/units "m"}]}])

  (d/transact conn [{:db/id 1 :worksheet/inputs [{:input/value 15 :input/units "m"}]}])

  (d/q '[:find ?v ?units :where [?e :input/value ?v][?e :input/units ?units]] @conn)

  (d/q '[:find ?v ?units :where [?e :input/value ?v][?e :input/units ?units]] @conn)

  (d/datoms @conn :eavt)

  (require '[fig-repl :as r])

  (r/start-figwheel!)

  ;; Connect to 1337
  (r/start-repl!)

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

  (def out-file "projects/behave/resources/public/layout.msgpack")
  (def os (FileOutputStream. (io/file out-file)))

  (def packed-in-str (c/pack (filter #(du/safe-attr? unsafe-attrs %) (dc/datoms @dh-conn :eavt))))


  (type packed-in-str)

  (.write os packed-in-str)
  (spit  packed-in-str)
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


  (export-table :module :modules)



  )
