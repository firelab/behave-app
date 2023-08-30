(ns add-units
  (:require
   [clojure.edn :refer [read-string]]
   [clojure.java.io :as io]
   [clojure.set :refer [rename-keys]]
   [datom-store.main :as ds]
   [datahike.api :as d]
   [datahike.core :as dc]
   [datascript.core :refer [squuid]]
   [map-utils.interface :refer [index-by]]
   [datom-utils.interface :refer [safe-deref unwrap]]
   [me.raynes.fs :as fs]))

(comment

  (def dimensions (read-string (slurp (io/file "cms-exports/dimensions.edn"))))

  (def all-enums
    (index-by :cpp.enum/name
              (d/q '[:find ?enum ?enum-name ?uuid
                     :keys db/id cpp.enum/name bp/uuid
                     :where
                     [?enum :cpp.enum/name ?enum-name]
                     [?enum :bp/uuid ?uuid]]
                   (safe-deref ds/conn))))

  (defn enum-members [id]
    (d/q '[:find [(pull ?member [*]) ...]
           :in $ ?enum
           :where
           [?enum :cpp.enum/enum-member ?member]]
         (safe-deref ds/conn) id))

  (first all-enums)
  (map all-enums [:bp/uuid])

  (def tx (mapv (fn [{:keys [enum-name] :as dimension}]
                  (let [enum      (get all-enums enum-name)
                        enum-uuid (:bp/uuid enum)
                        members   (index-by :cpp.enum-member/name (enum-members (:db/id enum)))
                        units     (mapv #(assoc %
                                                :bp/uuid (squuid)
                                                :unit/enum-member-uuid
                                                (get-in members [(:enum-member-name %) :bp/uuid])) (:dimension/units dimension))]
                    (-> dimension 
                        (assoc
                         :bp/uuid (squuid)
                         :dimension/cpp-enum-uuid enum-uuid
                         :dimension/units units)))) dimensions))

  #_(d/transact (unwrap ds/conn) tx)

  )
