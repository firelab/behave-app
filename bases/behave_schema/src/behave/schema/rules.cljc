(ns behave.schema.rules)

;;; UUID Rules

(def uuid-rules
  "Generic Rules for resolving entities to their UUID"
  '[;; Lookup by UUID
    [(lookup ?uuid ?e) [?e :bp/uuid ?uuid]]

    ;; Lookup another entity by a shared UUID
    [(ref ?uuid1 ?attr ?e2)
     (lookup ?uuid1 ?e1)
     [?e1 ?rel ?uuid2]
     (lookup ?uuid2 ?e2)]])

;;; VMS Rules

(def vms-rules
  '[;; -- Find the root application for a module, submodule, group, or subgroup
    [(app-root ?a ?g)
     [?sm :submodule/groups ?g]
     [?m :module/submodules ?sm]
     [?a :application/modules ?m]]

    [(app-root ?a ?s)
     (subgroup ?g ?s)
     [?sm :submodule/groups ?g]
     [?m :module/submodules ?sm]
     [?a :application/modules ?m]]

    ;; -- Recursive rule to find Submodule root for a subgroup
    [(submodule-root ?submodule ?subgroup)
     [?submodule :submodule/groups ?subgroup]]

    [(submodule-root ?submodule ?subgroup)
     (subgroup ?group ?subgroup)
     [?submodule :submodule/groups ?group]]

    ;; --  Recursive rules to find a group's subgroups
    [(subgroup ?g ?s)
     [?g :group/children ?s]]

    [(subgroup ?g ?s)
     [?g :group/children ?x]
     (subgroup ?x ?s)]

    ;; --  Recursive rule to find a submodule's groups
    [(group ?s ?g)
     [?s :submodule/groups ?g]]

    [(group ?s ?g)
     [?s :submodule/groups ?x]
     (subgroup ?x ?g)]

    ;; --  Submodule of a module
    [(submodule ?m ?s)
     [?m :module/submodules ?s]]

    ;; --  Application's modules
    [(module ?a ?m)
     [?e :application/modules ?m]]

    ;; -- Group's Group-Variables (with associated Variable entity)
    [(group-variable ?g ?gv ?v)
     [?g :group/group-variables ?gv]
     [?v :variable/group-variables ?gv]]

    ;; -- Entity's Input/Ouput (Group Variable, Group, Submodule)
    [(io ?e ?io)
     [?e :submodule/io ?io]]

    [(io ?e ?io) ;; Group's IO
     (group ?s ?e)
     [?s :submodule/io ?io]]

    [(io ?e ?io) ;; Group variable's IO
     (group-variable ?g ?e ?v)
     (group ?s ?g)
     [?s :submodule/io ?io]]

    ;; --  Find the root application for a module, submodule, group, or subgroup
    [(app-root ?a ?g)
     [?sm :submodule/groups ?g]
     [?m :module/submodules ?sm]
     [?a :application/modules ?m]]

    [(app-root ?a ?s)
     (subgroup ?g ?s)
     [?sm :submodule/groups ?g]
     [?m :module/submodules ?sm]
     [?a :application/modules ?m]]

    ;; -- Conditionals
    [(conditional-variable ?io ?g-uuid ?gv-uuid)
     (lookup ?gv-uuid ?gv)
     (group-variable ?g ?gv ?v)
     (io ?gv ?io)
     (lookup ?g-uuid ?g)]

    [(conditional ?io ?gc ?c ?g-uuid ?gv-uuid ?type ?op ?values)
     [?gc :group/conditionals ?c]
     [?c :conditional/group-variable-uuid ?gv-uuid]
     [?c :conditional/operator ?op]
     [?c :conditional/values ?values]
     [?c :conditional/type ?type]

     ;; get ?gv-uuid
     (group-variable ?g ?gv ?v)
     (lookup ?gv-uuid ?g)

     ;; get ?io
     (io ?gv ?io)

     ;; get ?g-uuid
     (lookup ?g-uuid ?g)]

    ;; -- Units
    [(units ?units-uuid ?unit)
     [?u :bp/uuid ?units-uuid]
     [?u :unit/cpp-enum-member-uuid ?em-uuid]
     [?em :bp/uuid ?em-uuid]
     [?em :cpp.enum-member/value ?unit]]

    ;; --  Language from short code
    [(language ?code ?l)
     [?l :language/shortcode ?code]]

    ;; --  Entity's translation key
    [(translation-key ?e ?k)
     [?e :application/translation-key ?k]]

    [(translation-key ?e ?k)
     [?e :module/translation-key ?k]]

    [(translation-key ?e ?k)
     [?e :submodule/translation-key ?k]]

    [(translation-key ?e ?k)
     [?e :group/translation-key ?k]]

    [(translation-key ?e ?k)
     [?e :group-variable/translation-key ?k]]

    ;; --  Entity's help key
    [(translation-key ?e ?k)
     [?e :application/help-key ?k]]

    [(translation-key ?e ?k)
     [?e :module/help-key ?k]]

    [(translation-key ?e ?k)
     [?e :submodule/help-key ?k]]

    [(translation-key ?e ?k)
     [?e :group/help-key ?k]]

    [(translation-key ?e ?k)
     [?e :group-variable/help-key ?k]]])

;;; Variable Rules

