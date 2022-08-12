(ns behave.wizard.events
  (:require [bidi.bidi :refer [path-for]]
            [re-frame.core :as rf]
            [behave-routing.main :refer [routes]]))


(rf/reg-event-fx
  :wizard/select-tab
  (fn [_ [_ {:keys [id module io submodule]}]]
    (let [path (path-for routes
                         :ws/wizard
                         :id id
                         :module module
                         :io io
                         :submodule submodule)]
    {:fx [[:dispatch [:navigate path]]]})))
