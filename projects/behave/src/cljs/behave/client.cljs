(ns ^:figwheel-hooks behave.client
  (:require [reagent.dom :refer [render]]
            [re-frame.core :as rf]
            [re-frisk.core :as re-frisk]
            [behave.results   :as results]
            [behave.review    :as review]
            [behave.settings  :as settings]
            [behave.tools     :as tools]
            [behave.translate :refer [<t load-translations!]]
            [behave.store     :refer [load-store!]]
            [behave.wizard    :as wizard]
            [behave.events]
            [behave.subs]))

(defn home-root []
  [:div
   [:h1 (<t "behaveplus")]])

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
  (render [:div [page-component params]] (.getElementById js/document "app")))

(defn- ^:after-load mount-root!
  "A hook for figwheel to call the init function again."
  []
  (init {}))
