(ns behave.solver.queries
  (:require [datascript.core    :as d]
            [data-utils.interface :refer [is-digit? parse-int parse-float]]
            [behave.store        :as store]
            [behave.schema.core  :refer [rules]]
            [behave.vms.store    :refer [vms-conn]]))

;;; VMS Queries

(defn q-vms
  "Enables querying of both the VMS and Worksheet Datastore with VMS rules.
  Where clauses must use the format `[$ws <entity> <attr> <value>]`
  to retrieve data from the Worksheet datastore."
  [query & args]
  (let [[find in+where] (split-with (complement #{:in :where}) query)
        [in where]      (split-with (complement #{:where}) in+where)
        query-after     (vec (concat find '(:in $ $ws %) (rest in) where))]
    (apply d/q query-after @@vms-conn @@store/conn rules args)))

(defn inputs+units+fn+params [module-name]
  (q-vms '[:find ?uuid ?unit ?fn ?fn-name (count ?all-params) ?p-name ?p-type ?p-order
           :keys
           group-variable/uuid
           group-variable/units
           function/id
           function/name
           function/num-params
           param/name
           param/type
           param/order

           :in ?module-name

           :where
           [?m :module/name ?module-name]
           (module-input-vars ?m ?gv)
           (lookup ?uuid ?gv)
           (variable-fn ?uuid ?fn)
           (varable-units ?uuid ?unit)
           (variable-param ?uuid ?p)
           (cpp-name ?fn ?fn-name)
           (param-attrs ?p ?p-name ?p-type ?p-order)
           (cpp-param ?fn ?all-params)]
         module-name))

(defn variable-kind [group-variable-uuid]
  (q-vms '[:find  ?kind .
           :in    ?gv-uuid
           :where (variable-kind ?gv-uuid ?kind)]
         group-variable-uuid))

(defn parsed-value [group-variable-uuid value]
  (let [kind (variable-kind group-variable-uuid)]
    (condp = kind
      :discrete   (if (is-digit? value) (parse-int value) value)
      :continuous (parse-float value)
      :text       value)))

(defn fn-params [function-id]
  (->> (q-vms '[:find ?p ?p-name ?p-type ?p-order
                :in ?fn
                :where (fn-params ?fn ?p ?p-name ?p-type ?p-order)]
              function-id)
       (sort-by #(nth % 3))))

(defn variable-units [group-variable-uuid]
  (q-vms '[:find  ?units .
           :in    ?gv-uuid
           :where
           (variable-units ?gv-uuid ?units)]
         group-variable-uuid))

(defn variable-native-units-uuid
  [group-variable-uuid]
  (q-vms '[:find  ?native-unit-uuid .
           :in    ?gv-uuid
           :where
           (variable-native-units-uuid ?gv-uuid ?native-unit-uuid)]
         group-variable-uuid))

(defn unit-uuid->enum-value [unit-uuid]
  (q-vms '[:find  ?units .
           :in    ?unit-uuid
           :where
           (units ?unit-uuid ?units)]
         unit-uuid))

(defn unit [unit-uuid]
  (d/pull @@vms-conn '[*] [:bp/uuid unit-uuid]))

;; Cannot use pull due to the use of UUID's to join CPP ns/class/fns
(defn group-variable->fn [group-variable-uuid]
  (or (q-vms '[:find  [?fn ?fn-name (count ?p)]
               :in    ?gv-uuid
               :where (variable-fn ?gv-uuid ?fn)
                      [?fn :cpp.function/name ?fn-name]
                      [?fn :cpp.function/parameters ?p]]
             group-variable-uuid)
      (-> (q-vms '[:find  [?fn ?fn-name]
                   :in    ?gv-uuid
                   :where (variable-fn ?gv-uuid ?fn)
                          [?fn :cpp.function/name ?fn-name]]
                 group-variable-uuid)
          (conj 0))))

(defn subtool-variable->fn [subtool-variable-uuid]
  (q-vms '[:find  [?fn ?fn-name]
           :in    ?gv-uuid
           :where
           (variable-fn ?gv-uuid ?fn)
           [?fn :cpp.function/name ?fn-name]]
         subtool-variable-uuid))

(defn subtool-compute->fn-name [subtool-uuid]
  (q-vms '[:find  ?fn-name .
           :in    ?gv-uuid
           :where
           (subtool-compute-fn ?gv-uuid ?fn)
           [?fn :cpp.function/name ?fn-name]]
         subtool-uuid))

;; Used to get the parent group's UUID
(defn group-variable->group [group-variable-uuid]
  (q-vms '[:find ?group-uuid .
           :in ?gv-uuid
           :where
           [?gv :bp/uuid ?gv-uuid]
           (lookup ?gv-uuid ?gv)
           (group-variable ?g ?gv)
           (lookup ?group-uuid ?g)]
         group-variable-uuid))

(defn module-diagrams [module-name]
  (q-vms '[:find [(pull ?d [* {:diagram/group-variable [:bp/uuid]}]) ...]
           :in ?module-name
           :where
           [?m :module/name ?m-name]
           [(str "(?i)" ?module-name) ?module-find]
           [(re-pattern ?module-find) ?module-find-re]
           [(re-find ?module-find-re ?m-name)]
           [?m :module/diagrams ?d]]
         module-name))

;; Cannot use pull due to the use of UUID's to join CPP ns/class/fns/param
(defn parameter->group-variable [parameter-id]
  (q-vms '[:find  ?gv-uuid .
           :in    ?p
           :where [?p :bp/uuid ?p-uuid]
                  [?gv :group-variable/cpp-parameter ?p-uuid]
                  [?gv :bp/uuid ?gv-uuid]]
         parameter-id))

(defn class-to-group-variables [class-name]
  (set (q-vms '[:find [?gv-uuid ...]
                :in ?class-name
                :where
                [?c :cpp.class/name ?class-name]
                [?c :bp/uuid ?c-uuid]
                [?gv :group-variable/cpp-class ?c-uuid]
                [?gv :bp/uuid ?gv-uuid]] class-name)))

(defn class-to-subtool-variables [class-name]
  (set (q-vms '[:find [?sv-uuid ...]
                :in ?class-name
                :where
                [?c :cpp.class/name ?class-name]
                [?c :bp/uuid ?c-uuid]
                [?sv :subtool-variable/cpp-class-uuid ?c-uuid]
                [?sv :bp/uuid ?sv-uuid]] class-name)))

(defn source-links [gv-uuids]
  (into {}
        (q-vms '[:find ?gv-uuid ?destination-uuid
                 :in [?gv-uuid ...]
                 :where
                 [?s :bp/uuid ?gv-uuid]
                 [?l :link/source ?s]
                 [?l :link/destination ?d]
                 [?d :bp/uuid ?destination-uuid]]
               (vec gv-uuids))))

(defn destination-links [gv-uuids]
  (into {}
        (q-vms '[:find ?source-uuid ?gv-uuid
                 :in [?gv-uuid ...]
                 :where
                 [?d :bp/uuid ?gv-uuid]
                 [?l :link/destination ?d]
                 [?l :link/source ?s]
                 [?s :bp/uuid ?source-uuid]]
               (vec gv-uuids))))

(defn worksheet-modules [ws-uuid]
  (set (q-vms '[:find [?modules ...]
                :in ?ws-uuid
                :where
                [$ws ?w :worksheet/uuid ?ws-uuid]
                [$ws ?w :worksheet/modules ?modules]] ws-uuid)))
