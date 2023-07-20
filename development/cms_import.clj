(ns cms-import
  (:require
   [clojure.edn :refer [read-string]]
   [clojure.java.io :as io]
   [clojure.set :refer [rename-keys]]
   [datom-store.main :as ds]
   [datahike.api :as d]
   [datahike.core :as dc]
   [datascript.core :refer [squuid]]
   [string-utils.interface :refer [->str]]
   [datom-utils.interface :refer [safe-deref unwrap]]
   [me.raynes.fs :as fs]))

(defn dissoc-in [m keys]
  (update-in m (butlast keys) dissoc (last keys)))

(defn write-pprint-edn [m f]
  (io/make-parents f)
  (pr-str m)
  (with-open [out-file (clojure.java.io/writer f)]
    (clojure.pprint/pprint m out-file)))

(defn cms-import [{:keys [behave-file
                          sig-adapter-file
                          out-file-name
                          from-key
                          to-key]}]
  (let [behave-edn (read-string (slurp (fs/expand-home behave-file)))
        sig-edn    (read-string (slurp (fs/expand-home sig-adapter-file)))
        merged-edn (-> behave-edn
                       (dissoc-in [:global from-key from-key])
                       (update-in [:global] rename-keys {from-key to-key})
                       (update-in [:global to-key] merge (get-in sig-edn [:global to-key])))]

    (write-pprint-edn merged-edn (str "cms-exports/" out-file-name))))

(defn ->class [[class-name methods]]
  (let [->param   (fn [i p] (merge {:cpp.parameter/order i
                                    :bp/uuid             (str (squuid))}
                                   (rename-keys p {:id   :cpp.parameter/name
                                                   :type :cpp.parameter/type})))
        ->fn      (fn [[_ {:keys [type id parameters]}]]
                    (merge {:bp/uuid                (str (squuid))
                            :cpp.function/name      id
                            :cpp.function/parameter (vec (map-indexed ->param parameters))}
                           (when type {:cpp.function/return-type type})))
        has-name? (comp some? :cpp.function/name)]
    {:bp/uuid            (str (squuid))
     :cpp.class/name     (->str class-name)
     :cpp.class/function (vec (filter has-name? (map ->fn methods)))}))

(defn lookup-ns-id [ns-name conn]
  (d/q '[:find ?e .
         :in $ ?name
         :where [?e :cpp.namespace/name ?name]]
       (safe-deref conn) ns-name))

(defn add-export-file-to-conn [f conn]
  (let [source-edn (read-string (slurp (fs/expand-home f)))
        namespaces (reduce (fn [acc ns] (assoc acc ns (lookup-ns-id (->str ns) conn))) {} (keys source-edn))
        tx         (mapv (fn [[ns-key ns-id]]
                           (merge {:cpp.namespace/name  (->str ns-key)
                                   :cpp.namespace/class (mapv ->class (get source-edn ns-key))}
                                  (when ns-id {:db/id ns-id})))
                         namespaces)]
    (d/transact (unwrap conn) tx)))

