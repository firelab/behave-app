(ns ^:figwheel-hooks behave.client
  (:require [reagent.dom               :refer [render]]
            [re-frame.core             :as rf]
            [behave.components.sidebar :refer [sidebar]]
            [behave.components.toolbar :refer [toolbar]]
            [behave.components.modal :refer [modal]]
            [behave.help.views         :refer [help-area]]
            [behave.settings           :as settings]
            [behave.store              :refer [load-store!]]
            [behave.tools              :as tools]
            [behave.translate          :refer [<t load-translations!]]
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

(def ^:private CANCEL-TIMEOUT-MS 9000)

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
                    :settings/all        settings/root-component
                    :settings/page       settings/root-component
                    :tools/all           tools/root-component
                    :tools/page          tools/root-component})

(defn load-scripts! [{:keys [issue-collector]}]
  (when issue-collector
    (rf/dispatch [:system/add-script issue-collector])))

(defn- before-unload-fn [e]
  (.preventDefault e)
  (js/setTimeout #(rf/dispatch [:system/cancel-close]) CANCEL-TIMEOUT-MS)
  (rf/dispatch [:system/close]))

(defn add-before-unload-event! [{:keys [mode]}]
  (when (= mode "prod")
    (.addEventListener js/window "beforeunload" before-unload-fn)))

(defn app-shell [params]
  (let [route              (rf/subscribe [:handler])
        sync-loaded?       (rf/subscribe [:state :sync-loaded?])
        vms-loaded?        (rf/subscribe [:state :vms-loaded?])
        vms-export-results (rf/subscribe [:state :vms-export-http-results])
        page               (get handler->page (:handler @route) not-found)
        params             (-> (merge params (:route-params @route))
                               (assoc :route-handler (:handler @route)))]
    (if (= (:handler @route) :ws/print)
      (if (and @vms-loaded? @sync-loaded?)
        [page params]
        [:h3 "Loading..."])
      [:div.page
       (when @vms-export-results
         [modal {:title          "Vms Sync"
                 :close-on-click #(do (rf/dispatch [:state/set :vms-export-http-results nil])
                                      (rf/dispatch [:app/reload]))
                 :content        @vms-export-results}])
       [:table.page__top
        [:tr
         [:td.page__top__logo
          [:div.behave-identity
           {:href     "#"
            :on-click #(rf/dispatch [:navigate "/"])
            :tabindex 0}
           [:img.behave-identity__logo
            {:src "/images/logo.png"}]]]
         [:td.page__top__toolbar-container
          [toolbar params]]]]
       [:div.page__main
        [sidebar params]
        [:div.container
         [:div.working-area
          {:area-live "assertive"}
          (if (and @vms-loaded? @sync-loaded?)
            [page params]
            [:h3 "Loading..."])]
         [help-area params]]]])))

(defn- ^:export init
  "Defines the init function to be called from window.onload()."
  [params]
  (let [params (js->clj params :keywordize-keys true)]
    (rf/dispatch-sync [:initialize])
    (rf/dispatch-sync [:navigate (-> js/window .-location .-pathname)])
    (.addEventListener js/window "popstate" #(rf/dispatch [:popstate %]))
    (load-translations!)
    (load-vms!)
    (load-store!)
    (load-scripts! params)
    (add-before-unload-event! params)
    (render [app-shell params] (.getElementById js/document "app"))))

(defn- ^:after-load mount-root!
  "A hook for figwheel to call the init function again."
  []
  (init {}))
