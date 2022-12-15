(ns behave-cms.events
  (:require [clojure.string     :as str]
            [clojure.core.async :refer [go <!]]
            [ajax.core          :refer [ajax-request]]
            [ajax.edn           :refer [edn-request-format edn-response-format]]
            [bidi.bidi          :refer [path-for]]
            [re-frame.core      :refer [dispatch
                                        path
                                        reg-event-db
                                        reg-event-fx
                                        reg-fx]]
            [re-posh.core       :refer [reg-event-ds]]
            [behave-cms.applications.events]
            [behave-cms.authentication.events]
            [behave-cms.groups.events]
            [behave-cms.languages.events]
            [behave-cms.modules.events]
            [behave-cms.submodules.events]
            [behave-cms.routes  :refer [app-routes singular]]
            [behave-cms.utils   :as u]))

;;; Initialization

(def initial-state {:router   {:history       ["/"]
                               :curr-position 0}
                    :state    {:editors {}
                               :sidebar {}
                               :loaded? false}
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

;;; Sidebar

(reg-event-fx
  :sidebar/select
  (fn [cofx [_ entity-type uuid]]
    (let [path (path-for app-routes (keyword (str "get-" (name (singular entity-type)))) :uuid uuid)]
      {:db (assoc-in (:db cofx) [:state :sidebar (singular entity-type)] uuid)
       :fx [[:dispatch [:navigate path]]]})))

(reg-event-fx
  :sidebar/reset
  (fn [cofx [_ parent-type parent-uuid]]
    {:db (update-in (:db cofx) [:state :sidebar] assoc (singular parent-type) nil)
     :fx [[:dispatch [:navigate (path-for app-routes parent-type :uuid parent-uuid)]]]}))

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
  (fn [{db :db} [_ file editor-path]]
    (let [form-data (js/FormData.)]
      (.append form-data "file" file)
      {:db (assoc-in db (into editor-path [:uploading?]) true)
       :http/request {:uri        "/file/upload"
                      :body       form-data
                      :method     :post
                      :on-success :files/upload-success
                      :on-error   :files/upload-error
                      :fn-args    editor-path}})))

(reg-event-db
  :files/upload-success
  (fn [db [_ {body :body} editor-path]]
    (let [{:keys [cursor content]
           :or {content "" cursor [0 0]}} (get-in db editor-path)
          path    (get-in body [:results :path])
          img-md  (str "![](" path ")")
          [c1 c2] (split-at (first cursor) content)]
      (update-in db
                 editor-path
                 merge
                 {:content         (str (str/join "" c1) "\n" img-md "\n" (str/join "" c2))
                  :upload-filename (get-in body [:results :path])
                  :uploading?      false}))))

(reg-event-db
  :files/upload-error
  (fn [db [_ {body :body} editor-path]]
    (update-in db
               editor-path
               merge
               {:upload-error (:error body)
                :uploading?   false})))

;;; Data Sync

(reg-event-ds
  :transact
  (fn [_ [_ tx]]
    (when (coll? tx) tx)))

(reg-event-fx
  :sync
  (fn [_ [_ _]]
    (println "Successfully transacted!")
    nil))

(reg-event-fx
  :transact/error
  (fn [_ [_ {body :body}]]
    (println "Unable to transact:" body)
    nil))

(reg-event-fx
  :transact
  (fn [_ [_ tx-data]]
    {:http/request {:uri        "/sync"
                    :body       {:tx-data tx-data}
                    :method     :post
                    :on-success :sync
                    :on-error   :transact/error}}))

;;; Entities API

(reg-event-fx
  :api/create-entity
  (fn [_ [_ data]]
    {:fx [[:dispatch [:transact [(assoc data :db/id -1)]]]]}))

(reg-event-fx
  :api/update-entity
  (fn [_ [_ data]]
    (when (:db/id data)
      {:fx [[:dispatch [:transact [data]]]]})))

(reg-event-fx
  :api/delete-entity
  (fn [_ [_ {id :db/id}]]
    {:fx [[:dispatch [:transact [[:db.fn/retractEntity id]]]]]}))

(reg-event-fx
  :reorder
  (fn [_ [_ all-entities entity order-field direction]]
    (let [curr-order (get entity order-field)
          sorted     (sort-by order-field all-entities)
          next-order (condp = direction
                       :down (when (< curr-order (dec (count sorted)))
                               (inc curr-order))
                       :up   (when (> curr-order 0)
                               (dec curr-order)))]
      (println "--- REORDERING:" all-entities entity curr-order next-order)
      #_(when next-order
        [{:db/id (:db/id (nth all-entities next-order))
          :module/order curr-order}
         {:db/id (:db/id entity)
          :module/order next-order}]))))