(comment
  (def surface-edn (read-string (slurp (fs/expand-home "~/Code/sig/hatchet/exports/surface.edn"))))
  (def sig-surface-edn (read-string (slurp (fs/expand-home "~/Code/sig/hatchet/exports/SIGSurface.edn"))))

  (dissoc-in surface-edn [:global :Surface :Surface])

  (def sig-surface (-> surface-edn
                       (dissoc-in [:global :Surface :Surface])
                       (update-in [:global] rename-keys {:Surface :SIGSurface})
                       (update-in [:global :SIGSurface] merge (get-in sig-surface-edn [:global :SIGSurface]))))

  (get-in sig-surface-edn [:global :SIGSurface :SIGSurface])
  (get-in sig-surface-edn [:global :SIGSurface :setMoistureScenarios])

  (spit "cms-exports/SIGSurface.edn" (str sig-surface))
  (write-pprint-edn sig-surface "cms-exports/SIGSurface.edn")

  ;;; Combine exports
  (cms-import {:behave-file      "~/work/code/hatchet/behave-mirror/surface.edn"
               :sig-adapter-file "~/work/code/hatchet/sig-adapters/SIGSurface.edn"
               :out-file-name    "SIGSurface.edn"
               :from-key         :Surface
               :to-key           :SIGSurface})

  (cms-import {:behave-file      "~/work/code/hatchet/behave-mirror/moistureScenarios.edn"
               :sig-adapter-file "~/work/code/hatchet/sig-adapters/SIGMoistureScenarios.edn"
               :out-file-name    "SIGMoistureScenarios.edn"
               :from-key         :MoistureScenarios
               :to-key           :SIGMoistureScenarios})

  (cms-import {:behave-file      "~/work/code/hatchet/behave-mirror/crown.edn"
               :sig-adapter-file "~/work/code/hatchet/sig-adapters/SIGCrown.edn"
               :out-file-name    "SIGCrown.edn"
               :from-key         :Crown
               :to-key           :SIGCrown})

  (cms-import {:behave-file      "~/work/code/hatchet/behave-mirror/behaveRun.edn"
               :sig-adapter-file "~/work/code/hatchet/sig-adapters/SIGBehaveRun.edn"
               :out-file-name    "SIGBehaveRun.edn"
               :from-key         :BehaveRun
               :to-key           :SIGBehaveRun})

  (cms-import {:behave-file      "~/work/code/hatchet/behave-mirror/mortality.edn"
               :sig-adapter-file "~/work/code/hatchet/sig-adapters/SIGMortality.edn"
               :out-file-name    "SIGMortality.edn"
               :from-key         :Mortality
               :to-key           :SIGMortality})

  (cms-import {:behave-file      "~/work/code/hatchet/behave-mirror/spot.edn"
               :sig-adapter-file "~/work/code/hatchet/sig-adapters/SIGSpot.edn"
               :out-file-name    "SIGSpot.edn"
               :from-key         :Spot
               :to-key           :SIGSpot})

  (cms-import {:behave-file      "~/work/code/hatchet/behave-mirror/ContainAdapter.edn"
               :sig-adapter-file "~/work/code/hatchet/sig-adapters/SIGContainAdapter.edn"
               :out-file-name    "SIGContainAdapter.edn"
               :from-key         :ContainAdapter
               :to-key           :SIGContainAdapter})

  ;;; Add exports to CMS db
  (require '[behave-cms.server :refer [init-datahike!]])

  ;; Init Datahike Connection
  (init-datahike!)

  ;; Check connection is good
  @ds/conn

  ;; Verify that global namespace exists
  (lookup-ns-id "global" @ds/conn)

  ;; Commit exports
  (add-export-file-to-conn "./cms-exports/SIGSurface.edn" ds/conn)
  (add-export-file-to-conn "./cms-exports/SIGMoistureScenarios.edn" ds/conn)
  (add-export-file-to-conn "./cms-exports/SIGCrown.edn" ds/conn)
  (add-export-file-to-conn "./cms-exports/SIGBehaveRun.edn" ds/conn)
  (add-export-file-to-conn "./cms-exports/SIGMortality.edn" ds/conn)

  ;; Verify that SIGSurface exists
  (sort (d/q '[:find [?c-name ...]
               :in $ ?name
               :where
               [?e :cpp.namespace/name "global"]
               [?e :cpp.namespace/class ?c]
               [?c :cpp.class/name ?c-name]]
             (safe-deref ds/conn)))

  ;; Verify that SIGSurface functions exist
  (sort (d/q '[:find [?f-name ...]
               :where
               [?c :cpp.class/name "SIGSurface"]
               [?c :cpp.class/function ?f]
               [?f :cpp.function/name ?f-name]]
             (safe-deref ds/conn)))

  )
