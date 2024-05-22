(ns behave.components.sidebar
  (:require
   [behave.components.core :as c]
   [behave.components.a11y :refer [on-enter]]
   [behave.translate       :refer [<t bp]]
   [re-frame.core          :as rf]))

(defn- sidebar-header [title]
  [:div.sidebar-group__header title
   #_[:div.sidebar-group__header__close
    [c/button {:icon-name "close"
               :on-click  #(rf/dispatch [:state/update [:sidebar :hidden?] (partial not)])
               :shape     "round"
               :size      "small"
               :variant   "secondary"}]]])

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
   [sidebar-header title]
   (for [module (sort-by :order-id modules)]
     ^{:key (:label module)}
     [sidebar-module module])])

(defn sidebar [{:keys [ws-uuid io]}]
  (let [*hidden?         (rf/subscribe [:state [:sidebar :hidden?]])
        *sidebar-modules (rf/subscribe [:state [:sidebar :*modules]])
        on-select        #(do (rf/dispatch [:state/set [:sidebar :*modules] (:module %)])
                              (rf/dispatch [:state/set [:worksheet :*modules] (:module %)]))]
    (if @*hidden?
      [:div [c/button {:variant       "highlight"
                       :icon-name     "settings"
                       :icon-position "right"
                       :size          "large"
                       :flat-edge     "left"
                       :on-click      #(rf/dispatch [:state/update [:sidebar :hidden?] (partial not)])}]]
      [:div.sidebar-container
       [sidebar-group {:title   @(<t (bp "modules"))
                       :modules (if @*sidebar-modules
                                  (for [module @*sidebar-modules]
                                    {:label     (str "behaveplus:" (name module))
                                     :icon      (name module)
                                     :selected? (contains? @*sidebar-modules module)
                                     :module    #{module}
                                     :on-select #(when ws-uuid
                                                   (let [module-name    (name (first (:module %)))
                                                         *module        (rf/subscribe [:wizard/*module module-name])
                                                         submodule-slug (:slug (first @(rf/subscribe [:wizard/submodules-io-output-only (:db/id @*module)])))]
                                                     (rf/dispatch [:navigate (str "/worksheets/" ws-uuid "/modules/" module-name "/output/" submodule-slug)])))})
                                  [{:label     "behaveplus:surface"
                                    :icon      "surface"
                                    :on-select on-select
                                    :selected? (contains? @*sidebar-modules :surface)
                                    :module    #{:surface}}

                                   {:label     "behaveplus:crown"
                                    :icon      "crown"
                                    :on-select on-select
                                    :selected? (contains? @*sidebar-modules :crown)
                                    :module    #{:surface :crown}}

                                   {:label     "behaveplus:contain"
                                    :icon      "contain"
                                    :on-select on-select
                                    :selected? (contains? @*sidebar-modules :contain)
                                    :module    #{:surface :contain}}

                                   {:label     "behaveplus:mortality"
                                    :icon      "mortality"
                                    :on-select on-select
                                    :selected? (contains? @*sidebar-modules :mortality)
                                    :module    #{:mortality}}])}]

       [sidebar-group {:title   @(<t (bp "tools_and_settings"))
                       :modules [{:label     "behaveplus:tools"
                                  :icon      "tools2"
                                  :on-select #(rf/dispatch [:state/set [:sidebar :*tools-or-settings] :tools])}
                                 {:label     "behaveplus:settings"
                                  :icon      "settings2"
                                  :on-select #(if ws-uuid
                                                (rf/dispatch [:navigate (str "/worksheets/"
                                                                             ws-uuid
                                                                             "/settings")])
                                                (rf/dispatch [:navigate "/settings"]))}]}]
       [:div.sidebar-container__close
        [c/button {:icon-name "close"
                   :on-click  #(rf/dispatch [:state/update [:sidebar :hidden?] (partial not)])
                   :shape     "round"
                   :size      "small"
                   :variant   "secondary"}]]])))
