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
   [me.raynes.fs :as fs]
   [behave.schema.core :refer [all-schemas]]))

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

  ;;; Unit-less
  ;; Add Dimensionless Units
  (d/transact (unwrap ds/conn) [{:bp/uuid (s-uuid)
                                 :dimension/name "Dimensionless"
                                 :dimension/members
                                 [{:bp/uuid (s-uuid)
                                   :unit/name "Ratio"
                                   :unit/short-code "ratio"}
                                  {:bp/uuid (s-uuid)
                                   :unit/name "Number"
                                   :unit/short-code "#"}
                                  {:bp/uuid (s-uuid)
                                   :unit/name "Scale"
                                   :unit/short-code ":"}]}])

  (d/transact (unwrap ds/conn)
              [{:db/id (get-in (find-unit "ratio") [:dimension/_units :db/id])
                :dimension/units
                [{:bp/uuid         (s-uuid)
                  :unit/name       "Number"
                  :unit/short-code "#"}
                 {:bp/uuid         (s-uuid)
                  :unit/name       "Scale"
                  :unit/short-code ":"}]}])

  ;; Add Compass Direction
  (d/transact (unwrap ds/conn) [{:bp/uuid        (s-uuid)
                                 :dimension/name "Direction"
                                 :dimension/units
                                 [{:unit/name       "Degrees"
                                   :unit/short-code "degN"}]}])

  ;; Add Map Coordinate
  (d/transact (unwrap ds/conn) [{:bp/uuid (s-uuid)
                                 :dimension/name "Coordinates"
                                 :dimension/units
                                 [{:unit/name "Decimal Degrees"
                                   :unit/short-code "coord-deg"}
                                  {:unit/name "Degrees/Minutes/Seconds"
                                   :unit/short-code "coord-dms"}]}])

  )


;;; Add Dimension/Units to Continuous Variables
(comment

  ;; Migrate new schema
  #_(ds/migrate (unwrap ds/conn) all-schemas)

  ;; Find all Continuous Variables
  (def all-continuous-variables
    (d/q '[:find [(pull ?v [*]) ...]
           :where
           [?v :variable/kind :continuous]]
         (safe-deref ds/conn)))

  (defn find-unit
    [short-code]
    (d/q '[:find (pull ?u [* {:dimension/_units [*]}]) .
           :in $ ?short-code ?system
           :where
           [?u :unit/short-code ?short-code]]
         (safe-deref ds/conn) short-code))

  (defn add-dimension-unit
    [variable]
    (let [english-unit (find-unit (:variable/english-units variable))
          metric-unit  (find-unit (:variable/metric-units variable))
          native-unit  (find-unit (:variable/native-units variable))
          dim-uuids    (set (map #(get-in % [:dimension/_units :bp/uuid]) [english-unit metric-unit native-unit]))]

      (if (= 1 (count dim-uuids))
        (cond->
            (select-keys variable [:db/id
                                   :variable/name
                                   :variable/english-units
                                   :variable/native-units
                                   :variable/metric-units])

          :always
          (assoc :variable/dimension-uuid (first dim-uuids))

          (map? english-unit)
          (assoc :english-unit (:unit/name english-unit)
                 :variable/english-unit-uuid (:bp/uuid english-unit))

          (map? metric-unit)
          (assoc :metric-unit (:unit/name metric-unit)
                 :variable/metric-unit-uuid (:bp/uuid metric-unit))

          (map? native-unit)
          (assoc :native-unit (:unit/name native-unit)
                 :variable/native-unit-uuid (:bp/uuid native-unit)))
        {:error        "Mismatched Dimensions"
         :dim-uuids    dim-uuids
         :variable     variable})))

  (def add-dimension-unit-tx (mapv add-dimension-unit all-continuous-variables))

  (pprint all-continuous-variables (clojure.java.io/writer "cont-vars.edn"))

  (pprint (filter #(:error %) add-dimension-unit-tx) (clojure.java.io/writer "errors.edn"))

  (filter #(nil? (:variable/dimension-uuid %)) add-dimension-unit-tx)

  (pprint add-dimension-unit-tx (clojure.java.io/writer "tx.edn"))

  ;; Fix Bole Char Height
  (def bole-char (first (filter #(= (:variable/name %) "Bole Char Height") all-continuous-variables)))

  (d/transact (unwrap ds/conn)
              [(assoc (select-keys bole-char [:db/id])
                      :variable/metric-units "m"
                      :variable/english-units "ft"
                      :variable/native-units "ft")])

  ;; Fix Direction
  (def direction-ids (map :db/id (filter (fn [{v-name :variable/name}]
                                           (and (string? v-name)
                                                (re-find #"Direction" v-name))) all-continuous-variables)))

  (d/transact (unwrap ds/conn)
              (map (fn [id] {:db/id id
                             :variable/english-units "degN"
                             :variable/metric-units "degN"
                             :variable/native-units "degN"})
                   direction-ids))

  ;; Fix Latitude/Longitude
  (def coordinate-ids (map :db/id (filter (fn [{v-name :variable/name}]
                                           (and (string? v-name)
                                                (or (= "Latitude" v-name) (= "Longitude" v-name)))) all-continuous-variables)))

  (d/transact (unwrap ds/conn)
              (map (fn [id] {:db/id id
                             :variable/english-units "coord-deg"
                             :variable/metric-units "coord-deg"
                             :variable/native-units "coord-deg"})
                   coordinate-ids))

  ;; Fix Load Units
  (def vars-w-load-units-ids (map :db/id (filter #(= (:variable/native-units %) "lb/ft2") all-continuous-variables)))

  (d/transact (unwrap ds/conn)
              (map (fn [id] {:db/id id
                             :variable/native-units "ton/ac"})
                   vars-w-load-units-ids))


  ;; Fix Heat Sink
  (def heat-sink-ids (map :db/id (filter #(= (:variable/name %) "Heat Sink") all-continuous-variables)))

  (d/transact (unwrap ds/conn)
              (mapv (fn [id] {:db/id id
                             :variable/metric-units "kJ/m3"})
                   heat-sink-ids))




  )
