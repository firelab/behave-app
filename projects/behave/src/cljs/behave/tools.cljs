(ns behave.tools
  (:require [re-frame.core :as rf]))

(defn root-component [params]
  [:div
   [:h1 (str "Tools - " (get params :page "All"))]])
