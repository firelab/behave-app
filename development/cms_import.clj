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

  )
