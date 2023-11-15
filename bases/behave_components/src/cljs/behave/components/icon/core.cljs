(ns behave.components.icon.core
  (:require [behave.components.icon.ring-icon :as ring-icon]
            [behave.components.icon.default-icon :as default-icon]
            [behave.components.icon.module-icon :as module-icon]))

(defn- get-icon [kw->icons icon-name]
  (get kw->icons (cond
                   (keyword? icon-name)
                   icon-name

                   (string? icon-name)
                   (keyword icon-name)

                   :else
                   nil)))

(defn icon
  "Returns the corresponding icon. Can pass in a map, string, or keyword.

  Examples:
  ```clojure
  [icon :zoom-in]
  [icon \"zoom-in\"]
  [icon {:icon-name \"zoom-in\"}]
  [icon {:icon-name :worksheet
         :disabled? false
         :checked?  false}]
  ```"
  [icon-name]
  (if (map? icon-name)
    (let [ic (:icon-name icon-name)]
      (cond
        (get-icon ring-icon/icons ic)
        (ring-icon/icon-base (merge
                              icon-name
                              {:variant (name ic)
                               :icon    (get-icon ring-icon/icons ic)}))

        (get-icon module-icon/icons ic)
        (module-icon/icon-base (merge
                                icon-name
                                {:variant (name ic)
                                 :icon    (get-icon module-icon/icons ic)}))

        :else
        (icon (:icon-name icon-name))))
    (get-icon default-icon/icons icon-name)))
