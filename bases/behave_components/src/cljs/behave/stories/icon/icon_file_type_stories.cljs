(ns behave.stories.icon.icon-file-type-stories
  (:require [behave.components.icon.core :refer [icon]]
            [behave.stories.icon.ring-icon-common :refer [template]]
            [reagent.core           :as r]))

(def ^:export default
  #js {:title     "Icons/File Type Icons"
       :component (r/reactify-component icon)})

(def ^:export FuelModelIcon   (template {:icon-name :fuel-model}))
(def ^:export ImportFilesIcon (template {:icon-name :import-files}))
(def ^:export WorksheetIcon   (template {:icon-name :worksheet}))
