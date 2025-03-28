(ns ^:figwheel-hooks behave.client
  (:require [clojure.string            :as str]
            [reagent.dom               :refer [render]]
            [re-frame.core             :as rf]
            [behave.components.sidebar :refer [sidebar]]
            [behave.components.toolbar :refer [toolbar]]
            [behave.components.modal   :refer [modal]]
            [behave.help.views         :refer [help-area]]
            [behave.settings.views     :as settings]
            [behave.store              :refer [load-store!]]
            [behave.tools              :as tools]
            [behave.translate          :refer [<t]]
            [behave.vms.store          :refer [load-vms!]]
            [behave.wizard.views       :as wizard]
            [behave.print.views        :refer [print-page]]
            [behave.demo.views         :refer [demo-output-diagram-page]]
            [behave.worksheet.views    :refer [new-worksheet-page
                                               import-worksheet-page
                                               guided-worksheet-page
                                               independent-worksheet-page]]
            [behave.events]
            [behave.subs]
            [day8.re-frame.http-fx]))

(def ^:private CANCEL-TIMEOUT-MS 4000)

(defn not-found []
  [:div
   [:h1 (str @(<t "notfound") " :(")]])

(def handler->page {:home                new-worksheet-page
                    :demo/diagram        demo-output-diagram-page
                    :ws/all              new-worksheet-page
                    :ws/import           import-worksheet-page
                    :ws/guided           guided-worksheet-page
                    :ws/independent      independent-worksheet-page
                    :ws/wizard           wizard/root-component
                    :ws/review           wizard/wizard-review-page
                    :ws/results-settings wizard/wizard-results-settings-page
                    :ws/results          wizard/wizard-results-page
                    :ws/print            print-page
                    :settings/all        settings/settings-page
                    :settings/page       settings/settings-page
                    :tools/all           tools/root-component
                    :tools/page          tools/root-component})

(defn load-scripts! [{:keys [issue-collector sentry]}]
  (when issue-collector
    (rf/dispatch [:system/add-script issue-collector]))
  (when sentry
    (rf/dispatch [:system/add-script sentry])))


(defn- before-unload-fn [e]
  (when-not (str/includes? (.-pathname (.-location js/window)) "print")
    (.preventDefault e)
    (js/setTimeout #(rf/dispatch [:system/cancel-close]) CANCEL-TIMEOUT-MS)
    (rf/dispatch [:system/close])))

(defn add-before-unload-event! [{:keys [mode]}]
  (when (= mode "prod")
    (.addEventListener js/window "beforeunload" before-unload-fn)))

(defn app-shell [params]
  (let [route              (rf/subscribe [:handler])
        sync-loaded?       (rf/subscribe [:state :sync-loaded?])
        vms-loaded?        (rf/subscribe [:state :vms-loaded?])
        vms-export-results (rf/subscribe [:state :vms-export-http-results])
        image-modal        (rf/subscribe [:state [:help-area :image-modal]])
        page               (get handler->page (:handler @route) not-found)
        params             (-> (merge params (:route-params @route))
                               (assoc :route-handler (:handler @route)))]
    [:div.app-shell
     [:div {:style {:display (if @image-modal "block" "none")}}
      [:div.modal__background]
      [modal {:title          (:title @image-modal)
              :close-on-click #(rf/dispatch [:state/set [:help-area :image-modal] nil])
              :content        [:div.image-viewer
                               [:img {:src (:src @image-modal)}]
                               [:p (:alt @image-modal)]]}]]

     (if (= (:handler @route) :ws/print)
       (if (and @vms-loaded? @sync-loaded?)
         [page params]
         [:h3 "Loading..."])
       [:div.page
        (when @vms-export-results
          [modal {:title          "Vms Sync"
                  :close-on-click #(do (rf/dispatch [:state/set :vms-export-http-results nil])
                                       (rf/dispatch [:app/reload]))
                  :content        [:div
                                   {:style {:width "400px"}}
                                   @vms-export-results]}])
        [:table.page__top
         [:tr
          [:td.page__top__logo
           [:div.behave-identity
            {:href     "#"
             :on-click #(rf/dispatch [:wizard/navigate-home])
             :tabindex 0}
            [:img {:src "/images/logo.svg"}]]]
          [:td.page__top__toolbar-container
           [toolbar params]]]]
        [:div.page__main
         [sidebar params]
         [:div {:class ["working-area"
                        (when @(rf/subscribe [:state [:sidebar :hidden?]])
                          "working-area--sidebar-hidden")
                        (when @(rf/subscribe [:state [:help-area :hidden?]])
                          "working-area--help-area-hidden")]}
          (if (and @vms-loaded? @sync-loaded?)
            [page params]
            [:h3 "Loading..."])]
         [help-area params]]])]))

(defn- ^:export init
  "Defines the init function to be called from window.onload()."
  [params]
  (let [params (js->clj params :keywordize-keys true)]
    (rf/dispatch-sync [:initialize])
    (rf/dispatch-sync [:navigate (-> js/window .-location .-pathname)])
    (.addEventListener js/window "popstate" #(rf/dispatch [:popstate %]))
    (load-vms! (:vms-version params))
    (load-store!)
    (load-scripts! params)
    (add-before-unload-event! params)
    (render [app-shell params] (.getElementById js/document "app"))))

(defn- ^:after-load mount-root!
  "A hook for figwheel to call the init function again."
  []
  (init {}))
