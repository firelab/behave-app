(ns behave.solver.queries
  (:require [re-frame.core      :as rf]
            [datascript.core    :as d]
            [clojure.string      :as str]
            [behave.logger       :refer [log]]
            [behave.lib.enums    :as enum]
            [behave.lib.units    :as units]
            [behave.store        :as store]
            [behave.vms.store    :refer [vms-conn]]))

;;; Datalog Rules

(def rules '[;; Lookup by UUID
             [(lookup ?uuid ?e) [?e :bp/uuid ?uuid]]

             ;; Lookup another entity by a shared UUID
             [(ref ?uuid1 ?attr ?e2)
              (lookup ?uuid1 ?e1)
              [?e1 ?rel ?uuid2]
              (lookup ?uuid2 ?e2)]

             ;; Find a group variable's variable
             [(gv->var ?uuid ?v)
              (lookup ?uuid ?gv)
              [?v :variable/group-variables ?gv]]

             ;; Find a variable's units
             [(var-units ?uuid ?units)
              (gv->var ?uuid ?v)
              [?v :variable/native-units ?units]]

             ;; Find a variable's kind
             [(kind ?uuid ?kind)
              (gv->var ?uuid ?v)
              [?v :variable/kind ?kind]]

             ;; Find a group variable's function
             [(var->fn ?uuid ?fn)
              (ref ?uuid :group-variable/cpp-function ?fn)]

             ;; Find a group variable's parameter
             [(var->param ?uuid ?p)
              (ref ?uuid :group-variable/cpp-parameter ?p)]

             [(param-attrs ?p ?p-name ?p-type ?p-order)
              [?p cpp.parameter/name ?p-name]
              [?p cpp.parameter/type ?p-type]
              [?p cpp.parameter/order ?p-order]]

             ;; Find the function's parameters
             [(fn-params ?fn ?p ?p-name ?p-type ?p-order)
              [?fn :cpp.function/parameters ?p]
              (param-attrs ?p ?p-name ?p-type ?p-order)]

             [(subgroup ?g ?sg) [?g :group/children ?sg]]

             [(module-output-vars ?m ?gv)
              [?m :module/submodules ?s]
              [?s :submodule/io :output]
              [?s :submodule/groups ?g]
              [?g :group/group-variables ?gv]]

             [(module-output-fns ?m ?fn ?fn-name)
              (module-output-vars ?m ?gv)
              (lookup ?uuid ?gv)
              (var->fn ?uuid ?fn)
              [?fn :cpp.function/name ?fn-name]]

             [(module-input-vars ?m ?gv)
              [?m :module/submodules ?s]
              [?s :submodule/io :input]
              [?s :submodule/groups ?g]
              [?g :group/group-variables ?gv]]

             [(module-input-fns ?m ?fn ?fn-name)
              (module-input-vars ?m ?gv)
              (lookup ?uuid ?gv)
              (var->fn ?uuid ?fn)
              [?fn :cpp.function/name ?fn-name]]

             [(module ?a ?m) [?e :application/modules ?m]]
             [(submodule ?m ?s) [?m :module/submodules ?s]]
             [(group ?s ?g) [?m :submodule/groups ?s]]
             [(subgroup ?g ?sg) [?g :group/children ?sg]]
             [(variable ?g ?v) [?g :group/group-variables ?v]]
             [(language ?code ?l) [?l :language/shortcode ?code]]
             [(translation ?k ?t) [?t :translation/key ?k]]

             [(subgroup ?g ?s)
              [?g :group/children ?s]]

             [(subgroup ?g ?s)
              [?g :group/children ?x]
              (subgroup ?x ?s)]

             [(submodule-root ?submodule ?subgroup)
              [?submodule :submodule/groups ?subgroup]]

             [(submodule-root ?submodule ?subgroup)
              (subgroup ?group ?subgroup)
              [?submodule :submodule/groups ?group]]

             ;; Find the root application for a module, submodule, group, or subgroup
             [(app-root ?a ?g)
              [?sm :submodule/groups ?g]
              [?m :module/submodules ?sm]
              [?a :application/modules ?m]]

             [(app-root ?a ?s)
              (subgroup ?g ?s)
              [?sm :submodule/groups ?g]
              [?m :module/submodules ?sm]
              [?a :application/modules ?m]]])

;;; VMS Queries

(defn q-vms [query & args]
  (let [[find in+where] (split-with (complement #{:in :where}) query)
        [in where]      (split-with (complement #{:where}) in+where)
        query-after     (vec (concat find '(:in $ $ws %) (rest in) where))]
    (apply d/q query-after @@vms-conn @@store/conn rules args)))

(defn inputs+units+fn+params [module-name]
  (q-vms '[:find ?uuid ?units ?fn ?fn-name (count ?all-params) ?p-name ?p-type ?p-order
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
           (var->fn ?uuid ?fn)
           (var-units ?uuid ?units)
           (var->param ?uuid ?p)
           [?fn :cpp.function/name ?fn-name]
           (param-attrs ?p ?p-name ?p-type ?p-order)
           [?fn :cpp.function/parameters ?all-params]]
         module-name))

(defn parsed-value [group-variable-uuid value]
  (let [kind (q-vms '[:find  ?kind .
                      :in    ?gv-uuid
                      :where (kind ?gv-uuid ?kind)]
                    group-variable-uuid)]
    (condp = kind
      "discrete"   (get enum/contain-tactic value)
      "continuous" (js/parseFloat value)
      "text"       value)))

(defn fn-params [function-id]
  (->> (q-vms '[:find ?p ?p-name ?p-type ?p-order
                :in ?fn
                :where (fn-params ?fn ?p ?p-name ?p-type ?p-order)]
              function-id)
       (sort-by #(nth % 3))))

(defn variable-units [group-variable-uuid]
  (q-vms '[:find  ?units .
           :in    ?gv-uuid
           :where (var-units ?gv-uuid ?units)]
         group-variable-uuid))

;; Cannot use pull due to the use of UUID's to join CPP ns/class/fns
(defn group-variable->fn [group-variable-uuid]
  (or (q-vms '[:find  [?fn ?fn-name (count ?p)]
               :in    ?gv-uuid
               :where (var->fn ?gv-uuid ?fn)
                      [?fn :cpp.function/name ?fn-name]
                      [?fn :cpp.function/parameters ?p]]
             group-variable-uuid)
      (-> (q-vms '[:find  [?fn ?fn-name]
                   :in    ?gv-uuid
                   :where (var->fn ?gv-uuid ?fn)
                          [?fn :cpp.function/name ?fn-name]]
                 group-variable-uuid)
          (conj 0))))

;; Used to get the parent group's UUID
(defn group-variable->group [group-variable-uuid]
  (q-vms '[:find ?group-uuid .
           :in ?gv-uuid
           :where
           [?gv :bp/uuid ?gv-uuid]
           (lookup ?gv-uuid ?gv)
           (variable ?g ?gv)
           (lookup ?group-uuid ?g)]
         group-variable-uuid))

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
                [$ ?c :cpp.class/name ?class-name]
                [$ ?c :bp/uuid ?c-uuid]
                [$ ?gv :group-variable/cpp-class ?c-uuid]
                [$ ?gv :bp/uuid ?gv-uuid]] class-name)))

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
