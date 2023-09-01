(ns add-units
  (:require
   [clojure.edn :refer [read-string]]
   [clojure.pprint :refer [pprint]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.set :refer [rename-keys]]
   [datom-store.main :as ds]
   [datahike.api :as d]
   [datahike.core :as dc]
   [datascript.core :refer [squuid]]
   [map-utils.interface :refer [index-by]]
   [datom-utils.interface :refer [safe-deref unwrap]]
   [me.raynes.fs :as fs]))

(defn s-uuid []
  (str (squuid)))

(defn all-enums 
  "Find all enums with their members."
  []
  (d/q '[:find [(pull ?enum [* {:cpp.enum/enum-member [*]}]) ...]
         :where
         [?enum :cpp.enum/name ?enum-name]]
       (safe-deref ds/conn)))

(defn units-enums
  "Finds all enums related to units."
  []
  (filter #(str/ends-with? (:cpp.enum/name %) "UnitsEnum") (all-enums)))

(all-enums)

;;; Remove old *UnitsEnum enums
(comment
  ;; Check
  (def remove-units-enums (units-enums))

  ;; TX
  (def enums-and-members-ids (flatten (map (fn [u] (into [(:db/id u)] (map :db/id (:cpp.enum/enum-member u)))) remove-units-enums)))
  (def remove-tx (mapv (fn [id] [:db/retractEntity id]) enums-and-members-ids))

  ;; Remove
  (d/transact (unwrap ds/conn) remove-tx)
  )

;;; Add back *UnitsEnum enums
(comment
  ;; Read from file
  (def new-units-enums (read-string (slurp (io/file "cms-exports/unit-enums.edn"))))

  ;; Create tx
  (def new-units-enums-tx (mapv (fn [[enum-name enum-members]]
                                  {:bp/uuid       (s-uuid)
                                   :cpp.enum/name (name enum-name)
                                   :cpp.enum/enum-member
                                   (into [] (map-indexed (fn [idx enum-member]
                                                           {:bp/uuid               (s-uuid)
                                                            :cpp.enum-member/name  (name enum-member)
                                                            :cpp.enum-member/value idx}) enum-members))}) new-units-enums))

  ;; Add tx
  (d/transact (unwrap ds/conn) new-units-enums-tx)
  )

;;; Add Dimension/Units with Enum/Enum-Member mappings
(comment
  ;; Read dimensions file
  (def dimensions (read-string (slurp (io/file "cms-exports/dimensions.edn"))))

  ;; Create TX
  (defn ->dimension [{:keys [enum-name] :as dimension}]
    (let [enums     (index-by :cpp.enum/name (units-enums))
          enum      (get enums enum-name)
          _         (println enum-name enum)
          enum-uuid (:bp/uuid enum)
          members   (index-by :cpp.enum-member/name (:cpp.enum/enum-member enum))
          units     (mapv #(-> %
                               (dissoc :unit/enum-member-name)
                               (assoc 
                                :bp/uuid (s-uuid)
                                :unit/cpp-enum-member-uuid
                                (get-in members [(:unit/enum-member-name %) :bp/uuid])))
                          (:dimension/units dimension))]
      (-> dimension 
          (dissoc :enum-name)
          (assoc
           :bp/uuid (s-uuid)
           :dimension/cpp-enum-uuid enum-uuid
           :dimension/units units))))

  (def dimensions-tx (mapv ->dimension dimensions))

  ;; Transact
  (d/transact (unwrap ds/conn) dimensions-tx)
  )
