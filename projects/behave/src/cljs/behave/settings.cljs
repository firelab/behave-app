(ns behave.settings
  (:require [re-frame.core :as rf]))

(defn root-component [params]
  [:div
   [:h1 (str "Settings - " (get params :page "All"))]])
