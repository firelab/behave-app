(ns schema-migrate.core
  (:require
   [clojure.walk       :as walk]
   [clojure.string     :as s]
   [datascript.core    :refer [squuid]]
   [datomic.api        :as d]
   [datomic-store.main :as ds]
   [clojure.spec.alpha :as spec]
   [nano-id.core       :refer [nano-id]]))

(def
  ^{:doc "Random UUID in string format."}
  rand-uuid (comp str squuid))

(defn name->uuid
  "Get the :bp/uuid using the name for the specified name attribute"
  [conn attr nname]
  (d/q '[:find ?uuid .
         :in $ ?attr ?name
         :where
         [?e ?attr ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       attr
       nname))

(defn name->nid
  "Get the :bp/nid using the name for the specified name attribute"
  [conn attr nname]
  (d/q '[:find ?nid .
         :in $ ?attr ?name
         :where
         [?e ?attr ?name]
         [?e :bp/nid ?nid]]
       (d/db conn)
       attr
       nname))

(defn name->eid
  "Get the datomic entity id given the attribute and name. May not work as expected if attr and name
  is not unique."
  [conn attr nname]
  (d/q '[:find ?e .
         :in $ ?attr ?name
         :where [?e ?attr ?name]]
       (d/db conn)
       attr
       nname))

(defn cpp-ns->uuid
  "Given the namespace name, ret the :bp/uuid using the cpp namepsace name"
  [conn nname]
  (d/q '[:find ?uuid .
         :in $ ?name
         :where
         [?e :cpp.namespace/name ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       nname))

(defn cpp-class->uuid
  "Given the namespace and class, return the :bp/uuid of the class entity."
  [conn nnamespace cclass]
  (d/q '[:find ?uuid .
         :in $ ?namespace ?class
         :where
         [?ns :cpp.namespace/name ?namespace]
         [?ns :cpp.namespace/class ?c]
         [?c :cpp.class/name ?class]
         [?c :bp/uuid ?uuid]]
       (d/db conn)
       nnamespace
       cclass))

(defn cpp-fn->uuid
  "Given the namespace class and function name, return the :bp/uuid of the function entity."
  ([conn nnamespace cclass fn-name]
   (d/q '[:find ?uuid .
          :in $ ?namespace ?class ?fn-name
          :where
          [?ns :cpp.namespace/name ?namespace]
          [?ns :cpp.namespace/class ?c]
          [?c :cpp.class/name ?class]
          [?c :cpp.class/function ?f]
          [?f :cpp.function/name ?fn-name]
          [?f :bp/uuid ?uuid]]
        (d/db conn)
        nnamespace
        cclass
        fn-name)))

(defn cpp-param->uuid
  "Given the namespace, class, function name and parameter name, return the :bp/uuid of the parameter entity."
  ([conn nnamespace cclass fn-name param-name]
   (d/q '[:find ?uuid .
          :in $ ?namespace ?class ?fn-name
          :where
          [?ns :cpp.namespace/name ?namespace]
          [?ns :cpp.namespace/class ?c]
          [?c :cpp.class/name ?class]
          [?c :cpp.class/function ?f]
          [?f :cpp.function/name ?fn-name]
          [?f :cpp.function/parameter ?p]
          [?p :cpp.parameter/name ?param-name]
          [?p :bp/uuid ?uuid]]
        (d/db conn)
        nnamespace
        cclass
        fn-name
        param-name)))

(defn cpp-uuids
  "Given a map of with the names of a namespace, class and function, return a map
  that resolves the names to a uuid. Requires all three names."
  [conn {:keys [cpp-namespace cpp-class cpp-function cpp-param]}]
  (first
   (d/q '[:find ?ns-uuid ?c-uuid ?f-uuid ?param-uuid
          :keys cpp-namespace cpp-class cpp-function cpp-param
          :in $ ?namespace ?class ?function
          :where
          [?n :cpp.namespace/name ?namespace]
          [?n :cpp.namespace/class ?c]
          [?n :bp/uuid ?ns-uuid]
          [?c :cpp.class/name ?class]
          [?c :bp/uuid ?c-uuid]
          [?c :cpp.class/function ?fn]
          [?fn :cpp.function/name ?function]
          [?fn :bp/uuid ?f-uuid]
          [?f :cpp.function/parameter ?p]
          [?p :cpp.parameter/name ?param-name]
          [?p :bp/uuid ?param-uuid]]
        (d/db conn)
        cpp-namespace
        cpp-class
        cpp-function
        cpp-param)))