(def variable-rules
  '[;; -- Variable associated with Group Variable
    ;;    lookup can be performed with either Entity ID or a UUID
    [(variable ?gv ?v) ;; By Entity ID
     [?v :variable/group-variables ?gv]]

    [(variable ?gv-uuid ?v) ;; By UUID
     (lookup ?gv-uuid ?gv)
     [?v :variable/group-variables ?gv]]

    [(variable ?gv-uuid ?v) ;; Variable associated with Subtool Group
     (lookup ?gv-uuid ?gv)
     [?v :variable/subtool-variables ?gv]]

    ;; -- Group variable's function
    [(variable-fn ?gv-uuid ?fn)
     (ref ?gv-uuid :group-variable/cpp-function ?fn)]
    [(variable-fn ?gv ?fn)
     [?gv :group-variable/cpp-function ?fn]]

    [(variable-fn ?gv-uuid ?fn) ;; Subtool Variant
     (ref ?gv-uuid :subtool-variable/cpp-function-uuid ?fn)]
    [(variable-fn ?gv ?fn)
     [?gv :subtool-variable/cpp-function ?fn]]

    ;; -- Goup variable's parameter
    [(variable-param ?gv-uuid ?p)
     (ref ?uuid :group-variable/cpp-parameter ?p)]
    [(variable-param ?gv ?p)
     [?gv :group-variable/cpp-parameter ?p]]
    [(variable-param ?gv-uuid ?p)
     (ref ?uuid :subtool-variable/cpp-parameter ?p)]
    [(variable-param ?gv ?p)
     [?gv :subtool-variable/cpp-parameter ?p]]

    ;; -- Variable's UUID ---
    [(variable-uuid ?gv-uuid ?var-uuid)
     (variable ?gv-uuid ?v)
     [?v :bp/uuid ?var-uuid]]

    ;; -- Variable's units (for continuous variables)
    [(variable-units ?gv-uuid ?unit)
     (variable ?gv-uuid ?v)
     [?v :variable/kind :continuous]
     [?v :variable/native-unit-uuid ?units-uuid]
     (units ?units-uuid ?unit)]

    ;; -- Variable's native unit UUID (for continuous variables)
    [(variable-native-units-uuid ?gv-uuid ?units-uuid)
     (variable ?gv-uuid ?v)
     [?v :variable/kind :continuous]
     [?v :variable/native-unit-uuid ?units-uuid]]

    ;; -- Variable's kind
    [(variable-kind ?v ?kind)
     [?v :variable/kind ?kind]]

    [(variable-kind ?gv-uuid ?kind)
     (variable ?gv-uuid ?v)
     [?v :variable/kind ?kind]]])

;;; CPP Rules

(def cpp-rules
  "Rules related to the CPP operations"

  '[;; -- Find a subtool's compute function
    [(subtool-compute-fn ?uuid ?fn)
     (ref ?uuid :subtool/cpp-function-uuid ?fn)]

    ;; -- Parameter's attributes
    [(param-attrs ?p ?p-name ?p-type ?p-order)
     [?p :cpp.parameter/name ?p-name]
     [?p :cpp.parameter/type ?p-type]
     [?p :cpp.parameter/order ?p-order]]

    ;; -- Find the function's parameters and attributes
    [(fn-params ?fn ?p ?p-name ?p-type ?p-order)
     [?fn :cpp.function/parameter ?p]
     (param-attrs ?p ?p-name ?p-type ?p-order)]

    ;; -- Finds all output variables for module
    [(module-output-vars ?m ?gv)
     [?m :module/submodules ?s]
     (io ?s :output)
     (group ?s ?g)
     [?g :group/group-variables ?gv]]

    ;; -- Finds all output variables and related CPP functions for module
    [(module-output-fns ?m ?fn ?fn-name)
     (module-output-vars ?m ?gv)
     (lookup ?uuid ?gv)
     (var->fn ?uuid ?fn)
     [?fn :cpp.function/name ?fn-name]]

    ;; -- Finds all input variables for module
    [(module-input-vars ?m ?gv)
     [?m :module/submodules ?s]
     [?s :submodule/io :input]
     [?s :submodule/groups ?g]
     [?g :group/group-variables ?gv]]

    ;; -- Finds all input variables and related CPP functions for module
    [(module-input-fns ?m ?fn ?fn-name)
     (module-input-vars ?m ?gv)
     (lookup ?uuid ?gv)
     (var->fn ?uuid ?fn)
     [?fn :cpp.function/name ?fn-name]]

    ;; -- Find the function's parameters
    [(fn-params ?fn ?p ?p-name ?p-type ?p-order)
     [?fn :cpp.function/parameters ?p]
     (param-attrs ?p ?p-name ?p-type ?p-order)]

    ;; -- CPP Relations
    [(cpp-enum ?n ?e)
     [?n :cpp.namespace/enum ?e]]

    [(cpp-enum-member ?e ?m ?v)
     [?e :cpp.enum/enum-member ?m]
     [?m :cpp.enum-member/value ?v]]

    [(cpp-class ?ns ?class)
     [?ns :cpp.namespace/class ?class]]

    [(cpp-fn ?class ?fn)
     [?class :cpp.class/function ?fn]]

    [(cpp-param ?fn ?param)
     [?f :cpp.function/parameter ?p]]

    ;; -- CPP Names
    [(cpp-name ?e ?name)
     [?e :cpp.namespace/name ?name]]

    [(cpp-name ?e ?name)
     [?e :cpp.class/name ?name]]

    [(cpp-name ?e ?name)
     [?e :cpp.function/name ?name]]

    [(cpp-name ?e ?name)
     [?e :cpp.parameter/name ?name]]])

;;; All Rules

(def all-rules
  (concat uuid-rules vms-rules variable-rules cpp-rules))

(def rule-names
  (->> all-rules
       (map ffirst)
       (set)
       (mapv keyword)
       (sort)))
