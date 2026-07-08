(ns behave.vms.store
  (:require [ajax.core                  :refer [ajax-request]]
            [ajax.protocols             :as pr]
            [behave.perf                :as perf]
            [behave.schema.core         :refer [all-schemas]]
            [behave.translate           :refer [load-translations!]]
            [behave.vms.cache           :as cache]
            [datascript.core            :as d]
            [datom-compressor.interface :as c]
            [datom-utils.interface      :refer [db-attrs
                                                datoms->map]]
            [ds-schema-utils.interface  :refer [->ds-schema]]
            [goog.object                :as gobj]
            [posh.reagent               :refer [pull pull-many q posh!]
             :rename {q posh-query pull posh-pull pull-many posh-pull-many}]
            [re-frame.core              :as rf]))

;;; State

(defonce vms-conn (atom nil))

;;; Helpers

(defn- schema-hash
  "Hash of the client DataScript schema; invalidates cached DBs that were
  hydrated under an older app build's schema."
  []
  (hash (->ds-schema all-schemas)))

(defn- vms-ready!
  "Common tail of both hydrate paths."
  []
  (rf/dispatch-sync [:state/set :vms-loaded? true])
  (perf/store-loaded! :vms)
  (load-translations!))

(defn- write-cache!
  "Serializes the hydrated VMS DB into IndexedDB (off the boot path) so the
  next launch can skip download + unpack + transact."
  [version]
  (let [work (fn []
               (when-let [conn @vms-conn]
                 (cache/put! version
                             (cache/cached-wrapper (schema-hash)
                                                   (d/serializable @conn)))))]
    (if (exists? js/requestIdleCallback)
      (js/requestIdleCallback work)
      (js/setTimeout work 2000))))

(defn- load-data-handler [version [ok body]]
  (when ok
    (perf/mark! "vms-fetch-done")
    (let [raw-datoms (c/unpack body)
          bad-attrs  (db-attrs raw-datoms)
          datoms     (remove (fn [[_ attr vval]]
                               (or (bad-attrs attr)
                                   (nil? vval)))
                             raw-datoms)
          datoms-map (datoms->map datoms)]
      (rf/dispatch-sync [:vms/initialize (->ds-schema all-schemas) datoms-map])
      (vms-ready!)
      (when version (write-cache! version)))))

(defn- hydrate-from-cache!
  "Restores the VMS conn from a cached serialized DB. Returns true on
  success, false if the cache entry is unusable (caller re-fetches)."
  [wrapper]
  (try
    (if (not= (cache/wrapper-schema-hash wrapper) (schema-hash))
      false
      (do
        (when-not @vms-conn
          (reset! vms-conn (d/conn-from-db (d/from-serializable (cache/wrapper-value wrapper))))
          (posh! @vms-conn))
        (perf/mark! "vms-cache-hit")
        (vms-ready!)
        true))
    (catch :default e
      (js/console.warn "VMS cache hydrate failed, refetching:" e)
      false)))

(defn- reloaded-vms-data [[ok _]]
  (when ok
    (rf/dispatch-sync [:state/set :vms-reloaded? true])))

;;; Public Fns

(defn- fetch-vms!
  "XHR fallback when no head-initiated preload is available (dev/figwheel),
  the preload fetch failed, or a cache entry turned out unusable."
  [version]
  (ajax-request {:uri             (str "/layout.msgpack?v=" version)
                 :handler         (partial load-data-handler version)
                 :format          {:content-type "application/text" :write str}
                 :response-format {:description  "ArrayBuffer"
                                   :type         :arraybuffer
                                   :content-type "application/msgpack"
                                   :read         pr/-body}}))

(defn load-vms! [version]
  (perf/mark! "vms-fetch-start")
  ;; The page head resolves this with either the IndexedDB-cached wrapper
  ;; (`{bhpCached: ...}`) or a fetched ArrayBuffer — see
  ;; behave.views/data-prefetch-script. Both are checked here; anything
  ;; unusable falls back to a plain XHR re-fetch.
  (if-let [preload (some-> (gobj/get js/window "bhpPreloads") (gobj/get "vms"))]
    (-> preload
        (.then (fn [res]
                 (if-let [wrapper (and (object? res) (gobj/get res "bhpCached"))]
                   (when-not (hydrate-from-cache! wrapper)
                     (fetch-vms! version))
                   (load-data-handler version [true res]))))
        (.catch (fn [err]
                  (js/console.warn "VMS preload failed, refetching:" err)
                  (fetch-vms! version))))
    (fetch-vms! version)))

(defn reload-vms! []
  (ajax-request {:uri     "/api/vms-sync"
                 :handler reloaded-vms-data}))

;;; Public Fns

(defn init! [{:keys [datoms schema]}]
  (if @vms-conn
    @vms-conn
    (do
      (reset! vms-conn (d/create-conn schema))
      (d/transact @vms-conn datoms)
      (posh! @vms-conn)
      @vms-conn)))

;;; Effects

(rf/reg-fx :vms/init init!)

;;; Events

(rf/reg-event-fx
 :vms/initialize
 (fn [_ [_ schema datoms]]
   {:vms/init {:datoms datoms :schema schema}}))

;;; Operations
(defn q [query & variables]
  (apply posh-query query @vms-conn variables))

(defn pull [pattern id]
  (posh-pull @vms-conn pattern id))

(defn pull-many [pattern ids]
  (posh-pull-many @vms-conn pattern ids))

(defn entity-from-uuid
  "Return a re-frame entity using a UUID (maps to the `:bp/uuid` attribute)"
  [bp-uuid]
  (d/entity @@vms-conn [:bp/uuid bp-uuid]))

(defn entity-from-nid
  "Return a re-frame entity using a Nano-ID (maps to the `:bp/nid` attribute)"
  [bp-nid]
  (d/entity @@vms-conn [:bp/nid bp-nid]))

(defn entity-from-eid
  "Return a re-frame entity using an entity ID (maps to the `:db/id` attribute)"
  [eid]
  (d/entity @@vms-conn eid))
