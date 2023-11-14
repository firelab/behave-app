(ns behave.stories.icon.icon-results-menu-stories
  (:require [behave.components.icon.core :refer [icon]]
            [behave.stories.icon.icon-common :refer [template]]
            [reagent.core           :as r]))

(def ^:export default
  #js {:title     "Icons/Results Menu Icons"
       :component (r/reactify-component icon)})

(def ^:export GraphsIcon (template :graphs))
(def ^:export NotesIcon  (template :notes))
(def ^:export TablesIcon (template :tables))
