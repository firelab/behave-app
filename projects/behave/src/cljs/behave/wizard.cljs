(ns behave.wizard
  (:require [re-frame.core :as rf]))

(defmulti submodule-page (fn [params] (:io params)))

(defmethod submodule-page :input [params]
  [:h4 "Inputs"])

(defmethod submodule-page :output [params]
  [:h4 "Outputs"])

(defn root-component [params]
  [:div
   [:h1 (str "Module:" (:module params))]
   [:h2 (str "Submodule:" (:submodule params))]
   [:h3 (str "Worksheet ID:" (:db/id params))]
   [submodule-page params]])
