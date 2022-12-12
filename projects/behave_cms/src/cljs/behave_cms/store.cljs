(ns behave-cms.store
  (:require [clojure.core.async      :refer [go <!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.set             :refer [union]]
            [datascript.core         :as d]
            [re-posh.core            :as rp]
            [behave.schema.core      :refer [all-schemas]]
            [behave.db.utils         :refer [split-datom]]
            [behave.db.compress      :as c]
            [behave-cms.utils        :as u]))

;;; State

(defonce conn (atom nil))

;;; Helper Fns

(defn keep-key
  [m k valid-vals]
  (if (contains? valid-vals (get m k))
    m
    (dissoc m k)))

(defn- simplify-schema [schema]
  (-> schema
      (select-keys [:db/ident :db/valueType :db/index :db/unique :db/cardinality :db/tupleAttrs])
      (keep-key :db/valueType #{:db.type/ref :db.type/tuple})
      (keep-key :db/cardinality #{:db.cardinality/many})))

(defn- required-schema? [schema]
  (or (:db/index schema)
      (:db/unique schema)
      (= (:db/cardinality schema) :db.cardinality/many)
      (#{:db.type/ref :db.type/tuple} (:db/valueType schema))))

(defn- datascript-schema [schema]
  (->> schema
       (filter required-schema?)
       (map simplify-schema)
       (reduce (fn [acc cur] (assoc acc
                                    (:db/ident cur)
                                    (dissoc cur :db/ident)))
               {})))

;;; Sync Helpers

(defonce my-txs (atom #{}))
(defonce sync-txs (atom #{}))

(defn- txs [datoms]
  (into #{} (map #(nth % 3) datoms)))

(defn- new-datom? [datom]
  (not (contains? (union @my-txs @sync-txs) (nth datom 3))))

(defn- sync-url []
  (let [location (.-location js/window)]
    (str (.-protocol location) "//" (.-host location) "/sync")))

(defn sync-tx-data [{:keys [tx-data]}]
  (let [datoms (->> tx-data (filter new-datom?) (mapv split-datom))]
    (when-not (empty? datoms)
      (swap! my-txs union (txs datoms))
      (u/call-remote! :post (sync-url) {:tx-data datoms}))))

(defn sync-latest-datoms! []
  (go
    (let [res    (<! (u/call-remote! :get (sync-url) {:tx (:max-tx @@conn)}))
          datoms (->> (:body res) (filter new-datom?) (map (partial apply d/datom)))]
      (when-not (empty? datoms)
        (swap! sync-txs union (txs datoms))
        (d/transact @conn datoms)))))

(defn get-compressed-datoms []
  (go
    (let [res (<! (u/fetch (sync-url) {:method "get"
                                       :headers {"Accept" "application/msgpack"
                                                 "Content-Type" "application/msgpack"}}))
          array-buffer (<p! (.arrayBuffer res))]
      (c/unpack array-buffer))))

(defn sync-start! []
  (u/refresh-on-interval! sync-latest-datoms! 5000))

(defn first-sync! [schema]
  (go
    (let [datoms (<! (get-compressed-datoms))]
      (reset! conn (d/conn-from-datoms datoms schema))
      (swap! sync-txs union (txs datoms))
      (d/listen! @conn :sync-listener sync-tx-data)
      (rp/connect! @conn)
      (sync-start!))))

;;; Public Fns
(defn connect! []
  (let [schema (datascript-schema (apply concat all-schemas))]
    (first-sync! schema)))

(comment


  (def conn behave-cms.store/conn)
  conn
  (require '[re-frame.core :as rf])
  (require '[re-posh.core :as rp])
  (rp/reg-sub
    :fnames
    (fn [_ _]
      {:type :query
       :query '[ :find  ?e ?fname
                 :where [?e :user/first-name ?fname]]}))

  (rp/reg-sub
    :names
    (fn [_ _]
      {:type :query
       :query '[ :find  ?e ?name
                 :where [?e :user/first+last-name ?name]]}))

  (rp/reg-sub
    :user-names
    (fn [_ _]
      {:type :query
       :query '[ :find  ?name
                 :where [_ :user/first+last-name ?name]]}))

  (rp/reg-event-ds
    :add-user
    (fn [_ [_ fname lname email]]
      [{:user/first-name fname :user/last-name lname :user/email email}]))

  (rf/dispatch [:add-user "Someone" "New" "snew@sig-gis.com"])

  (rf/subscribe [:users])
  (rf/subscribe [:fnames])
  (rf/subscribe [:names])
  (rf/subscribe [:user-ids])
  (rf/subscribe [:user-names])

  (d/q '[:find  ?name :where [?e :user/first+last-name ?name]] @@conn)


  (connect!)
  (count @behave-cms.store/my-txs)

  (:max-tx @@conn)
  (u/call-remote! :get "http://localhost:8080/sync" {:tx (:max-tx @@conn)})
  (sync-latest-datoms!)


  (d/transact @conn [{:db/id -1 :module/name "Mortality" :module/order 3}])

  (d/transact @conn [{:db/id -1 :user/first-name "RJ" :user/last-name "Sheperd" :user/email "rsheperd@sig-gis.com"}])

  (d/transact @conn [{:db/id -1 :user/first-name "Val" :user/last-name "Weslyack" :user/email "vw@sig-gis.com"}])

  (d/q '[:find ?e ?tx
         :in $ ?name
         :where [?e :module/name ?name ?tx]] @@conn "Mortality")

  (:max-tx @@conn)

  (d/q '[:find ?e ?name
          :where [?e :user/first-name ?name]] @@conn)

  (rp/reg-sub
    :all-first-names
    (fn [_ _]
      {:type :query
       :query '[:find ?e ?name
                :where [?e :user/first-name ?name]]}))

  (rp/reg-sub
    :all-user-ids
    (fn [_ _]
      {:type :query
       :query '[:find ?e
                :where [?e :user/first-name]]}))

  (rp/reg-sub
    :all-users
    :<- [:all-user-ids]
    (fn [eids _]
      {:type    :pull-many
       :pattern '[*]
       :ids     (reduce into [] eids)}))

  (rf/subscribe [:all-first-names])
  (rf/subscribe [:all-user-ids])
  (rf/subscribe [:all-users])

  )
