(ns user
  (:require ;[fig-repl :as r]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [datascript.core :as d]
            [datahike.api :as dh]
            [datahike.core :as dc]
            [string-utils.interface :refer [->kebab]]
            [config.interface :refer [load-config get-config]]
            [ds-schema-utils.interface :refer [->ds-schema]]
            [behave.schema.core :refer [all-schemas]]))


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

  #_(b/init!)

  #_(r/start-figwheel!)

  ;; Connect to 1337
  #_(r/start-repl!)

  (require '[clojure.java.io :as io])
  (require '[me.raynes.fs :as fs])

  (load-config "projects/behave/resources/config.edn")

  (def dh-conn (dh/connect (get-config :database :config)))


  (def ws-path "/Users/rsheperd/Code/sig/behave-polylith/worksheets/first-worksheet/")
  (def ws-cfg (assoc-in (get-config :database :config) [:store :path] ws-path))

  (io/make-parents "/Users/rsheperd/Code/sig/behave-polylith/worksheets/first-worksheet/.do_not_delete")
  (fs/ns-path )
  (dh/delete-database ws-cfg)
  (dh/create-database ws-cfg)

  (def ws-conn (dh/connect ws-cfg))
  (filter #(= :variable/maximum (:db/ident %)) all-schemas)
  (filter #(= :variable/metric-decimals (:db/ident %)) all-schemas)
  (dh/transact ws-conn all-schemas)

  (dh/transact ws-conn [{:worksheet/name "UUID Worksheet"}])
  (dc/datoms @ws-conn :eavt)

  (dh/q '[:find [?w] :where [?w :worksheet/name]] @ws-conn)

  (dh/transact ws-conn [{:db/id 104 :worksheet/notes [{:note/content "This is a note."}]}])
  (dh/transact ws-conn [{:db/id 104 :worksheet/notes [{:note/content "A new note that is much much longer."}]}])

  (dh/q '[:find [?note ...]
         :where [?w :worksheet/name]
                [?w :worksheet/notes ?n]
                [?n :note/content ?note]] @ws-conn)

  (dh/transact ws-conn [{:db/id 104
                         :worksheet/result-table {:result-table/header [{}]}}])

  (dh/q '[:find [?note ...]
         :where [?w :worksheet/name]
                [?w :worksheet/notes ?n]
                [?n :note/content ?note]] @ws-conn)

  ;; Export SQL to datoms

  (db/pg-db (select-keys (get-config :database) [:dbname :host :port :user :password]))
  (db/exec-one! {:select [:*] :from :users})

  (def rows (str/split-lines (slurp "projects/behave_cms/src/csv/cont_vars.csv")))

  (def header (->> (str/split (first rows) #",") (mapv ->kebab) (map #(keyword (str "variable/" %)))))

  (def digits (->> #{0 1 2 3 4 5 6 7 8 9} (map char) (set)))

  (defn num-str? [s]
    (number? (edn/read-string s)))

  (defn remove-quotes [s]
    (when-not (empty? s)
      (str/replace s #"\"" "")))

  (defn ->num [s]
    (if (and (string? s) (num-str? s))
      (edn/read-string s)
      s))

  (defn convert-key [m k f]
    (when (get m k)
      (assoc m k (f (get m k)))))

  (def cont-vars (mapv (fn [row]
                         (let [cells (->> (str/split row #",") (map (comp ->num remove-quotes)))
                               m     (apply hash-map (interleave header cells))
                               clean (into {} (filter (fn [[_ v]] (some? v))) m)]
                           (-> clean
                               (convert-key :variable/maximum float)
                               (convert-key :variable/minimum float)
                               (convert-key :variable/default-value float)))) (rest rows)))

  (map keyword (filter (comp not empty?) (set (map :variable/english-units cont-vars))))
  (map keyword (filter (comp not empty?) (set (map :variable/metric-units cont-vars))))

  (map #(-> % :variable/metric-decimals type) cont-vars)

  cont-vars

  (dh/transact ws-conn cont-vars)


  (export-table :variable :variables)



  )
