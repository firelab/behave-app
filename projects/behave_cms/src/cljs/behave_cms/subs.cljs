(ns behave-cms.subs
  (:require [clojure.string    :as str]
            [bidi.bidi         :refer (match-route)]
            [re-frame.core     :as rf]
            [re-posh.core      :as rp]
            [datascript.core   :as d]
            [behave-cms.routes :refer [app-routes]]
            [behave-cms.store  :as s]
            [behave-cms.applications.subs]
            [behave-cms.domains.subs]
            [behave-cms.group-variables.subs]
            [behave-cms.groups.subs]
            [behave-cms.languages.subs]
            [behave-cms.lists.subs]
            [behave-cms.modules.subs]
            [behave-cms.subgroups.subs]
            [behave-cms.submodules.subs]
            [behave-cms.subtools.subs]
            [behave-cms.tools.subs]
            [behave-cms.units.subs]
            [behave-cms.variables.subs]))

;; Taken from https://lambdaisland.com/blog/11-02-2017-re-frame-form-1-subscriptions
(def <sub (comp deref rf/subscribe))

;;; Navigation

(rf/reg-sub :route (fn [{:keys [router]} _]
                     (get (:history router) (:curr-position router))))

(rf/reg-sub :handler (fn [db _]
                     (->> db
                         (:history)
                         (first)
                         (match-route app-routes))))

;;; State

(rf/reg-sub
  :state
  (fn [{state :state} [_ path]]

    (cond
      (nil? path)
      state

      (keyword? path)
      (get state path)

      (or (vector? path) (list? path))
      (get-in state path))))

(rf/reg-sub
 :dirty-state?
 (fn [{state :state}]
   (pos?
    (+ (count (keys (:editors state)))
       (count (keys (:selected state)))))))

(rf/reg-sub
 :selected
 (fn [{state :state} [_ path]]
   (let [path (cond
                (keyword? path)
                (conj [:selected] path)

                (vector? path)
                (concat [:selected] path)

                :else
                [:selected])]
     (get-in state path))))

;;; Entities

(rp/reg-sub
  :entities
  (fn [_ [_ eids pattern]]
    {:type    :pull-many
     :pattern (or pattern '[*])
     :ids     eids}))

(rp/reg-sub
  :entity
  (fn [_ [_ eid pattern]]
    {:type    :pull
     :pattern (or pattern '[*])
     :id      eid}))

(rf/reg-sub
 :touch-entity
 (fn [_ [_ eid]]
   (d/touch (d/entity @@s/conn eid))))

(rp/reg-query-sub
  :entity-attr
  '[:find  ?v .
    :in    $ ?e ?a
    :where [?e ?a ?v]])

;;; DataScript Queries

(rp/reg-sub
  :query
  (fn [_ [_ query variables]]
    {:type :query
     :query query
     :variables variables}))

(rp/reg-sub
  :pull
  (fn [_ [_ pattern id]]
    {:type    :pull
     :pattern pattern
     :id      id}))

(rp/reg-sub
  :pull-many
  (fn [_ [_ pattern ids]]
    {:type    :pull-many
     :pattern pattern
     :ids     ids}))

(rp/reg-query-sub
  :parent-id
  '[:find  ?e .
    :in    $ ?parent-attr ?child-id
    :where [?e ?parent-attr ?child-id]])

(defn- simple-parent-attr [parent-attr]
  (-> (str parent-attr)
      (subs 1)
      (str/replace #"/_" "/")
      (keyword)))

(rp/reg-sub
  :pull-parent
  (fn [[_ parent-attr child-id _]]
    (rf/subscribe [:parent-id (simple-parent-attr parent-attr) child-id]))

  (fn [parent-id [_ _ _ pattern]]
    {:type    :pull
     :pattern (or pattern '[*])
     :id      parent-id}))

(rp/reg-query-sub
  :ids-with-attr
  '[:find  ?e
    :in    $ ?attr
    :where [?e ?attr]])

(rp/reg-sub
  :pull-with-attr
  (fn [[_ attr _]]
    (rf/subscribe [:ids-with-attr attr]))

  (fn [eids [_ _ pattern]]
    {:type    :pull-many
     :pattern (or pattern '[*])
     :ids     (reduce into [] eids)}))

(rp/reg-query-sub
  :children-ids
  '[:find  ?children
    :in    $ ?child-attr ?e
    :where [?e ?child-attr ?children]])

(rp/reg-sub
  :pull-children
  (fn [[_ child-attr id]]
    (rf/subscribe [:children-ids child-attr id]))

  (fn [eids [_ _ _ pattern]]
    {:type    :pull-many
     :pattern (or pattern '[*])
     :ids     (reduce into [] eids)}))

;;; Lookup

(rp/reg-query-sub
 :bp/lookup
 '[:find  ?id .
   :in    $ ?uuid
   :where [?id :bp/uuid ?uuid]])

(rf/reg-sub
 :q-with-rules
 (fn [_ [_ query rules & args]]
   (apply d/q query @@s/conn rules args)))

(rf/reg-sub
 :entity-uuid->name
 (fn [_ [_ uuid]]
   (let [entity (s/entity-from-uuid s/conn uuid)]
    (->> entity
         (keys)
         (filter #(= (name %) "name"))
         first
         (get entity)))))

(rf/reg-sub
 :gv-uuid->variable-name
 (fn [_ [_ uuid]]
   (d/q '[:find ?name .
          :in  $ ?gv-uuid
          :where
          [?gv :bp/uuid ?gv-uuid]
          [?v :variable/group-variables ?gv]
          [?v :variable/name ?name]]
        @@s/conn
        uuid)))


(rf/reg-sub
 :gv-eid->variable-name
 (fn [_ [_ gv-eid]]
   (d/q '[:find ?name .
          :in  $ ?gv
          :where
          [?v :variable/group-variables ?gv]
          [?v :variable/name ?name]]
        @@s/conn
        gv-eid)))
