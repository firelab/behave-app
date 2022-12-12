(ns behave-cms.pages.functions
  (:require [re-frame.core :as rf]))

(defn root-component [_]
  [:<>
   [:h1 "Behave Functions"]])
