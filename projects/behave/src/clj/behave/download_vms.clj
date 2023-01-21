(ns behave.download-vms
  (:require [clojure.java.io            :as io]
            [clojure.set                :refer [rename-keys]]
            [ajax.core                  :as http]
            [behave.schema.core         :refer [all-schemas]]
            [datahike.core              :as dc]
            [datom-compressor.interface :as c]
            [datom-utils.interface      :as du]
            [triangulum.logging         :refer [log-str]])
  (:import [java.io FileOutputStream]))

(defn dissoc-nil [m]
  (apply dissoc m (for [[k v] m :when (or (nil? v) (= [:bp/uuid nil] v))] k)))

(defn ->rename-keys [m]
  (rename-keys m {:group-variable/variable-order :group-variable/order
                  :language/language             :language/name
                  :cpp.parameter/parameter-order :cpp.parameter/order
                  :cpp.parameter/parameter-type  :cpp.parameter/type}))

(defn ->kind [m]
  (if-let [kind (:variable/kind m)]
    (assoc m :variable/kind (keyword kind))
    m))

(defn ->longify [m]
  (update-vals m #(if (= java.lang.Integer (class %)) (long %) %)))

(defn ->io [entity]
  (if-let [io (:submodule/input-output entity)]
    (-> entity
        (dissoc :submodule/input-output)
        (assoc :submodule/io (keyword io)))
    entity))

(defn ->repeat? [entity]
  (let [repeat (:group/repeat entity)]
    (if (nil? repeat)
      entity
      (-> entity
          (dissoc :group/repeatable :group/repeat)
          (assoc :group/repeat? (boolean repeat))))))

(defn conn->msgpack [conn]
  (log-str "Transferring datoms to MessagePack...")
  (let [out-file     (io/resource "public/layout.msgpack")
        os           (FileOutputStream. (io/file out-file))
        unsafe-attrs (du/unsafe-attrs all-schemas)]
    (->> (dc/datoms conn :eavt)
         (filter #(du/safe-attr? unsafe-attrs %) )
         (c/pack)
         (.write os))))

(defn export-handler [response]
  (log-str "Loading datoms...")
  (let [entities (->> response
                      (apply concat)
                      (mapv #(-> %
                                 (->io)
                                 (->repeat?)
                                 (dissoc-nil)
                                 (assoc :bp/uuid (:db/id %)))))
        conn     (dc/create-conn)]
    (dc/transact conn all-schemas)
    (dc/transact conn entities)
    (conn->msgpack @conn)))

(defn export-from-vms [auth-token & [url]]
  (log-str "Beginning download from VMS...")
  (http/GET (or "https://behave.sig-gis.com/clj/export-all" url)
            {:params {:auth-token auth-token}
             :format :transit
             :headers {"Content-Type" "application/transit+json"}
             :handler export-handler}))
