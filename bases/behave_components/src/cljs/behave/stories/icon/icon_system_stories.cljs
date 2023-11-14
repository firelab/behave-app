(ns behave.stories.icon.icon-system-stories
  (:require [behave.components.icon.core :refer [icon]]
            [behave.stories.icon.icon-common :refer [template]]
            [reagent.core           :as r]))

(def ^:export default
  #js {:title     "Icons/System Icons"
       :component (r/reactify-component icon)})

(def ^:export ArrowIcon      (template :arrow))
(def ^:export Arrow2Icon     (template :arrow2))
(def ^:export DeleteIcon     (template :delete))
(def ^:export HomeIcon       (template :home))
(def ^:export MinusIcon      (template :minus))
(def ^:export PlusIcon       (template :plus))
(def ^:export EditIcon       (template :edit))
(def ^:export PrintIcon      (template :print))
(def ^:export SaveIcon       (template :save))
(def ^:export ShareIcon      (template :share))
(def ^:export ZoomInIcon     (template :zoom-in))
(def ^:export ZoomOutIcon    (template :zoom-out))
