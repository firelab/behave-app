(ns behave-cms.help.subs
  (:require [clojure.string             :as str]
            [clojure.set                :refer [rename-keys]]
            [clojure.walk               :refer [postwalk]]
            [applied-science.js-interop :as j]
            [reagent.core               :as r]
            [re-frame.core              :as rf]
            [re-posh.core               :as rp]
            [herb.core                  :refer [<class]]
            [hickory.core               :refer [parse-fragment as-hiccup]]
            [behave-cms.markdown.core   :refer [md->html]]
            [data-utils.interface       :refer [parse-int]]
            [behave-cms.utils           :as u]))

;;; Database

(rp/reg-sub
 :help/_page
 (fn [_ [_ help-key language]]
   {:type      :query
    :query     '[:find  [?h]
                 :in    $ ?l ?help-key
                 :where [?h :help/key ?help-key]
                        [?l :language/help-pages ?h]]
    :variables [language help-key]}))

(rp/reg-sub
 :help/content
 (fn [_ [_ help-key language]]
   {:type      :query
    :query     '[:find  [?content]
                 :in    $ ?l ?help-key
                 :where [?h :help/key ?help-key]
                        [?l :language/help-pages ?h]
                        [?h :help/content ?content]]
    :variables [language help-key]}))

(rf/reg-sub
 :help/page
 (fn [[_ help-key language]]
   (rf/subscribe [:help/_page help-key language]))

 (fn [& args]
   (when (some? (ffirst args))
     @(rf/subscribe [:pull '[*] (ffirst args)]))))

;;; Editor

(defn- editor [db]
  (get-in db [:state :editors :help-page]))

(rf/reg-sub
 :help-editor/state
 (fn [db [_ & keys]]
   (get-in (editor db) keys)))


(rf/reg-sub
 :help-editor/content-as-hiccup
 (fn [_]
   (rf/subscribe [:help-editor/state]))

 (fn [{:keys [content]}]
   (md->hiccup content)))

(comment

  (rf/subscribe [:help/content "behaveplus:help" "en-US"])
  (rf/subscribe [:help/content-as-hiccup "behaveplus:help" "en-US"])

  (rf/subscribe [
                 :query '[:find  [?content]
                          :in    $ ?language-shortcode ?help-key
                          :where [?h :help/key ?help-key]
                          [?l :language/shortcode ?language-shortcode]
                          [?l :language/help-pages ?h]
                          [?h :help/content ?content]]
                 ["en-US" "behaveplus:help"]])

  )

