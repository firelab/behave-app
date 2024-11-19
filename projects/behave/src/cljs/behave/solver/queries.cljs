(ns behave.solver.queries
  (:require [datascript.core    :as d]
            [data-utils.interface :refer [is-digit? parse-int parse-float]]
            [behave.store        :as store]
            [behave.schema.core  :refer [rules]]
            [behave.vms.store    :refer [vms-conn]]))

;;; helpers
(defn uuid->entity
  "Given a UUID Return a datascript entity."
  [uuid_]
  (d/entity @@vms-conn [:bp/uuid uuid_]))

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

(defn variable
  "Given a uuid for a group-variable or subtool-variable return its associated variable as a
  datascript entity."
  [group-variable-uuid]
  (let [group-variable-entity (uuid->entity group-variable-uuid)]
    (or (-> group-variable-entity
            :variable/_group-variables
            first)
        (-> group-variable-entity
            :variable/_subtool-variables
            first))))

(defn variable-kind
  "Given a uuid for a group variable return the `:variable/kind` of its associated variable."
  [group-variable-uuid]
  (-> group-variable-uuid
      variable
      :variable/kind))

(defn parsed-value [group-variable-uuid value]
  (let [kind (variable-kind group-variable-uuid)]
    (condp = kind
      :discrete       (if (is-digit? value) (parse-int value) value)
      :continuous     (parse-float value)
      :text           value)))

(defn fn-params
  "Given an Function entity id, return a sequence of parameter info.
  Parameter info has the form:
  [entity-id name type order]."
  [function-id]
  (let [function-entity (d/entity @@vms-conn function-id)
        function-params (:cpp.function/parameter function-entity)]
    (->> function-params
         (map (fn [param]
                [(:db/id param)
                 (:cpp.parameter/name param)
                 (:cpp.parameter/type param)
                 (:cpp.parameter/order param)]))
         (sort-by #(nth % 3)))))

(defn variable-uuid
  "Given a uuid for a group-variable return the uuid of it's associated variable."
  [group-variable-uuid]
  (-> group-variable-uuid
      variable
      :bp/uuid))

(defn variable-native-units-uuid
  "Given a uuid for a group-variable return either the native-unit uuid from its associated domain
  entity or from it's assocated variable entity."
  [group-variable-uuid]
  (let [var-entity (variable group-variable-uuid)]
    (or (-> var-entity
            :variable/domain-uuid
            uuid->entity
            :domain/native-unit-uuid)
        (-> var-entity
            :variable/native-unit-uuid))))

(defn unit-uuid->enum-value
  "Given a uuid to a unit entity return the enum value for that unit."
  [unit-uuid]
  (->> unit-uuid
       uuid->entity
       :unit/cpp-enum-member-uuid
       uuid->entity
       :cpp.enum-member/value))

(defn unit [unit-uuid]
  (uuid->entity unit-uuid))

(defn group-variable->fn
  "Given a uuid for a group-variable return a sequence of function params:
  [entity-id function-name count-of-function-parameters]."
  [group-variable-uuid]
  (let [group-variable-entity (uuid->entity group-variable-uuid)
        cpp-fn-uuid           (:group-variable/cpp-function group-variable-entity)
        cpp-fn-entity         (uuid->entity cpp-fn-uuid)
        fn-name               (:cpp.function/name cpp-fn-entity)
        cpp-fn-params         (:group-variable/cpp-parameter group-variable-entity)]
    [(:db/id cpp-fn-entity)
     fn-name
     (count cpp-fn-params)]))

(defn subtool-variable->fn
  "Given a uuid for a subtool-variable return a tuple of [function-eid function-name]."
  [subtool-variable-uuid]
  (let [subtool-variable-entity (uuid->entity subtool-variable-uuid)
        cpp-fn-uuid             (:subtool-variable/cpp-function-uuid subtool-variable-entity)
        cpp-fn-entity           (uuid->entity cpp-fn-uuid)
        fn-name                 (:cpp.function/name cpp-fn-entity)]
    [(:db/id cpp-fn-entity)
     fn-name]))

(defn subtool-compute->fn-name
  "Given a uuid for a subtool-variable return its associated function's name."
  [subtool-uuid]
  (-> subtool-uuid
      uuid->entity
      :subtool/cpp-function-uuid
      uuid->entity
      :cpp.function/name))

(defn group-variable->group
  "Given a uuid for a group-varible return it's parent group's uuid."
  [group-variable-uuid]
  (-> group-variable-uuid
      uuid->entity
      :group/_group-variables
      :bp/uuid))

(defn module-diagrams
  "Given a module-name #{surface contain mortality crown} return a sequence of diagram entities."
  [module-name]
  (d/q '[:find [(pull ?d [* {:diagram/group-variable [:bp/uuid]}]) ...]
         :in $ ?module-name
         :where
         [?m :module/name ?m-name]
         [(str "(?i)" ?module-name) ?module-find]
         [(re-pattern ?module-find) ?module-find-re]
         [(re-find ?module-find-re ?m-name)]
         [?m :module/diagrams ?d]]
       @@vms-conn
       module-name))

(defn parameter->group-variable
  "Given a prameter entity id return the uuid for it's associated group variable."
  [parameter-id]
  (let [param-uuid (->> parameter-id
                       (d/entity @@vms-conn)
                       :bp/uuid)]
    (d/q '[:find  ?gv-uuid .
           :in    $ ?p-uuid
           :where
           [?gv :group-variable/cpp-parameter ?p-uuid]
           [?gv :bp/uuid ?gv-uuid]]
          @@vms-conn param-uuid)))

(defn class-to-group-variables
  "Given a class-name (i.e. SIGSurface), return a list of group-variable uuids that belong to that
  class."
  [class-name]
  (set
   (d/q '[:find [?gv-uuid ...]
          :in $ ?class-name
          :where
          [?c :cpp.class/name ?class-name]
          [?c :bp/uuid ?c-uuid]
          [?gv :group-variable/cpp-class ?c-uuid]
          [?gv :bp/uuid ?gv-uuid]]
         @@vms-conn class-name)))

(defn class-to-subtool-variables
  "Given a class-name (i.e. SIGSlopeTool), return a list of subtool-variable uuids that belong to that
  class."
  [class-name]
  (set
   (d/q '[:find [?sv-uuid ...]
          :in $ ?class-name
          :where
          [?c :cpp.class/name ?class-name]
          [?c :bp/uuid ?c-uuid]
          [?sv :subtool-variable/cpp-class-uuid ?c-uuid]
          [?sv :bp/uuid ?sv-uuid]]
         @@vms-conn class-name)))

