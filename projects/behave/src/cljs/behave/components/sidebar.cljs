(ns behave.components.sidebar
  (:require
   [behave.components.core :as c]
   [behave.components.a11y :refer [on-enter]]
   [behave.translate       :refer [<t bp]]
   [clojure.string         :as str]
   [re-frame.core          :as rf]))

(defn- sidebar-module [{icon-name       :icon
                        translation-key :label
                        on-select       :on-select
                        selected?       :selected?
                        disabled?       :disabled? :as c}]
  (let [translation (<t translation-key)]
    [:div {:class        "sidebar-group__module"
           :on-click     (when (not disabled?)
                           #(on-select c))
           :tabindex     0
           :on-key-press (on-enter #(on-select c))}
     [:div.sidebar-group__module__icon
      [c/icon {:icon-name icon-name
               :selected? selected?}]]
     [:div.sidebar-group__module__label
      @translation]]))

(defn- sidebar-group [{:keys [modules title]}]
  [:div.sidebar-group
   [:div.sidebar-group__header title]
   (for [module (sort-by :order-id modules)]
     ^{:key (:label module)}
     [sidebar-module module])])

(defn sidebar
  "A component for displaying a sidebar with two sections. One for a a list of active modules, and another for settings."
  [{:keys [ws-uuid]}]
  (let [*loaded?          (rf/subscribe [:app/loaded?])
        *hidden?          (rf/subscribe [:state [:sidebar :hidden?]])
        worksheet-modules (when @*loaded?
                            (map (fn [module-entity]
                                   (keyword (str/lower-case (:module/name module-entity))))
                                 @(rf/subscribe [:worksheet/modules ws-uuid])))
        sidebar-modules   (or (seq worksheet-modules)
                              @(rf/subscribe [:state [:sidebar :*modules]]))]

    (if @*hidden?
      [:div.sidebar__expand
       [c/button {:variant       "highlight"
                  :icon-name     "settings"
                  :icon-position "right"
                  :size          "large"
                  :flat-edge     "left"
                  :on-click      #(rf/dispatch [:state/update [:sidebar :hidden?] (partial not)])}]]
      [:div.sidebar-container
       [:div.sidebar-container__modules
        [sidebar-group {:title   @(<t (bp "modules"))
                        :modules (if sidebar-modules
                                   (for [module sidebar-modules]
                                     {:label     (str "behaveplus:" (name module))
                                      :icon      (name module)
                                      :selected? (contains? sidebar-modules module)
                                      :module    #{module}})
                                   [{:label     "behaveplus:surface"
                                     :icon      "surface"
                                     :selected? (contains? sidebar-modules :surface)
                                     :module    #{:surface}}

                                    {:label     "behaveplus:crown"
                                     :icon      "crown"
                                     :selected? (contains? sidebar-modules :crown)
                                     :module    #{:surface :crown}}

                                    {:label     "behaveplus:contain"
                                     :icon      "contain"
                                     :selected? (contains? sidebar-modules :contain)
                                     :module    #{:surface :contain}}

                                    {:label     "behaveplus:mortality"
                                     :icon      "mortality"
                                     :selected? (contains? sidebar-modules :mortality)
                                     :module    #{:mortality}}])}]]

       [:div.sidebar-container__tools-settings
        [sidebar-group {:title   @(<t (bp "calculators_and_settings"))
                        :modules [{:label     "behaveplus:calculators"
                                   :icon      "calculator"
                                   :on-select #(rf/dispatch [:state/set [:sidebar :*tools-or-settings] :tools])}
                                  {:label     "behaveplus:settings"
                                   :icon      "settings2"
                                   :on-select #(if ws-uuid
                                                 (rf/dispatch [:navigate (str "/worksheets/"
                                                                              ws-uuid
                                                                              "/settings")])
                                                 (rf/dispatch [:navigate "/settings"]))}]}]]
       [:div.sidebar-close
        [:div.container__close
         [c/button {:icon-name "close"
                    :on-click  #(rf/dispatch [:state/update [:sidebar :hidden?] (partial not)])
                    :size      "small"
                    :variant   "secondary"}]]]])))
