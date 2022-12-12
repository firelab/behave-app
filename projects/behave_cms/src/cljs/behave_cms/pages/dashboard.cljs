(ns behave-cms.pages.dashboard
  (:require [re-frame.core :as rf]
            [behave-cms.components.common :refer [btn]]))

(defn root-component [_]
  [:<>
   [:h1 "Firelab VMS (alpha)"]])
