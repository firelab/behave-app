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
