(ns behave.events
  (:require [browser-utils.core :refer [add-script script-exist? set-local-storage! clear-local-storage! assoc-in-local-storage! create-local-storage!]]
            [ajax.core :as ajax]
            [re-frame.core :as rf]
            [behave.tool.events]))

;;; Initialization

(def initial-state {:router       {:history       []
                                   :curr-position 0}
                    :state        {:io :output
                                   :worksheet {:inputs        {}
                                               :outputs       {}
                                               :repeat-groups {}}}
                    :translations {"en-US" {"behaveplus" "BehavePlus"}}
                    :settings     {:language "en-US"}})

(rf/reg-event-fx
 :initialize
 (fn [{:keys [db]} [_ _]]
   {:db (merge db initial-state)
    :fx [[:dispatch [:local-storage/init "behave-settings"]]]}))

;;; State

(rf/reg-event-db
  :state/set
  (rf/path :state)
  (fn [db [_ path value]]
    (cond
      (or (vector? path) (list? path))
      (assoc-in db path value)

      (keyword? path)
      (assoc db path value)

      :else db)))

(rf/reg-event-db
  :state/merge
  (rf/path :state)
  (fn [state [_ path value]]
    (let [orig-value (get-in state path)]
      (cond
        (nil? orig-value)
        (assoc-in state path value)

        (and (map? orig-value) (map? value))
        (update-in state path merge value)

        (and (or (vector? orig-value) (seq? orig-value))
             (or (vector? value) (seq? orig-value)))
        (update-in state path into value)))))

(rf/reg-event-db
  :state/update
  (rf/path :state)
  (fn [db [_ path f & args]]
    (cond
      (or (vector? path) (list? path))
      (update-in db path #(apply f % args))

      (keyword? path)
      (update db path #(apply f % args))

      :else db)))


;;; Datascript

;;; Navigation

(rf/reg-fx
  :history/push-state
  (fn [{:keys [position route]}]
    (.pushState js/history position nil route)))

(defn navigate [{:keys [history curr-position]} new-route]
  (let [curr-route (get history curr-position)]
    (when (not= new-route curr-route)
      (let [new-history  (-> (+ curr-position 1)
                             (split-at history)
                             (first)
                             (vec)
                             (conj new-route))
            new-position (- (count new-history) 1)]
        [new-history new-position new-route]))))

(rf/reg-event-fx
  :navigate
  (fn [{db :db} [_ new-route]]
    (when-let [[new-history new-position new-route] (navigate (:router db) new-route)]
      {:db (assoc db :router {:history new-history :curr-position new-position})
       :history/push-state {:position new-position
                            :route new-route}})))

(rf/reg-event-db
  :popstate
  (rf/path :router)
  (fn [router [_ e]]
    (let [new-position (.-state e)]
      (assoc router :curr-position (or new-position 0)))))

;;; Settings

(rf/reg-event-db
  :settings/set
  (rf/path [:settings])
  (fn [settings [_ k v]]
    (cond
      (keyword? k)
      (assoc settings k v)

      (vector? k)
      (assoc-in settings k v))))

;;; Local Storage

(rf/reg-event-db
 :local-storage/init
 (fn [_ [_ local-key]]
   (create-local-storage! local-key)))

(rf/reg-event-db
 :local-storage/set
 (fn [_ [_ data]]
   (set-local-storage! data)))

(rf/reg-event-db
 :local-storage/update-in
 (fn [_ [_ path data]]
   (assoc-in-local-storage! path data)))

(rf/reg-event-db
 :local-storage/clear
 (fn [_ [_ data]]
   (clear-local-storage!)))

;;; System

(rf/reg-event-fx
 :system/add-script
 (fn [_ [_ src]]
   (when-not (script-exist? src)
     (add-script src))))

(rf/reg-event-fx
 :system/close
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/close"
                 :response-format (ajax/text-response-format)}}))

(rf/reg-event-fx
 :system/cancel-close
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/close?cancel=true"
                 :response-format (ajax/text-response-format)}}))

;;; Translations

(rf/reg-event-db
  :translations/load
  (rf/path [:translations])
  (fn [translations [_ language new-translations]]
    (update translations language merge new-translations)))

;;; Dev
(rf/reg-event-fx
 :dev/export-from-vms
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/vms-sync"
                 :response-format (ajax/text-response-format)
                 :on-success      [:state/set :vms-export-http-results]
                 :on-failure      [:state/set :vms-export-http-results]}}))

(rf/reg-event-fx
 :dev/print
 (fn [_]
   (js/window.print)))

(rf/reg-event-fx
 :dev/close-after-print
 (fn [_]
   (.addEventListener js/window "afterprint" #(.close js/window))))

(rf/reg-event-fx
 :app/reload
 (fn [_ _]
   (js/window.location.reload)))

(rf/reg-event-fx
 :toolbar/print
 (fn [_ [_ ws-uuid]]
   (.open js/window (str "/worksheets/" ws-uuid "/print"))))
