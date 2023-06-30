(ns behave.vms.rules)

(def rules
    '[[(module ?a ?m) [?e :application/modules ?m]]
      [(submodule ?m ?s) [?m :module/submodules ?s]]

      ;;subgroup
      [(subgroup ?g ?s) [?g :group/children ?s]]
      [(subgroup ?g ?s)
       [?g :group/children ?x]
       (subgroup ?x ?s)]

      ;;group
      [(group ?s ?g) [?s :submodule/groups ?g]]
      [(group ?s ?g)
       [?s :submodule/groups ?x]
       (subgroup ?x ?g)]

      [(variable ?g ?v) [?g :group/group-variables ?v]]
      [(language ?code ?l) [?l :language/shortcode ?code]]
      [(translation ?k ?t) [?t :translation/key ?k]]

      ;; Find the root application for a module, submodule, group, or subgroup
      [(app-root ?a ?g)
       [?sm :submodule/groups ?g]
       [?m :module/submodules ?sm]
       [?a :application/modules ?m]]

      [(app-root ?a ?s)
       (subgroup ?g ?s)
       [?sm :submodule/groups ?g]
       [?m :module/submodules ?sm]
       [?a :application/modules ?m]]

      [(io ?gv ?io)
       (variable ?g ?gv)
       (group ?s ?g)
       [?s :submodule/io ?io]]

      [(conditonal ?io ?gc ?g-uuid ?gv-uuid ?op ?values)
       [?gc :group/conditionals ?c]
       [?c :conditional/group-variable-uuid ?gv-uuid]
       [?c :conditional/operator ?op]
       [?c :conditional/values ?values]

       ;; get ?gv-uuid
       (variable ?g ?gv)
       [?gv :bp/uuid ?gv-uuid]

       ;; get ?io
       (io ?gv ?io)

       ;; get ?g-uuid
       [?g :bp/uuid ?g-uuid]
       ]])
