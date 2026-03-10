(ns behave.vms.store
  (:require [ajax.core                  :refer [ajax-request]]
            [ajax.protocols             :as pr]
            [absurder-sql.datascript.core :as d]
            [clojure.string             :as str]
            [posh.reagent               :refer [pull pull-many q posh!]
             :rename {q posh-query pull posh-pull pull-many posh-pull-many}]
            [promesa.core               :as p]
            [re-frame.core              :as rf]
            [datom-compressor.interface :as c]
            [ds-schema-utils.interface  :refer [->ds-schema]]
            [behave.schema.core         :refer [all-schemas]]
            [behave.translate           :refer [load-translations!]]))

;;; State

(defonce vms-conn (atom nil))

;;; IndexedDB VMS Cache

(def ^:private idb-name "behave-vms-cache")
(def ^:private idb-store "vms")

(defn- idb-open []
  (js/Promise.
   (fn [resolve reject]
     (let [req (.open js/indexedDB idb-name 1)]
       (set! (.-onupgradeneeded req)
             (fn [e]
               (let [db (.. e -target -result)]
                 (when-not (.contains (.-objectStoreNames db) idb-store)
                   (.createObjectStore db idb-store)))))
       (set! (.-onsuccess req) (fn [e] (resolve (.. e -target -result))))
       (set! (.-onerror req) (fn [e] (reject (.. e -target -error))))))))

(defn- idb-get-cache [version]
  (-> (idb-open)
      (p/then (fn [db]
                (js/Promise.
                 (fn [resolve _reject]
                   (let [tx  (.transaction db idb-store "readonly")
                         os  (.objectStore tx idb-store)
                         req (.get os version)]
                     (set! (.-onsuccess req)
                           (fn [e]
                             (.close db)
                             (resolve (.. e -target -result))))
                     (set! (.-onerror req)
                           (fn [_]
                             (.close db)
                             (resolve nil))))))))
      (p/catch (fn [_] nil))))

(defn- idb-set-cache! [version body]
  (-> (idb-open)
      (p/then (fn [db]
                (let [tx (.transaction db idb-store "readwrite")
                      os (.objectStore tx idb-store)]
                  (.clear os)
                  (.put os body version)
                  (.close db))))
      (p/catch (fn [e] (js/console.warn "VMS cache write failed:" e)))))

;;; Helpers

(defn- db-attr?
  "Returns true if keyword starts with :db or :fressian."
  [k]
  (let [s (str k)]
    (or (str/starts-with? s ":db")
        (str/starts-with? s ":fressian"))))

(defn- raw-datoms->map
  "Single-pass reduce: filters out :db/* / :fressian* attrs and nil values,
   and accumulates into an entity map keyed by entity id."
  [raw-datoms]
  (let [entities (persistent!
                  (reduce (fn [acc [e a v]]
                            (if (or (db-attr? a) (nil? v))
                              acc
                              (if-let [entity (get acc e)]
                                (let [cur (get entity a)
                                      val (cond
                                            (coll? cur) (conj cur v)
                                            (some? cur) (vector cur v)
                                            :else       v)]
                                  (assoc! acc e (assoc entity a val)))
                                (assoc! acc e {a v}))))
                          (transient {})
                          raw-datoms))]
    (sort-by :db/id
             (map (fn [[idx m]] (assoc m :db/id idx)) entities))))

(defn- process-and-init! [body version]
  (let [datoms-map (raw-datoms->map (c/unpack body))]
    (rf/dispatch-sync [:vms/initialize (->ds-schema all-schemas) datoms-map])
    (rf/dispatch-sync [:state/set :vms-loaded? true])
    (load-translations!)
    (when version
      (idb-set-cache! version (js/Uint8Array. body)))))

(defn- fetch-vms! [version]
  (ajax-request {:uri             (str "/layout.msgpack?v=" version)
                 :handler         (fn [[ok body]]
                                   (when ok
                                     (process-and-init! body version)))
                 :format          {:content-type "application/text" :write str}
                 :response-format {:description  "ArrayBuffer"
                                   :type         :arraybuffer
                                   :content-type "application/msgpack"
                                   :read         pr/-body}}))

(defn- reloaded-vms-data [[ok _]]
  (when ok
    (rf/dispatch-sync [:state/set :vms-reloaded? true])))

;;; Public Fns

(defn load-vms! [version]
  (-> (idb-get-cache version)
      (p/then (fn [cached]
                (if cached
                  (process-and-init! cached nil)
                  (fetch-vms! version))))
      (p/catch (fn [_]
                 (fetch-vms! version)))))

(defn reload-vms! []
  (ajax-request {:uri     "/api/vms-sync"
                 :handler reloaded-vms-data}))

;;; Public Fns

(defn init! [{:keys [datoms schema]}]
  (if @vms-conn
    @vms-conn
    (let [conn (d/create-conn schema)]
      (reset! vms-conn conn)
      (d/transact conn datoms)
      (posh! conn)
      conn)))

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
