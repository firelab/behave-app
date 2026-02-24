(ns austinbirch.reactive-entity
  (:refer-clojure :exclude [exists?])
  (:require [absurder-sql.datascript.core :as d]
            [absurder-sql.datascript.db :as datascript.db]
            [reagent.ratom :as ratom]))

(def initial-state {:subs {}
                    :reverse-subs {}
                    :db-conn nil})

(defonce state (atom initial-state))

(defn cache-reactive-pair!
  [eid attr reactive-pair]
  (let [cache-key [eid attr]]
    (ratom/add-on-dispose! (:reaction reactive-pair)
                           (fn []
                             (let [existing-reactive-pair (get-in @state [:subs cache-key])
                                   existing-reaction (:reaction existing-reactive-pair)]
                               (when (identical? existing-reaction (:reaction reactive-pair))
                                 (swap! state update :subs
                                        (fn [cache]
                                          (dissoc cache cache-key)))))))
    (swap! state assoc-in [:subs cache-key] reactive-pair)
    nil))

(defn clear-cache!
  []
  (let [cache (get @state :subs)]
    (doseq [[cache-key reactive-pair] cache]
      (when-let [reaction (:reaction reactive-pair)]
        (ratom/dispose! reaction))))
  (when (not-empty (get @state :subs))
    (js/console.error "subs bucket should be empty after clearing it.")))

(declare ->ReactiveEntity equiv-entity reactive-entity-lookup)

(defn entity
  [eid]
  (->ReactiveEntity eid))

(defn lookup
  [e attr]
  (when-let [db-conn (:db-conn @state)]
    (let [db @db-conn
          eid (::eid e)
          v (get (d/entity db eid) attr)]

      (cond
        ;; db/id requires us to check index to actually find out if the entity really exists
        (= attr :db/id)
        (let [entid (datascript.db/entid db eid)]
          (if (seq (d/datoms db :eavt entid))
            entid
            nil))

        (and (not (nil? v))
             (or (datascript.db/ref? db attr)
                 (datascript.db/reverse-ref? attr)))
        (if (set? v)
          (->> v
               (map (fn [e]
                      (entity (:db/id e))))
               set)
          (entity (:db/id v)))

        :else
        v))))