(defn- submodule [conn t]
  (->> t
       (d/q '[:find ?e .
              :in $ ?t
              :where [?e :submodule/translation-key ?t]]
            (ds/unwrap-db conn)
            t)
       (d/entity (ds/unwrap-db conn))))

(defn eid->t-key
  "Returns an entity's translation key."
  [conn eid]
  (d/q '[:find ?k .
         :in $ ?e
         :where
         (or
          [?e :application/translation-key ?k]
          [?e :module/translation-key ?k]
          [?e :submodule/translation-key ?k]
          [?e :group/translation-key ?k]
          [?e :subtool-variable/translation-key ?k]
          [?e :tag/translation-key ?k]
          [?e :tag-set/translation-key ?k]
          [?e :group-variable/translation-key ?k])] (d/db conn) eid))

(defn t-key->uuid
  "Get the :bp/uuid using translation-key"
  [conn t]
  (when-let [e (or (d/entity (ds/unwrap-db conn) [:group-variable/translation-key t])
                   (d/entity (ds/unwrap-db conn) [:group-variable/result-translation-key t])
                   (d/entity (ds/unwrap-db conn) [:subtool-variable/translation-key t])
                   (d/entity (ds/unwrap-db conn) [:group/translation-key t])
                   (d/entity (ds/unwrap-db conn) [:module/translation-key t])
                   (d/entity (ds/unwrap-db conn) [:tag-set/translation-key t])
                   (d/entity (ds/unwrap-db conn) [:tag/translation-key t])
                   (d/entity (ds/unwrap-db conn) [:color-tag/translation-key t])
                   (d/entity (ds/unwrap-db conn) [:search-table/translation-key t])
                   (submodule conn t))]
    (:bp/uuid (d/touch e))))

(defn t-key->entity
  "Get the datomic entity given a translation-key"
  [conn t]
  (d/entity (ds/unwrap-db conn) [:bp/uuid (t-key->uuid conn t)]))

(defn t-key->eid
  "Get the db/id using translation-key"
  [conn t]
  (:db/id (t-key->entity conn t)))

(defn t-key-action-name->eid
  "Given a translation-key of a group-variable an action's name return the action's entity id"
  [conn gv-t-key action-name]
  (let [entity (t-key->entity conn gv-t-key)]
    (->> (filter #(= (:action/name %) action-name)
                 (:group-variable/actions entity))
         first
         :db/id)))

(defn bp6-code->variable-eid
  "Given a Behave6 Variable code (e.g. `vSurfaceFuelCode`), returns the matching variable entity ID."
  [conn bp6-code]
  (d/q '[:find ?e .
         :in $ ?bp6-code
         :where [?e :variable/bp6-code ?bp6-code]]
       (d/db conn)
       bp6-code))

(defn make-attr-is-component-payload
  "Returns a payload for updating a given attribute to include :db/isComponent true"
  [conn attr]
  (when-let [eid (d/q '[:find ?e .
                        :in $ ?attr
                        :where [?e :db/ident ?attr]]
                      (ds/unwrap-db conn)
                      attr)]
    {:db/id          eid
     :db/isComponent true}))

(defn make-attr-is-component!
  [conn attr]
  (ds/transact conn [(make-attr-is-component-payload conn attr)]))

