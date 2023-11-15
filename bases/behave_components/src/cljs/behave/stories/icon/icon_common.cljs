(ns behave.stories.icon.icon-common
  (:require [behave.components.icon.core :refer [icon]]
            [behave.stories.utils   :refer [->default]]))

(defn template [icon-name]
  (->default {:component icon
              :args      {:icon-name icon-name}}))
