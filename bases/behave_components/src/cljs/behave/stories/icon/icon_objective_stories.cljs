(ns behave.stories.icon.icon-objective-stories
  (:require [behave.components.icon.core :refer [icon]]
            [behave.stories.icon.ring-icon-common :refer [template]]
            [reagent.core           :as r]))

(def ^:export default
  #js {:title     "Icons/Objective Icons"
       :component (r/reactify-component icon)})

(def ^:export ActiveIncidentIcon (template {:icon-name :active-incident}))
(def ^:export CustomModelIcon    (template {:icon-name :custom-model}))
(def ^:export PlanningIcon       (template {:icon-name :planning}))
(def ^:export ResearchIcon       (template {:icon-name :research}))
