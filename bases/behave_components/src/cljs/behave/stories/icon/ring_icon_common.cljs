(ns behave.stories.icon.ring-icon-common
  (:require [behave.components.icon.core :refer [icon]]
            [behave.stories.utils   :refer [->default]]))

(defn template [& [args]]
  (->default {:component icon
              :args      (merge {:selected? false
                                 :disabled? false
                                 :error?    false}
                                args)}))
