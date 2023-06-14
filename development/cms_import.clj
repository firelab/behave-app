(ns cms-import
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.edn :refer [read-string]]
            [clojure.set :refer [rename-keys]]))

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

  (cms-import {:behave-file      "~/work/code/hatchet/behave/surface.edn"
               :sig-adapter-file "~/work/code/hatchet/sig-adapters/SIGSurface.edn"
               :out-file-name    "SIGSurface.edn"
               :from-key         :Surface
               :to-key           :SIGSurface})

  (cms-import {:behave-file      "~/work/code/hatchet/behave/moistureScenarios.edn"
               :sig-adapter-file "~/work/code/hatchet/sig-adapters/SIGMoistureScenarios.edn"
               :out-file-name    "SIGMoistureScenarios.edn"
               :from-key         :MoistureScenarios
               :to-key           :SIGMoistureScenarios})

  (cms-import {:behave-file      "~/work/code/hatchet/behave/crown.edn"
               :sig-adapter-file "~/work/code/hatchet/sig-adapters/SIGCrown.edn"
               :out-file-name    "SIGCrown.edn"
               :from-key         :Crown
               :to-key           :SIGCrown})

  (cms-import {:behave-file      "~/work/code/hatchet/behave/behaveRun.edn"
               :sig-adapter-file "~/work/code/hatchet/sig-adapters/SIGBehaveRun.edn"
               :out-file-name    "SIGBehaveRun.edn"
               :from-key         :BehaveRun
               :to-key           :SIGBehaveRun})

  )
