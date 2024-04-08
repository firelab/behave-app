(ns behave.subs
  (:require [bidi.bidi           :refer [match-route]]
            [re-frame.core       :as rf]
            [re-posh.core        :as rp]
            [behave-routing.main :refer [routes]]
            [behave.vms.subs]
            [behave.wizard.subs]
            [behave.components.results.inputs.subs]
            [behave.print.subs]
            [behave.tool.subs]
            [behave.settings.subs]
            [browser-utils.core :refer [get-local-storage]]))

;; Taken from https://lambdaisland.com/blog/11-02-2017-re-frame-form-1-subscriptions
(def <sub (comp deref rf/subscribe))

;;; Navigation

(rf/reg-sub
  :route
  (fn [{:keys [router]} _]
    (get (:history router) (:curr-position router))))

(rf/reg-sub
  :handler
  :<- [:route]
  (fn [new-route]
    (match-route routes new-route)))

;;; State

(rf/reg-sub
  :state
  (fn [{state :state} [_ path]]

    (cond
      (keyword? path)
      (get state path)

      (or (vector? path) (list? path))
      (get-in state path))))

;;; Loading

(rf/reg-sub
  :app/loaded?
  (fn [{state :state} [_]]
    (and (:vms-loaded? state)
         (:sync-loaded? state))))

;;; Settings

(rf/reg-sub
  :settings/get
  (fn [{:keys [settings]} [_ k]]
    (cond
      (keyword? k)
      (get settings k)

      (vector? k)
      (get-in settings k))))

;;; Translations

(rf/reg-sub
  :all-translations
  (fn [{:keys [translations]}]
    translations))

(rf/reg-sub
  :t
  :<- [:settings/get :language]
  :<- [:all-translations]
  (fn [[language all-translations] [_ translation-key]]
    (get-in all-translations [language translation-key])))

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
  (fn [[_ child-attr id _]]
    (rf/subscribe [:children-ids child-attr id]))

  (fn [eids [_ _ _ pattern]]
    {:type    :pull-many
     :pattern (or pattern '[*])
     :ids     (reduce into [] eids)}))

(rf/reg-sub
 :local-storage/get
 (fn [_]
   (get-local-storage)))
