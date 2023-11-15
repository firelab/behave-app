(ns behave.stories.icon.icon-tab-stories
  (:require [behave.components.icon.core :refer [icon]]
            [behave.stories.icon.icon-common :refer [template]]
            [reagent.core           :as r]))

(def ^:export default
  #js {:title     "Icons/Tab Icons"
       :component (r/reactify-component icon)})

(def ^:export CloseIcon      (template :close))
(def ^:export HelpIcon       (template :help))
(def ^:export Help2Icon      (template :help2))
(def ^:export HelpManualIcon (template :help-manual))
(def ^:export ManualIcon     (template :manual))
(def ^:export PdfIcon        (template :pdf))
(def ^:export SettingsIcon   (template :settings))
(def ^:export StarIconIcon   (template :star))
(def ^:export SystemIcon     (template :system))
(def ^:export TipIcon        (template :tip))
(def ^:export ToolsIcon      (template :tools))
