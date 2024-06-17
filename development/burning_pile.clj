(ns burning-pile)

(do
  (require '[behave-cms.server        :as cms]
           '[datomic.api              :as d]
           '[datomic-store.main       :as ds]
           '[datascript.core          :refer [squuid]]
           '[behave.schema.core       :refer [rules]]
           '[schema-migrate.interface :as sm]
           '[nano-id.core             :refer [nano-id]])

  (cms/init-db!)

  (def ^:private rand-uuid (comp str squuid))

  (def ^:private db (d/db @ds/datomic-conn))

  (defn- uuid->id [uuid]
    (d/q '[:find ?e .
           :in $ ?uuid
           :where [?e :bp/uuid ?uuid]]
         db uuid))

  (def ^:private surface-outputs
    (d/q
     '[:find [?uuid ...]
       :in $ %
       :where
       [?m :module/name "Surface"]
       [?m :module/submodules ?sm]
       [?sm :submodule/io :output]
       (group ?sm ?g)
       (group-variable ?g ?gv ?v)
       #_[?v :variable/kind :discrete]
       (or
        [(missing? $ ?gv :group-variable/research?)]
        [?gv :group-variable/research? false])
       (or
        [(missing? $ ?gv :group-variable/conditionally-set?)]
        [?gv :group-variable/conditionally-set? false])
       [?gv :group-variable/translation-key ?t]
       (not [?gv :group-variable/translation-key "behaveplus:surface:output:spot:maximum_spotting_distance:burning_pile"])
       [?gv :bp/uuid ?uuid]]
     db rules))

  (def ^:private burning-pile-uuid
    (sm/t-key->uuid db "behaveplus:surface:output:spot:maximum_spotting_distance:burning_pile"))

  (defn- disable-surface-outputs-action [output-uuid]
    {:db/id (uuid->id output-uuid)
     :group-variable/actions
     [{:bp/uuid (rand-uuid)
       :bp/nid  (nano-id)

       :action/name
       "Disable when Spot > Burning Pile is selected."

       :action/type
       :disable

       :action/conditionals-operator
       :or

       :action/conditionals
       [{:bp/uuid                         (rand-uuid)
         :bp/nid                          (nano-id)
         :conditional/group-variable-uuid burning-pile-uuid
         :conditional/type                :group-variable
         :conditional/operator            :equal
         :conditional/values              "true"}]}]})
  
  (def ^:private surface-disable-actions-tx (mapv disable-surface-outputs-action surface-outputs))

  (d/transact @ds/datomic-conn surface-disable-actions-tx)

  ;; Burning Pile Group Variable

  (def ^:private burning-pile-output-action-tx
    {:bp/uuid burning-pile-uuid
     :group-variable/actions
     [{:bp/uuid (rand-uuid)
       :bp/nid  (nano-id)
       
       :action/name
       "Disable when Surface Outputs are selected."

       :action/type
       :disable

       :action/conditionals-operator
       :or

       :action/conditionals
       (mapv
        (fn [gv-uuid] 
          {:bp/uuid                         (rand-uuid)
           :bp/nid                          (nano-id)
           :conditional/group-variable-uuid gv-uuid
           :conditional/type                :group-variable
           :conditional/operator            :equal
           :conditional/values              "true"})
        surface-outputs)}]})

  (d/pull db '[*] [:bp/uuid burning-pile-uuid])

  ;; Add action
  (d/transact @ds/datomic-conn [burning-pile-output-action-tx]))
