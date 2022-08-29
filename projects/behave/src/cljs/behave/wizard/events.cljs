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

(rf/reg-event-fx
  :wizard/prev-tab
  (fn [cofx _]

    (let [history   (get-in cofx [:db :router :history])
          prev-page (second (reverse history))]
      {:fx [[:dispatch [:navigate prev-page]]]})))

(rf/reg-event-fx
  :wizard/next-tab
  (fn [_ [_
          _*module
          _*submodule
          all-submodules
          {:keys [id module io submodule]}]]
    (let [[i-subs o-subs] (partition-by #(:submodule/io %) (sort-by :submodule/io all-submodules))
          submodules      (if (= io :input) i-subs o-subs)
          next-submodules (rest (drop-while #(not= (:slug %) submodule) (sort-by :submodule/order submodules)))
          path            (cond
                            (seq next-submodules)
                            (path-for routes
                                      :ws/wizard
                                      :id id
                                      :module module
                                      :io io
                                      :submodule (:slug (first next-submodules)))

                            (and (= io :output) (empty? next-submodules))
                            (path-for routes
                                      :ws/wizard
                                      :id id
                                      :module module
                                      :io :input
                                      :submodule (:slug (first i-subs)))

                            (and (= io :input) (empty? next-submodules))
                            (path-for routes
                                      :ws/review
                                      :id id))]
      {:fx [[:dispatch [:navigate path]]]})))