(defn rollback-tx!
  "Given a transaction ID or a transaction result (return from datomic.api/transact), Reassert
  retracted datoms and retract asserted datoms in a transaction, effectively \"undoing\" the
  transaction."
  [conn tx]
  (when-let [tx-id (cond
                     (number? tx) tx
                     (map? tx)    (nth (first (:tx-data tx)) 3)
                     :else        nil)]
    (let [tx-log  (-> conn ds/unwrap-conn d/log (d/tx-range tx-id nil) first) ; find the transaction
          txid    (-> tx-log :t d/t->tx) ; get the transaction entity id
          newdata (->> (:data tx-log)   ; get the datoms from the transaction
                       (remove #(= (:e %) txid)) ; remove transaction-metadata datoms
                                        ; invert the datoms add/retract state.
                       (map #(do [(if (:added %) :db/retract :db/add) (:e %) (:a %) (:v %)]))
                       reverse)] ; reverse order of inverted datoms.
      (ds/transact conn newdata))))

(defn- insert-bp-uuid [x]
  (if (and (map? x) (nil? (:bp/uuid x)))
    (assoc x :bp/uuid (str (squuid)))
    x))

(defn- insert-bp-nid [x]
  (if (and (map? x) (nil? (:bp/nid x)))
    (assoc x :bp/nid (nano-id))
    x))

(defn postwalk-insert
  "Postwalk over data and insert bp/uuid and bp/nid into every map form"
  [data]
  (->> data
       (walk/postwalk insert-bp-uuid)
       (walk/postwalk insert-bp-nid)))

(defn build-translations-payload
  "Given a map of translation-key to it's translation create a payload that creates these
  translation entities as well as adding these refs to the exisitng Enlgish language entity.
  `eid-start` is optional and will be used as the starting eid for the
  translation entities (prevents eid overlap if you wish to include this payload
  in the same transaction where you've manually assigned :db/id). "
  ([conn t-key->translation-map]
   (build-translations-payload conn 0 t-key->translation-map))

  ([conn eid-start t-key->translation-map]
   (let [translations              (map-indexed (fn [idx [t-key translation]]
                                                  {:db/id                   (-> idx
                                                                                inc
                                                                                (+ eid-start)
                                                                                (* -1))
                                                   :translation/key         t-key
                                                   :translation/translation translation})
                                                t-key->translation-map)
         english-language-eid      (d/q '[:find ?e .
                                          :in $
                                          :where
                                          [?e :language/name "English"]]
                                        (d/db conn))
         language-translation-refs {:db/id                english-language-eid
                                    :language/translation (map :db/id translations)}]
     (into [language-translation-refs]
           translations))))

(defn update-translations-payload
  "Creates a payload to update existing translations for specific language shortcode."
  [conn shortcode translation-map]
  (let [language-eid
        (d/q '[:find ?e .
               :in $ ?shortcode
               :where
               [?e :language/shortcode ?shortcode]]
             (d/db conn) shortcode)
        query-translation-eid
        (fn [language-eid t-key]
          (d/q '[:find ?t .
                 :in $ ?lang ?t-key
                 :where
                 [?lang :language/translation ?t]
                 [?t :translation/key ?t-key]]
               (d/db conn) language-eid t-key))]
    (map (fn [[t-key translation]]
           {:db/id                   (query-translation-eid language-eid t-key)
            :translation/translation translation}) translation-map)))

(defn remove-nested-i18ns-tx
  "Removes an entity (and it's components), along with all nested
   translation keys."
  [conn t-key]
  (let [i18ns (d/q '[:find [?e ...]
                     :in $ ?t-key
                     :where
                     [?e :translation/key ?key]
                     (or
                      [(= ?key ?t-key)]
                      [(clojure.string/starts-with? ?key ?t-key)])] (d/db conn) t-key)]

    (map (fn [eid] [:db/retractEntity eid]) i18ns)))

(defn ->entity
  "Payload for an Entity"
  [data]
  (merge data
         {:bp/uuid (rand-uuid)
          :bp/nid  (nano-id)}))

(defn ->gv-conditional
  "Payload for a Group Variable Conditional."
  [uuid operator value]
  {:bp/uuid                         (rand-uuid)
   :bp/nid                          (nano-id)
   :conditional/group-variable-uuid uuid
   :conditional/type                :group-variable
   :conditional/operator            operator
   :conditional/values              value})

(defn ->module-conditional
  "Payload for a Module Conditional."
  [operator values]
  {:bp/uuid              (rand-uuid)
   :bp/nid               (nano-id)
   :conditional/type     :module
   :conditional/operator operator
   :conditional/values   values})

(defn ->conditional
  "Payload for a Conditional."
  [_conn {:keys [ttype operator values group-variable-uuid] :as params}]
  (let [payload (cond-> {}
                  (nil? (:bp/uuid params)) (assoc :bp/uuid  (rand-uuid))
                  (nil? (:bp/nid params))  (assoc :bp/nid  (nano-id))
                  (:bp/uuid params)        (assoc :bp/uuid (:bp/uuid params))
                  (:bp/nid params)         (assoc :bp/nid (:bp/nid params))
                  (:db/id params)          (assoc :db/id (:db/id params))
                  group-variable-uuid      (assoc :conditional/group-variable-uuid group-variable-uuid)
                  ttype                    (assoc :conditional/type ttype)
                  operator                 (assoc :conditional/operator operator)
                  values                   (assoc :conditional/values values))]
    (if (spec/valid? :behave/conditional payload)
      payload
      (spec/explain :behave/conditional payload))))