(defn source-links
  "Given a colleciton of group-variable uuids return a map of the given uuids to it's associated `:link/destination`."
  [gv-uuids]
  (into {}
        (d/q '[:find ?gv-uuid ?destination-uuid
               :in $ [?gv-uuid ...]
               :where
               [?s :bp/uuid ?gv-uuid]
               [?l :link/source ?s]
               [?l :link/destination ?d]
               [?d :bp/uuid ?destination-uuid]]
             @@vms-conn
             (vec gv-uuids))))

(defn output-source-links
  "Obtains outpout Group Variables that serve as sources to links."
  [gv-uuids]
  (into {}
        (d/q '[:find ?gv-uuid ?destination-uuid
               :in $ % [?gv-uuid ...]
               :where
               [?s :bp/uuid ?gv-uuid]
               [?l :link/source ?s]
               [?l :link/destination ?d]
               (io ?s :output)
               [?d :bp/uuid ?destination-uuid]]
             @@vms-conn
             rules
             (vec gv-uuids))))

(defn destination-links
  "Given a colleciton of group-variable uuids return a map of the given uuids to it's associated `:link/source`."
  [gv-uuids]
  (into {}
        (d/q '[:find ?source-uuid ?gv-uuid
               :in $ [?gv-uuid ...]
               :where
               [?d :bp/uuid ?gv-uuid]
               [?l :link/destination ?d]
               [?l :link/source ?s]
               [?s :bp/uuid ?source-uuid]]
             @@vms-conn
             (vec gv-uuids))))

(defn worksheet-modules
  "Given a worksheet uuid return a sequence of modules."
  [ws-uuid]
  (set
   (d/q '[:find [?modules ...]
          :in $ ?ws-uuid
          :where
          [?w :worksheet/uuid ?ws-uuid]
          [?w :worksheet/modules ?modules]]
        @@store/conn
        ws-uuid)))
