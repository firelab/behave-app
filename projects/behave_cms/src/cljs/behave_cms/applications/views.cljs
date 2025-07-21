(ns behave-cms.applications.views
  (:require [re-frame.core :as rf]
            [behave-cms.components.common      :refer [window]]
            [behave-cms.components.sidebar     :refer [sidebar sidebar-width]]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.applications.subs]))

(defn list-applications-page [_]
  [:<>
   [sidebar "Applications" @(rf/subscribe [:sidebar/applications])]
   [window sidebar-width
    [:div.container {:style {:height "900px"}}
     [table-entity-form
      {:title              "Applications"
       :entity             :application
       :entities           (sort-by :application/name
                                    @(rf/subscribe [:applications]))
       :table-header-attrs [:application/name :application/version]
       :entity-form-fields [{:label     "Name"
                             :required? true
                             :field-key :application/name}
                            {:label     "Major Version"
                             :field-key :application/version-major
                             :type      :number
                             :required? true}
                            {:label     "Minor Version"
                             :field-key :application/version-minor
                             :type      :number
                             :required? true}
                            {:label     "Patch Version"
                             :field-key :application/version-patch
                             :type      :number
                             :required? true}]}]]]])
