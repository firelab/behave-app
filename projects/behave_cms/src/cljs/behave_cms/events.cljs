(ns behave-cms.events
  (:require [clojure.core.async :refer [go <!]]
            [ajax.core          :refer [ajax-request]]
            [ajax.edn           :refer [edn-request-format edn-response-format]]
            [bidi.bidi          :refer [path-for]]
            [datascript.core    :refer [squuid]]
            [nano-id.core       :refer [nano-id]]
            [re-frame.core      :refer [dispatch
                                        path
                                        reg-event-db
                                        reg-event-fx
                                        reg-fx]]
            [behave-cms.applications.events]
            [behave-cms.authentication.events]
            [behave-cms.groups.events]
            [behave-cms.languages.events]
            [behave-cms.lists.events]
            [behave-cms.modules.events]
            [behave-cms.submodules.events]
            [behave-cms.subgroups.events]
            [behave-cms.variables.events]
            [behave-cms.routes  :refer [app-routes singular]]
            [behave-cms.subtools.events]
            [behave-cms.utils   :as u]
            [data-utils.interface :refer [remove-nth]]))

;;; Initialization

(def initial-state {:router   {:history       ["/"]
                               :curr-position 0}
                    :state    {:editors  {}
                               :sidebar  {}
                               :selected {}
                               :loaded?  false}
                    :entities {:applications {}
                               :functions    {}
                               :groups       {}
                               :modules      {}
                               :variables    {}}})

(reg-event-db
  :initialize
  (fn [db [_ _]]
    (merge db initial-state)))

(reg-fx
  :history/push-state
  (fn [{:keys [position route]}]
    (.pushState js/history position nil route)))

;;; Session Storage

(reg-fx
  :session/get
  (fn [path]
    (cond
      (keyword? path)
      (get (u/get-session-storage) path)

      (or (vector? path) (seq? path))
      (get-in (u/get-session-storage) path))))

(reg-fx
  :session/set
  (fn [data]
    (u/set-session-storage! data)))

(reg-fx
  :session/remove
  (fn [& ks]
    (apply u/remove-session-storage! ks)))

(reg-fx
  :session/clear
  (fn []
    (u/clear-session-storage!)))

;;; Navigation

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

(reg-event-fx
  :navigate
  (fn [{db :db} [_ new-route]]
    (when-let [[new-history new-position new-route] (navigate (:router db) new-route)]
      {:db (assoc db :router {:history new-history :curr-position new-position})
       :history/push-state {:position new-position
                            :route new-route}})))

(reg-event-fx
  :refresh
  (fn [{db :db} [_ new-route]]
    (set! (.-location js/window) new-route)))

(reg-event-db
  :popstate
  (path :router)
  (fn [router [_ e]]
    (let [new-position (.-state e)]
      (assoc router :curr-position (or new-position 0)))))

;;; State

(reg-event-db
  :state/set-state
  (path :state)
  (fn [db [_ path value]]
    (cond
      (or (vector? path) (list? path))
      (assoc-in db path value)

      (keyword? path)
      (assoc db path value)

      :else db)))

(reg-event-db
  :state/update
  (path :state)
  (fn [db [_ path f]]
    (cond
      (or (vector? path) (list? path))
      (update-in db path f)

      (keyword? path)
      (update db path f)

      :else db)))

(reg-event-db
  :state/select
  (path [:state :selected])
  (fn [db [_ path value]]
    (if (keyword? path)
      (assoc db path value)
      db)))

(reg-event-db
  :state/merge
  (path :state)
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

(reg-event-db
  :state/remove-nth
  (path :state)
  (fn [db [_ path n]]
    (let [v (get-in db path)]
      (println [:REMOVE-NTH [:ARGS path n] path v (remove-nth v n) (assoc-in db path (remove-nth v n))])
      (assoc-in db path (remove-nth v n)))))

;;; AJAX/Fetch Effects

(defn request [{:keys [uri method data on-success on-error fn-args]}]
  (let [handler (fn [[ok result]]
                  (dispatch [(if ok on-success on-error)
                             result
                             fn-args]))]
    (ajax-request {:uri             uri
                   :method          method
                   :handler         handler
                   :params          {:api-args data}
                   :format          (edn-request-format)
                   :response-format (edn-response-format)})))

(reg-fx :api/request request)

(defn http-request [{:keys [uri method body on-success on-error fn-args]}]
  (go (let [response (<! (u/call-remote! method uri body))]
        (if (<= 200 (:status response) 299)
          (dispatch (conj [on-success response] fn-args))
          (dispatch (conj [on-error response] fn-args))))))

(reg-fx :http/request http-request)

;;; Image Uploads

(reg-event-fx
  :files/upload
  (fn [{db :db} [_ file editor-path on-success]]
    (let [form-data (js/FormData.)]
      (.append form-data "file" file)
      {:db (assoc-in db (into editor-path [:uploading?]) true)
       :http/request {:uri        "/file/upload"
                      :body       form-data
                      :method     :post
                      :on-success :files/upload-success
                      :on-error   :files/upload-error
                      :fn-args    [editor-path on-success]}})))

(reg-event-db
 :files/upload-success
 (fn [db [_ {body :body} [editor-path on-success]]]
   (let [file-path (get-in body [:results :path])]
     (js/setTimeout #(on-success file-path) 200)
     (update-in db
                editor-path
                merge
                {:upload-filename file-path
                 :uploading?      false}))))

(reg-event-db
  :files/upload-error
  (fn [db [_ {body :body} editor-path]]
    (update-in db
               editor-path
               merge
               {:upload-error (:error body)
                :uploading?   false})))

;;; Entities API

(reg-event-fx
  :ds/transact
  (fn [_ [_ data]]
    {:transact
     (if (map? data) [data] data)}))

(reg-event-fx
  :api/create-entity
  (fn [_ [_ data]]
    {:transact [(merge {:db/id   -1
                        :bp/uuid (str (squuid))
                        :bp/nid  (nano-id)}
                       data)]}))

(reg-event-fx
 :api/upsert-entity
 (fn [_ [_ data]]
   {:transact
    [(merge data
            (when (nil? (:db/id data))
              {:db/id -1
               :bp/uuid (str (squuid))
               :bp/nid  (nano-id)}))]}))

(reg-event-fx
  :api/update-entity
  (fn [_ [_ data]]
    (when (:db/id data)
      {:transact [data]})))

(reg-event-fx
 :api/retract-entity-attr
 (fn [_ [_ entity attr]]
   (when-let [id (:db/id entity)]
     {:transact [[:db/retract id attr]]})))

(reg-event-fx
 :api/retract-entity-attr-value
 (fn [_ [_ entity-id attr value]]
   {:transact [[:db/retract entity-id attr value]]}))

(reg-event-fx
 :api/delete-entity
 (fn [_ [_ arg]]
   (let [id (cond
              (number? arg)
              arg

              (string? arg)
              [:bp/nid arg]

              (map? arg)
              [:bp/nid (:bp/nid arg)])]

     {:transact [[:db.fn/retractEntity id]]})))

(reg-event-fx
  :api/reorder
  (fn [_ [_ entity all-entities order-field direction]]
    (let [curr-order (get entity order-field)
          sorted     (sort-by order-field all-entities)
          next-order (condp = direction
                       :inc (when (< curr-order (dec (count sorted)))
                              (inc curr-order))
                       :dec (when (> curr-order 0)
                              (dec curr-order)))]
      (when next-order
        (let [swap (nth sorted next-order)]
          {:transact [(assoc (select-keys swap [:db/id]) order-field curr-order)
                      (assoc (select-keys entity [:db/id]) order-field next-order)]})))))
