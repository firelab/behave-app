(ns behave-cms.events
  (:require [ajax.core            :refer [ajax-request]]
            [ajax.edn             :refer [edn-request-format edn-response-format]]
            [behave-cms.applications.events]
            [behave-cms.authentication.events]
            [behave-cms.groups.events]
            [behave-cms.languages.events]
            [behave-cms.lists.events]
            [behave-cms.modules.events]
            [behave-cms.subgroups.events]
            [behave-cms.submodules.events]
            [behave-cms.subtools.events]
            [behave-cms.utils     :as u]
            [behave-cms.variables.events]
            [clojure.core.async   :refer [go <!]]
            [data-utils.interface :refer [remove-nth]]
            [datascript.core      :refer [squuid]]
            [nano-id.core         :refer [nano-id]]
            [re-frame.core        :refer [dispatch
                                          path
                                          reg-event-db
                                          reg-event-fx
                                          reg-fx]]))

;;; Initialization

(def initial-state
  "Initial app-db state."
  {:router   {:history       ["/"]
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
 (fn [p]
   (cond
     (keyword? p)
     (get (u/get-session-storage) p)

     (or (vector? p) (seq? p))
     (get-in (u/get-session-storage) p))))

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

(defn navigate
  "Compute new history/position when navigating to a route."
  [{:keys [history curr-position]} new-route]
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
     {:db                 (assoc db :router {:history new-history :curr-position new-position})
      :history/push-state {:position new-position
                           :route    new-route}})))

(reg-event-fx
 :refresh
 (fn [_ [_ new-route]]
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
 (fn [db [_ p value]]
   (cond
     (or (vector? p) (list? p))
     (assoc-in db p value)

     (keyword? p)
     (assoc db p value)

     :else db)))

(reg-event-db
 :state/update
 (path :state)
 (fn [db [_ p f]]
   (cond
     (or (vector? p) (list? p))
     (update-in db p f)

     (keyword? p)
     (update db p f)

     :else db)))

(reg-event-db
 :state/select
 (path [:state :selected])
 (fn [db [_ p value]]
   (if (keyword? p)
     (assoc db p value)
     db)))

(reg-event-db
 :state/merge
 (path :state)
 (fn [state [_ p value]]
   (let [orig-value (get-in state p)]
     (cond
       (nil? orig-value)
       (assoc-in state p value)

       (and (map? orig-value) (map? value))
       (update-in state p merge value)

       (and (or (vector? orig-value) (seq? orig-value))
            (or (vector? value) (seq? orig-value)))
       (update-in state p into value)))))

(reg-event-db
 :state/remove-nth
 (path :state)
 (fn [db [_ p n]]
   (let [v (get-in db p)]
     (println [:REMOVE-NTH [:ARGS p n] p v (remove-nth v n) (assoc-in db p (remove-nth v n))])
     (assoc-in db p (remove-nth v n)))))

;;; AJAX/Fetch Effects

(defn request
  "Issue an EDN ajax request and dispatch on success/error."
  [{:keys [uri method data on-success on-error fn-args]}]
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

(defn http-request
  "Issue an HTTP request via core.async and dispatch on success/error."
  [{:keys [uri method body on-success on-error fn-args]}]
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
     {:db           (assoc-in db (into editor-path [:uploading?]) true)
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
 (fn [_ [_ data {:keys [order-attr siblings]}]]
   (let [next-order (when order-attr
                      (if (seq siblings)
                        (inc (apply max (keep order-attr siblings)))
                        0))]
     {:transact [(merge {:db/id   -1
                         :bp/uuid (str (squuid))
                         :bp/nid  (nano-id)}
                        data
                        (when order-attr {order-attr next-order}))]})))

(reg-event-fx
 :api/upsert-entity
 (fn [_ [_ data]]
   {:transact
    [(merge data
            (when (nil? (:db/id data))
              {:db/id   -1
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
 (fn [_ [_ arg {:keys [order-attr siblings]}]]
   (let [id         (cond
                      (number? arg) arg
                      (string? arg) [:bp/nid arg]
                      (map? arg)    [:bp/nid (:bp/nid arg)])
         deleted-id (cond
                      (number? arg) arg
                      (map? arg)    (:db/id arg))
         renumber   (when (and order-attr (seq siblings) deleted-id)
                      (->> siblings
                           (remove #(= deleted-id (:db/id %)))
                           (sort-by order-attr)
                           (keep-indexed
                            (fn [i e]
                              (when (not= i (get e order-attr))
                                {:db/id (:db/id e) order-attr i})))))]
     {:transact (into [[:db.fn/retractEntity id]] renumber)})))

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

(reg-event-fx
 :scroll-into-view
 (fn [_ [_ element-id]]
   (when-let [element (.getElementById js/document (str "row-" element-id))]
     (.scrollIntoView element #js {:behavior "smooth"
                                   :block    "start"}))))