(defn ->action
  "Payload for an Action."
  [conn {:keys [nname ttype target-value conditionals] :as params}]
  (let [payload (cond-> {}
                  (nil? (:bp/uuid params)) (assoc :bp/uuid  (rand-uuid))
                  (not  (:bp/nid params))  (assoc :bp/nid  (nano-id))
                  (:bp/uuid params)        (assoc :bp/uuid (:bp/uuid params))
                  (:bp/nid params)         (assoc :bp/nid (:bp/nid params))
                  (:db/id params)          (assoc :db/id (:db/id params))
                  nname                    (assoc :action/name nname)
                  ttype                    (assoc :action/type ttype)
                  target-value             (assoc :action/target-value target-value)
                  conditionals             (assoc :action/conditionals (map #(->conditional conn %) conditionals)))]
    (if (spec/valid? :behave/action payload)
      payload
      (spec/explain :behave/action payload))))

(defn ->variable
  [_conn {:keys [nname domain-uuid list-eid translation-key help-key kind bp6-label bp6-code map-units-convertible?] :as params}]
  (let [payload (cond-> {}
                  (nil? (:bp/uuid params)) (assoc :bp/uuid (rand-uuid))
                  (not  (:bp/nid params))  (assoc :bp/nid  (nano-id))
                  (:bp/uuid params)        (assoc :bp/uuid (:bp/uuid params))
                  (:bp/nid params)         (assoc :bp/nid (:bp/nid params))
                  (:db/id params)          (assoc :db/id (:db/id params))
                  nname                    (assoc :variable/name nname)
                  kind                     (assoc :variable/kind kind)
                  domain-uuid              (assoc :variable/domain-uuid domain-uuid)
                  list-eid                 (assoc :variable/list list-eid)
                  translation-key          (assoc :variable/translation-key translation-key)
                  help-key                 (assoc :variable/help-translation-key help-key)
                  bp6-label                (assoc :variable/bp6-label bp6-label)
                  bp6-code                 (assoc :variable/bp6-code bp6-code)
                  map-units-convertible?   (assoc :variable/map-units-convertible? map-units-convertible?))]
    (if (spec/valid? :behave/variable payload)
      payload
      (spec/explain :behave/variable payload))))

(defn ->group-variable
  "Payload for a new Group Variable."
  [conn {:keys
         [parent-group-eid order variable-eid  cpp-namespace cpp-class cpp-function cpp-parameter translation-key conditionally-set? actions
          hide-result-conditionals hide-result? disable-multi-valued-input-conditionals disable-multi-valued-input-conditional-operator] :as params}]
  (let [payload (if (spec/valid? :behave/group-variable params)
                  params
                  (cond-> {}
                    (nil? (:bp/uuid params))                        (assoc :bp/uuid  (rand-uuid))
                    (not  (:bp/nid params))                         (assoc :bp/nid  (nano-id))
                    (:bp/uuid params)                               (assoc :bp/uuid (:bp/uuid params))
                    (:bp/nid params)                                (assoc :bp/nid (:bp/nid params))
                    (:db/id params)                                 (assoc :db/id (:db/id params))
                    parent-group-eid                                (assoc :group/_group-variables parent-group-eid)
                    order                                           (assoc :group-variable/order order)
                    variable-eid                                    (assoc :variable/_group-variables variable-eid)
                    cpp-namespace                                   (assoc :group-variable/cpp-namespace (cpp-ns->uuid conn cpp-namespace))
                    cpp-class                                       (assoc :group-variable/cpp-class (cpp-class->uuid conn cpp-namespace cpp-class))
                    cpp-function                                    (assoc :group-variable/cpp-function (cpp-fn->uuid conn cpp-namespace cpp-class cpp-function))
                    cpp-parameter                                   (assoc :group-variable/cpp-parameter (cpp-param->uuid conn cpp-namespace cpp-class cpp-function cpp-parameter))
                    translation-key                                 (assoc :group-variable/translation-key translation-key)
                    translation-key                                 (assoc :group-variable/result-translation-key (s/replace translation-key ":output:" ":result:"))
                    translation-key                                 (assoc :group-variable/help-key (str translation-key ":help"))
                    conditionally-set?                              (assoc :group-variable/conditionally-set? conditionally-set?)
                    (seq actions)                                   (assoc :group-variable/actions (map #(cond->> % (map? %) (->action conn)) actions))
                    hide-result?                                    (assoc :group-variable/hide-result? hide-result?)
                    (seq hide-result-conditionals)                  (assoc :group-variable/hide-result-conditionals (map #(cond->> % (map? %) (->conditional conn)) hide-result-conditionals))
                    disable-multi-valued-input-conditional-operator (assoc :group-variable/disable-multi-valued-input-conditional-operator disable-multi-valued-input-conditional-operator)
                    (seq disable-multi-valued-input-conditionals)   (assoc :group-variable/disable-multi-valued-input-conditionals (map #(cond->> % (map? %) (->conditional conn)) disable-multi-valued-input-conditionals))))]
    (if (spec/valid? :behave/group-variable payload)
      payload
      (spec/explain :behave/group-variable payload))))

(defn ->group
  "Payload for a new Group."
  [conn {:keys [parent-submodule-eid parent-group-eid group-name order translation-key conditionals group-variables subgroups research? hidden?] :as params}]
  (let [payload (if (spec/valid? :behave/group params)
                  params
                  (cond-> {}
                    (nil? (:bp/uuid params)) (assoc :bp/uuid  (rand-uuid))
                    (nil? (:bp/nid params))  (assoc :bp/nid  (nano-id))
                    (:bp/uuid params)        (assoc :bp/uuid (:bp/uuid params))
                    (:bp/nid params)         (assoc :bp/nid (:bp/nid params))
                    (:db/id params)          (assoc :db/id (:db/id params))
                    parent-submodule-eid     (assoc :submodule/_groups parent-submodule-eid)
                    parent-group-eid         (assoc :group/_children parent-group-eid)
                    group-name               (assoc :group/name group-name)
                    order                    (assoc :group/order order)
                    translation-key          (assoc :group/translation-key translation-key)
                    translation-key          (assoc :group/result-translation-key (s/replace translation-key ":output:" ":result:"))
                    translation-key          (assoc :group/help-key (str translation-key ":help"))
                    (seq conditionals)       (assoc :group/conditionals (map #(cond->> % (map? %) (->conditional conn)) conditionals))
                    (seq group-variables)    (assoc :group/group-variables (map #(cond->> % (map? %) (->group-variable conn)) group-variables))
                    (seq subgroups)          (assoc :group/children (map #(cond->> % (map? %) (->group conn)) subgroups))
                    research?                (assoc :group/research? research?)
                    hidden?                  (assoc :group/hidden? hidden?)))]
    (if (spec/valid? :behave/group payload)
      payload
      (spec/explain :behave/group payload))))

(defn ->submodule
  "Payload for a Submdoule"
  [conn {:keys [io submodule-name order groups research? translation-key conditionals conditionals-operator] :as params}]
  (let [payload (if (spec/valid? :behave/submodule params)
                  params
                  (cond-> {}
                    (nil? (:bp/uuid params)) (assoc :bp/uuid  (rand-uuid))
                    (nil? (:bp/nid params))  (assoc :bp/nid  (nano-id))
                    (:bp/uuid params)        (assoc :bp/uuid (:bp/uuid params))
                    (:bp/nid params)         (assoc :bp/nid (:bp/nid params))
                    (:db/id params)          (assoc :db/id (:db/id params))
                    io                       (assoc :submodule/io io)
                    submodule-name           (assoc :submodule/name submodule-name)
                    order                    (assoc :submodule/order order)
                    (seq groups)             (assoc :submodule/groups (map #(cond->> % (map? %) (->group conn)) groups))
                    (seq conditionals)       (assoc :submodule/conditionals (map #(cond->> % (map? %) (->conditional conn)) conditionals))
                    conditionals-operator    (assoc :submodule/conditionals-operator conditionals-operator)
                    translation-key          (assoc :submodule/translation-key translation-key)
                    translation-key          (assoc :submodule/help-key (str translation-key ":help"))
                    research?                (assoc :submodule/research? research?)))]
    (if (spec/valid? :behave/submodule payload)
      payload
      (spec/explain :behave/submodule payload))))

(defn ->link
  "Payload for a new Link."
  [source-eid destination-eid]
  {:bp/uuid          (rand-uuid)
   :bp/nid           (nano-id)
   :link/source      source-eid
   :link/destination destination-eid})

(defn ->migration
  "New migration."
  [id]
  {:bp/uuid         (rand-uuid)
   :bp/nid          (nano-id)
   :bp/migration-id id})

(defn bp
  "add behaveplus: to `s`"
  [& s]
  (apply str "behaveplus:" s))
