(ns ^:figwheel-hooks behave.client
  (:require [reagent.dom :refer [render]]
            [re-frame.core :as rf]
            [re-frisk.core :as re-frisk]
            [behave.results   :as results]
            [behave.review    :as review]
            [behave.settings  :as settings]
            [behave.components.core :refer [icon]]
            [behave.tools     :as tools]
            [behave.translate :refer [<t load-translations!]]
            [behave.store     :refer [load-store!]]
            [behave.wizard    :as wizard]
            [behave.events]
            [behave.subs]))

(defn sidebar-module [{icon-name :icon translation-key :label}]
  (let [translation (rf/subscribe [:t translation-key])]
    [:div.sidebar-group__module
     [:div.sidebar-group__module__icon [icon icon-name]]
     [:div.sidebar-group__module__label @translation]]))

(defn sidebar-group [modules]
  [:div.sidebar-group
   (for [module modules]
     ^{:key (:label module)}
     [sidebar-module module])])

(defn toolbar-tool [{icon-name :icon translation-key :label}]
  (let [translation (rf/subscribe [:t translation-key])]
    [:div.toolbar__tool
     [:div.toolbar__tool__icon [icon icon-name]]
     [:div.toolbar__tool__label @translation]]))

(defn top-toolbar []
  (let [tools [{:icon :help  :label "behaveplus:help"}
               {:icon :save  :label "behaveplus:save"}
               {:icon :print :label "behaveplus:print"}
               {:icon :share :label "behaveplus:share"}]]
    [:div.toolbar
     (for [tool tools]
       ^{:key (:label tool)}
       [toolbar-tool tool])]))

(defn home-root []
  [:div.page
   [:div.behave-identity
    [:h1 (<t "behaveplus")]]
   [:div.header
    [top-toolbar]]
   [:div.sidebar-container
    [sidebar-group [{:label "behaveplus:surface"   :icon :surface}
                    {:label "behaveplus:crown"     :icon :crown}
                    {:label "behaveplus:mortality" :icon :mortality}
                    {:label "behaveplus:contain"   :icon :contain}]]
    [sidebar-group [{:label "behaveplus:tools"     :icon :tools}
                    {:label "behaveplus:settings"  :icon :settings}]]]
   [:div.container
    [:div.working-area]
    [:div.help-area]]])

(defn not-found []
  [:div
   [:h1 (str (<t "notfound") " :(")]])

(def handler->component {:home          home-root
                         :ws/wizard     wizard/root-component
                         :ws/review     review/root-component
                         :ws/results    results/root-component
                         :settings/all  settings/root-component
                         :settings/page settings/root-component
                         :tools/all     tools/root-component
                         :tools/page    tools/root-component})

(defn page-component [params]
  (let [route (rf/subscribe [:handler])
        component (get handler->component (:handler @route) not-found)]
    [:div.container
     [component (merge params (get @route :route-params))]]))

(defn- ^:export init
  "Defines the init function to be called from window.onload()."
  [params]
  (re-frisk/enable)
  (rf/dispatch-sync [:initialize])
  (load-translations!)
  (load-store!)
  (render [home-root params] (.getElementById js/document "app")))

(defn- ^:after-load mount-root!
  "A hook for figwheel to call the init function again."
  []
  (init {}))
