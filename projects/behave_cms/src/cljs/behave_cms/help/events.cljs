(ns behave-cms.help.events
  (:require [applied-science.js-interop :as j]
            [behave-cms.markdown.core   :refer [md->html]]
            [behave-cms.utils           :as u]
            [clojure.set                :refer [rename-keys]]
            [clojure.string             :as str]
            [clojure.walk               :refer [postwalk]]
            [data-utils.interface       :refer [parse-int]]
            [herb.core                  :refer [<class]]
            [hickory.core               :refer [parse-fragment as-hiccup]]
            [re-frame.core              :as rf :refer [path]]
            [reagent.core               :as r]))

;;; Editor

(rf/reg-event-db
 :help-editor/set
 (path :state :editors :help-page)
 (fn [page [_ help-key k v]]
   (assoc-in page [help-key k] v)))

;; Saving to DataScript

(rf/reg-event-fx
 :help-editor/save
 (fn [{db :db} [_ help-key latest-help-page]]
   (let [edited-page (get-in db [:state :editors :help-page help-key])
         language    (:language edited-page)
         event       (if (:db/id latest-help-page)
                       :api/update-entity
                       :api/create-entity)
         data        (merge (select-keys latest-help-page [:db/id])
                            (select-keys edited-page [:help-page/content])
                            {:help-page/key help-key :language/_help-page language})]
     {:fx [[:dispatch [event data]]]})))
