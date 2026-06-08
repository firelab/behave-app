(ns behave-cms.help.subs
  (:require [applied-science.js-interop :as j]
            [behave-cms.utils           :as u]
            [clojure.set                :refer [rename-keys]]
            [clojure.string             :as str]
            [clojure.walk               :refer [postwalk]]
            [data-utils.interface       :refer [parse-int]]
            [herb.core                  :refer [<class]]
            [markdown2hiccup.interface  :refer [md->hiccup]]
            [re-frame.core              :as rf]
            [re-posh.core               :as rp]
            [reagent.core               :as r]))

;;; Database

(rp/reg-sub
 :help/_page
 (fn [_ [_ help-key language]]
   {:type      :query
    :query     '[:find  ?h .
                 :in    $ ?l ?help-key
                 :where [?h :help-page/key ?help-key]
                 [?l :language/help-page ?h]]
    :variables [language help-key]}))

(rp/reg-sub
 :help/content
 (fn [_ [_ help-key language]]
   {:type      :query
    :query     '[:find  ?content .
                 :in    $ ?l ?help-key
                 :where [?h :help-page/key ?help-key]
                 [?l :language/help-page ?h]
                 [?h :help-page/content ?content]]
    :variables [language help-key]}))

(rf/reg-sub
 :help/page
 (fn [[_ help-key language]]
   (rf/subscribe [:help/_page help-key language]))

 (fn [id]
   (when id
     @(rf/subscribe [:pull '[*] id]))))

;;; Editor

(defn- editor [db]
  (get-in db [:state :editors :help-page]))

(rf/reg-sub
 :help-editor/state
 (fn [db [_ & keys]]
   (get-in (editor db) keys)))

(rf/reg-sub
 :help-editor/content-as-hiccup
 (fn [[_ help-key]]
   (rf/subscribe [:help-editor/state help-key]))

 (fn [{:keys [content]}]
   (md->hiccup content)))
