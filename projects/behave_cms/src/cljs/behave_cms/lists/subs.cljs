(ns behave-cms.lists.subs
  (:require
   [clojure.set    :refer [rename-keys]]
   [clojure.string :as str]
   [re-frame.core  :as rf]))

(rf/reg-sub
  :lists
  (fn [_]
    (rf/subscribe [:pull-with-attr :list/name '[* {:list/options [*]}]]))
  (fn [lists]
    (sort-by :list/name lists)))

(def ^:private xform-tags #(rename-keys % {:tag/name :label :db/id :value}))

(rf/reg-sub
  :list-option/tags
  (fn [[_ list-option-eid]]
    (rf/subscribe [:pull '[{:list-option/tag-refs [*]}] list-option-eid]))
  (fn [list-option]
    (when-let [tags (:list-option/tag-refs list-option)]
      (->> tags
           (sort-by :tag/order)
           (map :tag/name)
           (str/join ", ")))))

(rf/reg-sub
  :list-option/color-tag
  (fn [[_ list-option-eid]]
    (rf/subscribe [:pull '[{:list-option/color-tag-ref [*]}] list-option-eid]))
  (fn [list-option]
    (-> list-option
        (:list-option/color-tag-ref)
        (:tag/name))))

(rf/reg-sub
  :list-option/tags-to-select
  (fn [[_ list-eid]]
    (rf/subscribe [:pull '[{:list/tag-set [*]}] list-eid]))
  (fn [llist]
    (->> llist
         (:list/tag-set)
         (:tag-set/tags)
         (sort-by :tag/order)
         (map xform-tags))))

(rf/reg-sub
  :list-option/color-tags-to-select
  (fn [[_ list-eid]]
    (rf/subscribe [:pull '[{:list/color-tag-set [*]}] list-eid]))
  (fn [llist]
    (->> llist
         (:list/color-tag-set)
         (:tag-set/tags)
         (sort-by :tag/order)
         (map xform-tags))))
