(ns ^:figwheel-hooks behave.client
  (:require [reagent.dom               :refer [render]]
            [re-frame.core             :as rf]
            [behave.components.sidebar :refer [sidebar]]
            [behave.components.toolbar :refer [toolbar]]
            [behave.help.views         :refer [help-area]]
            [behave.settings           :as settings]
            [behave.store              :refer [load-store!]]
            [behave.tools              :as tools]
            [behave.translate          :refer [<t load-translations!]]
            [behave.vms.store          :refer [load-vms!]]
            [behave.wizard.views       :as wizard]
            [behave.worksheet.views    :refer [new-worksheet-page
                                               import-worksheet-page
                                               guided-worksheet-page
                                               independent-worksheet-page]]
            [behave.events]
            [behave.subs]))

(defn not-found []
  [:div
   [:h1 (str @(<t "notfound") " :(")]])

(def handler->page {:home                new-worksheet-page
                    :ws/all              new-worksheet-page
                    :ws/import           import-worksheet-page
                    :ws/guided           guided-worksheet-page
                    :ws/independent      independent-worksheet-page
                    :ws/wizard           wizard/root-component
                    :ws/review           wizard/wizard-review-page
                    :ws/results-settings wizard/wizard-results-settings-page
                    :ws/results          wizard/wizard-results-page
                    :settings/all        settings/root-component
                    :settings/page       settings/root-component
                    :tools/all           tools/root-component
                    :tools/page          tools/root-component})

(defn app-shell [params]
  (let [route        (rf/subscribe [:handler])
        sync-loaded? (rf/subscribe [:state :sync-loaded?])
        vms-loaded?  (rf/subscribe [:state :vms-loaded?])
        page         (get handler->page (:handler @route) not-found)
        params       (merge params (:route-params @route))]
    [:div.page
     [:table.page__top
      [:tr
       [:td.page__top__logo
        [:div.behave-identity
         [:h1 @(<t "behaveplus")]]]
       [:td.page__top__toolbar-container
        [toolbar]]]]
     [:div.page__main
      [sidebar]
      [:div.container
       [:div.working-area
        (if (and @vms-loaded? @sync-loaded?)
          [page params]
          [:h3 "Loading..."])]
       [help-area params]]]]))

(defn- ^:export init
  "Defines the init function to be called from window.onload()."
  [params]
  (rf/dispatch-sync [:initialize])
  (rf/dispatch-sync [:navigate (-> js/window .-location .-pathname)])
  (.addEventListener js/window "popstate" #(rf/dispatch [:popstate %]))
  (load-translations!)
  (load-vms!)
  (load-store!)
  (render [app-shell (js->clj params :keywordize-keys true)]
          (.getElementById js/document "app")))

(defn- ^:after-load mount-root!
  "A hook for figwheel to call the init function again."
  []
  (init {}))
