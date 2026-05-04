(ns behave-cms.pages.dashboard
  (:require [behave-cms.components.common :refer [btn]]
            [re-frame.core                :as rf]))

(defn root-component [_]
  [:<>
   [:h1 "Firelab VMS (alpha)"]])
