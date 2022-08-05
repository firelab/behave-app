(ns behave.stories.icon-stories
  (:require [clojure.string :as str]
            [behave.components.icon :refer [icon]]
            [behave.stories.utils   :refer [->default]]
            [reagent.core :as r]))

(def ^:export default
  #js {:title     "Icons/Icon"
       :component (r/reactify-component icon)})

(defn template [group icon-name]
  (->default {:title (str "Icons/" group "/" (-> icon-name (symbol) (str) (str/upper-case)))
              :component icon
              :args      {:icon-name icon-name}}))

; Module Icons
(def ^:export CrownIcon     (template "Modules" :crown))
(def ^:export ContainIcon   (template "Modules" :contain))
(def ^:export MortalityIcon (template "Modules" :mortality))
(def ^:export SurfaceIcon   (template "Modules" :surface))

; System Icons
(def ^:export ArrowIcon     (template "System" :arrow))
(def ^:export HelpIcon      (template "System" :help))
(def ^:export ManualIcon    (template "System" :manual))
(def ^:export PrintIcon     (template "System" :print))
(def ^:export PdfIcon       (template "System" :pdf))
(def ^:export SaveIcon      (template "System" :save))
(def ^:export ShareIcon     (template "System" :share))
(def ^:export SettingsIcon  (template "System" :settings))
(def ^:export SystemIcon    (template "System" :system))
(def ^:export ToolsIcon     (template "System" :tools))
(def ^:export WorksheetIcon (template "System" :worksheet))
(def ^:export ZoomInIcon    (template "System" :zoom-in))
(def ^:export ZoomOutIcon   (template "System" :zoom-out))
