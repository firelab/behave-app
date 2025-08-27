(ns behave-cms.queries
  (:require [clojure.string :as str]
            #?(:cljs [datascript.core :as d]
               :clj  [datahike.api :as d])))

(def rules
  '[[(module ?a ?m) [?e :application/modules ?m]]
    [(submodule ?m ?s) [?m :module/submodules ?s]]
    [(group ?s ?g) [?m :submodule/groups ?s]]
    [(subgroup ?g ?sg) [?g :group/children ?sg]]
    [(variable ?g ?v) [?g :group/variables ?v]]
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

    ;; Find the root application for a module, submodule, group, subgroup, group variable, or search table
    [(app-root ?a ?g)
     [?sm :submodule/groups ?g]
     [?m :module/submodules ?sm]
     [?a :application/modules ?m]]

    [(app-root ?a ?s)
     [?g :group/group-variables ?s]
     [?sm :submodule/groups ?g]
     [?m :module/submodules ?sm]
     [?a :application/modules ?m]]

    [(app-root ?a ?s)
     [?m :module/search-tables ?s]
     [?m :module/submodules ?sm]
     [?a :application/modules ?m]]

    [(app-root ?a ?s)
     (subgroup ?g ?s)
     [?sm :submodule/groups ?g]
     [?m :module/submodules ?sm]
     [?a :application/modules ?m]]

    [(app-root ?a ?s)
     [?m :module/submodules ?s]
     [?a :application/modules ?m]]])
