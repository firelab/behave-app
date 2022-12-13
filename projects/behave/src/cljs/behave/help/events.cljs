(ns behave.help.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
 :help/highlight-section
 (fn [_ [_ help-key]]
   {:fx [[:dispatch [:state/set :help-current-highlighted-key help-key]]]
    :help/scroll-into-view help-key}))

(rf/reg-fx
 :help/scroll-into-view
 (fn [help-key]
   (let [content (first (.getElementsByClassName js/document "help-area__content"))
         section (.getElementById js/document help-key)
         buffer  (* 0.05 (.-offsetHeight content))
         top     (- (.-offsetTop section) (.-offsetTop content) buffer)]
     (.scroll content #js {:top top :behavior "smooth"}))))

(rf/reg-fx
 :help/scroll-top
 (fn []
   (let [content (first (.getElementsByClassName js/document "help-area__content"))]
     (.scroll content #js {:top 0 :behavior "smooth"}))))

(rf/reg-event-db
 :help/select-tab
 (fn [db [_ new-tab]]
   (assoc-in db [:state :help-tab] (:tab new-tab))))
