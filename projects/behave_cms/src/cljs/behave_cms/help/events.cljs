(ns behave-cms.help.events
  (:require [clojure.string             :as str]
            [clojure.set                :refer [rename-keys]]
            [clojure.walk               :refer [postwalk]]
            [applied-science.js-interop :as j]
            [reagent.core               :as r]
            [re-frame.core              :as rf :refer [path]]
            [herb.core                  :refer [<class]]
            [hickory.core               :refer [parse-fragment as-hiccup]]
            [behave-cms.markdown.core   :refer [md->html]]
            [data-utils.interface       :refer [parse-int]]
            [behave-cms.utils           :as u]))


;;; Editor

(rf/reg-event-db
 :help-editor/set
 (path :state :editors :help-page)
 (fn [page [_ k v]]
   (assoc page k v)))

(rf/reg-event-db
 :help-editor/select-language
 (path :state :editors :help-page)
 (fn [page [_ language]]
   (assoc page :language language)))

;; Saving to DataScript

(rf/reg-event-fx
 :help-editor/save
 (fn [{db :db} [_ latest-help-page]]
   (let [edited-page (get-in db [:state :editors :help-page])
         language    (:language edited-page)
         data        (merge latest-help-page
                            (select-keys edited-page [:help/key :help/content]))]
     {:fx [[:dispatch [:api/update-entity {:db/id language :language/help-pages [data]}]]]})))

;;; Uploading Images

