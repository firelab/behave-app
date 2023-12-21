(ns ^:figwheel-hooks behave-cms.client
  (:require [bidi.bidi                          :refer [match-route]]
            [reagent.core                       :as r]
            [reagent.dom                        :refer [render]]
            [re-frame.core                      :as rf]
            [behave-cms.store                   :as s]
            [behave-cms.config                  :refer [update-config]]
            [behave-cms.events]
            [behave-cms.subs]
            [behave-cms.routes                  :refer [app-routes]]
            [behave-cms.components.menu         :refer [menu]]
            [behave-cms.pages.dashboard         :as dashboard]
            [behave-cms.applications.views      :refer [list-applications-page]]
            [behave-cms.authentication.views    :refer [invite-user-page
                                                        login-page
                                                        reset-password-page
                                                        verify-email-page]]
            [behave-cms.domains.views           :refer [domains-page]]
            [behave-cms.groups.views            :refer [list-groups-page]]
            [behave-cms.group-variables.views   :refer [group-variable-page]]
            [behave-cms.languages.views         :refer [list-languages-page]]
            [behave-cms.lists.views             :refer [list-lists-page]]
            [behave-cms.modules.views           :refer [list-modules-page]]
            [behave-cms.tools.views             :refer [tools-page]]
            [behave-cms.units.views             :refer [units-page]]
            [behave-cms.subtools.views          :refer [subtools-page]]
            [behave-cms.subtool-variables.views :refer [subtool-variable-page]]
            [behave-cms.subgroups.views         :refer [list-subgroups-page]]
            [behave-cms.submodules.views        :refer [submodules-page]]
            [behave-cms.variables.views         :refer [list-variables-page]]))

(declare render-page!)

(defonce original-params (atom {}))
(defonce history         (atom []))
(defonce *current-path   (atom nil))

(def menu-pages
  [{:page "Applications"         :path "/applications"}
   {:page "Variables"            :path "/variables"}
   {:page "Variable  Domains"    :path "/domains"}
   {:page "Lists"                :path "/lists"}
   {:page "Units"                :path "/units"}
   {:page "Languages"            :path "/languages"}
   {:page "Invite User"          :path "/invite-user"}])

(def app-pages {:applications         list-applications-page
                :dashboard            dashboard/root-component
                :domains              domains-page
                :get-application      list-modules-page
                :get-group            list-subgroups-page
                :get-group-variable   group-variable-page
                :get-module           submodules-page
                :get-submodule        list-groups-page
                :get-subtool          subtools-page
                :get-subtool-variable subtool-variable-page
                :get-tool             tools-page
                :languages            list-languages-page
                :lists                list-lists-page
                :units                units-page
                :variables            list-variables-page})

(def system-pages {:login          login-page
                   :verify-email   verify-email-page
                   :invite-user    invite-user-page
                   :reset-password reset-password-page})

(def system-page-handlers (set (keys system-pages)))

(def handler->root-component (merge app-pages system-pages))

(defn not-found
  "The root component for the 404 page."
  [_]
  [:div {:style {:margin-top "100px"}}
   [:div {:style {:align-content   "center"
                  :display         "flex"
                  :justify-content "center"
                  :margin-bottom   "6rem"}}
    [:h1 {:style {:text-align "center"}}
     "404 - Page Not Found"]]])

(defn page-component [params]
  (fn [params]
    (let [current-route                  (rf/subscribe [:route])
          {:keys [handler route-params]} (match-route app-routes @current-route)
          system-route?                  (system-page-handlers handler)
          loaded?                        (r/track #(or system-route? @(rf/subscribe [:state :loaded?])))
          component                      (get handler->root-component handler not-found)]
      (if (not @loaded?)
        [:div "Loading..."]
        [:div [component (merge params route-params)]]))))

(defn render-page! [path & [params]]
  (let [dirty-state? (rf/subscribe [:dirty-state?])]
    (rf/dispatch [:navigate path])
    (when (not= path "/login") (s/load-store!))
    (render (cond
              (match-route app-routes path)
              [:div
               [menu menu-pages
                #(if @dirty-state?
                   (when (js/confirm (str "Your work in progress will be lost. Are you sure you want to continue?"))
                     (rf/dispatch [:navigate %])
                     (rf/dispatch [:state/update :selected {}])
                     (rf/dispatch [:state/update :editors {}]))
                   (rf/dispatch [:navigate %]))]
               [page-component (or params @original-params)]]

              :else
              (not-found @original-params))
            (.getElementById js/document "app"))))

(defn- render-root
  "Renders the root component for the current URI."
  [params]
  (let [path (-> js/window .-location .-pathname)]
    (reset! *current-path {:path path :position 0})
    (swap! history conj @*current-path)
    (render-page! path params)))

(defn- ^:export init
  "Defines the init function to be called from window.onload()."
  [params]
  (.addEventListener js/window "popstate" #(rf/dispatch [:popstate %]))
  (let [clj-params (js->clj params :keywordize-keys true)
        cur-params (if (seq clj-params)
                     (reset! original-params
                             (js->clj params :keywordize-keys true))
                     @original-params)]
    (update-config (:client-config clj-params))
    (render-root cur-params)
    (rf/dispatch-sync [:initialize])))

(defn- ^:after-load mount-root!
  "A hook for figwheel to call the init function again."
  []
  (init {}))