(defn reactive-lookup
  [db e attr]
  (let [eid (::eid e)
        cache-key [eid attr]]
    (if-let [reactive-pair (get-in @state [:subs cache-key])]
      @(:reaction reactive-pair)
      (let [initial-v (lookup e attr)
            ratom (ratom/atom initial-v)
            reaction (ratom/make-reaction #(deref ratom))]
        (cache-reactive-pair! eid
                              attr
                              {:ratom ratom
                               :reaction reaction})
        @reaction))))

(deftype ReactiveEntity [eid]
  IEquiv
  (-equiv [this o] (equiv-entity this o))

  ILookup
  (-lookup [this attr] (reactive-entity-lookup this attr nil))
  (-lookup [this attr not-found] (reactive-entity-lookup this attr not-found))

  IFn
  (-invoke [this attr] (reactive-entity-lookup this attr nil))
  (-invoke [this attr not-found] (reactive-entity-lookup this attr not-found))

  IHash
  (-hash [_] (hash eid))

  IAssociative
  (-contains-key?
    [this attr]
    (not= ::not-found
          (reactive-entity-lookup this attr ::not-found))))

(defn reactive-entity-lookup
  [^ReactiveEntity this attr not-found]
  (let [db @(:db-conn @state)
        eid (.-eid this)]
    (if-some [v (reactive-lookup db {::eid eid} attr)]
      v
      not-found)))

(defn exists?
  [^ReactiveEntity this]
  (some? (reactive-entity-lookup this :db/id nil)))

(defn equiv-entity
  [^ReactiveEntity this that]
  (and (instance? ReactiveEntity that)
       (= (.-eid this) (.-eid ^ReactiveEntity that))))

(defn current-state
  "Used for debugging only, non-reactive. Returns a map of the
  existing state of the reactive entity - kind of like what you
  would get if you called d/touch on a DataScript entity.

  Useful for logging out the state of a reactive entity during
  a render pass, without having to subscribe to all changes on
  the entity."
  [^ReactiveEntity entity]
  (if-not (instance? ReactiveEntity entity)
    (throw (ex-info "Can only call `current-state` on a ReactiveEntity"
                    {:entity entity}))
    (let [db @(:db-conn @state)
          ds-entity (d/entity db (.-eid entity))]
      (->> (seq ds-entity)
           (reduce (fn [acc [attr v]]
                     (assoc acc
                       attr (if (datascript.db/ref? db attr)
                              (if (set? v)
                                (->> v
                                     (map (fn [e]
                                            {:db/id (:db/id e)}))
                                     set)
                                {:db/id (:db/id v)})
                              v)))
                   {})
           (merge {:db/id (:db/id entity)})))))

(defn lookup-ref?
  "Returns true if this eid looks like a lookup-ref (e.g. [:entity/id 123])

  #TODO: can check a lot more about lookup-refs here"
  [eid]
  (sequential? eid))

(defn make-matchers
  [{:keys [subs
           db-before
           db-after]}]
  (reduce (fn [acc [cache-key reactive-pair]]
            (let [[eid attr] cache-key
                  is-reverse? (datascript.db/reverse-ref? attr)
                  forward-attr (if is-reverse?
                                 (datascript.db/reverse-ref attr)
                                 attr)
                  lookup-ref (when (lookup-ref? eid)
                               eid)
                  eid-before (when lookup-ref
                               (datascript.db/entid db-before lookup-ref))
                  eid-after (when lookup-ref
                              (datascript.db/entid db-after lookup-ref))
                  update-fn (fnil conj #{})]
              (cond-> acc
                      (nil? lookup-ref)
                      (update (if is-reverse?
                                [forward-attr eid]
                                [eid forward-attr])
                              update-fn
                              cache-key)

                      eid-before
                      (update (if is-reverse?
                                [forward-attr eid-before]
                                [eid-before forward-attr])
                              update-fn
                              cache-key)

                      eid-after
                      (update (if is-reverse?
                                [forward-attr eid-after]
                                [eid-after forward-attr])
                              update-fn
                              cache-key))))
          {}
          subs))

(defn find-cache-keys-needing-re-read
  [{:keys [subs
           tx-report]}]
  (let [db-before (:db-before tx-report)
        db-after (:db-after tx-report)
        tx-data (:tx-data tx-report)]
    (let [matchers (make-matchers {:subs subs
                                   :db-before db-before
                                   :db-after db-after})
          attrs-for-reverse-refs (reduce (fn [acc [[attr eid] _]]
                                           (if (keyword? attr)
                                             (conj acc attr)
                                             acc))
                                         #{}
                                         matchers)]
      (reduce (fn [acc [e a v tx added?]]
                (let [forward-matcher-key [e a]
                      reverse-matcher-key (when (contains? attrs-for-reverse-refs a)
                                            [a v])
                      db-id-match-key [e :db/id]]
                  (as-> acc acc
                        ;; match for attribute reads (e.g. [1 :attr/something] or [[:some/id 1] :attr/something])
                        (if (some? (get matchers forward-matcher-key))
                          (let [matched-cache-keys (get matchers forward-matcher-key)]
                            (apply conj acc matched-cache-keys))
                          acc)

                        ;; match for reverse-ref reads (e.g. [1 :attr/_something] or [[:some/id 1] :attr/_something])
                        (if (and reverse-matcher-key
                                 (some? (get matchers reverse-matcher-key)))
                          (let [matched-cache-keys (get matchers reverse-matcher-key)]
                            (apply conj acc matched-cache-keys))
                          acc)

                        ;; if we have a :db/id for this e then we need to re-read in
                        ;; case the `exists?` state just changed (added, or deleted)
                        (if (some? (get matchers db-id-match-key))
                          (let [matched-cache-keys (get matchers db-id-match-key)]
                            (apply conj acc matched-cache-keys))
                          acc))))
              #{}
              tx-data))))

(defn process-tx-report
  [tx-report]
  (let [cache-keys-needing-re-read (find-cache-keys-needing-re-read
                                     {:subs (:subs @state)
                                      :reverse-subs (:reverse-subs @state)
                                      :tx-report tx-report})
        subs (:subs @state)]
    (doseq [cache-key cache-keys-needing-re-read]
      (let [ratom (:ratom (get subs cache-key))
            [e a] cache-key
            v (lookup {::eid e} a)]
        (reset! ratom v)))))

(defn listen!
  [db-conn]
  (d/listen! db-conn
             ::reactive-entity-listener
             #'process-tx-report))

(defn init!
  [conn]
  (listen! conn)
  (reset! state initial-state)
  (swap! state assoc :db-conn conn))

(comment

  ;; To initialise
  ;;
  ;; - create a db, then call `init!` passing the connection
  (let [db-conn (d/create-conn {:app.todo/id {:db/unique :db.unique/identity}})]
    (d/transact! [{:app.todo/id 1
                   :app.todo/date "2021-03-01"}
                  {:app.todo/id 2
                   :app.todo/date "2021-07-01"}]))

  ;; Ensure that you clear the cache when live reloading
  ;; code (see: https://github.com/day8/re-frame/blob/master/docs/FAQs/Why-Clear-Sub-Cache.md)
  (defn ^:dev/after-load mount-root
    []
    (clear-cache!)
    (reagent.dom/render [#'app] (.getElementById js/document "app")))

  ;; your application init function
  (defn init-reagent-app
    []
    ;; setup reactive-entity
    (init! db-conn)
    ;; mount the reagent app
    (mount-root))

  ;; To use in your reagent components:
  ;;
  ;; - create an entity via `entity`
  ;; - treat entities like maps by accessing keys the
  ;;   usual way
  ;; - components only re-render when the values for
  ;;   the keys that they have accessed change
  ;;
  ;; in the example below:
  ;;
  ;; - when the `:app.todo/date` value changes for the
  ;;   todo entity, only the `todo-date` component re-renders.
  ;; - when `:app.todo/message` changes, only the `todo-view`
  ;;   component re-renders for that todo
  ;; - when any other value changes for the todo, none
  ;;   of the components re-render

  (def todo-date
    [{:keys [<todo]}]
    [:div [:span "Date: " (:app.todo/date <todo)]])

  (defn todo-view
    []
    (let [<todo (entity [:app.todo/id 1])]
      [:div
       [:div
        [:span "Todo: " (:app.todo/message <todo)]]
       [todo-date {:<todo <todo}]]))

  )
