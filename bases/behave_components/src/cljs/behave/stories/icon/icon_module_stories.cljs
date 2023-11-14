(ns behave.stories.icon.icon-module-stories
  (:require [behave.components.icon.core :refer [icon]]
            [behave.stories.utils                :refer [->default]]
            [reagent.core           :as r]))



(def ^:export default
  #js {:title     "Icons/Module Icons"
       :component (r/reactify-component icon)})

(defn template [& [args]]
  (->default {:component icon
              :args      (-> {:selected? false}
                             (merge args))}))

(def ^:export ContainIcon    (template {:icon-name :contain}))
(def ^:export CrownIcon      (template {:icon-name :crown}))
(def ^:export ModulesIcon    (template {:icon-name :modules}))
(def ^:export MortalityIcon  (template {:icon-name :mortality}))
(def ^:export SurfaceIcon    (template {:icon-name :surface}))
(def ^:export SettingsIcon   (template {:icon-name :settings2}))
(def ^:export ToolsIcon      (template {:icon-name :tools2}))
