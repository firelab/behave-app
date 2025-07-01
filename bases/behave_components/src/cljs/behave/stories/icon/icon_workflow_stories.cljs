(ns behave.stories.icon.icon-workflow-stories
  (:require [behave.components.icon.core :refer [icon]]
            [behave.stories.icon.ring-icon-common :refer [template]]
            [reagent.core           :as r]))

(def ^:export default
  #js {:title     "Icons/Workflow Icons"
       :component (r/reactify-component icon)})

(def ^:export ExistingRunIcon     (template {:icon-name :existing-run}))
(def ^:export GuidedWorkIcon      (template {:icon-name :guided-work}))
(def ^:export IndependentWorkIcon (template {:icon-name :independent-work}))
(def ^:export ChecklistIcon       (template {:icon-name :checklist}))
