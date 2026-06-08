(ns schema-migrate.interface
  (:require [schema-migrate.core   :as c]
            [schema-migrate.runner :as r]))

(def ^{:arglists '([db attr nname])
       :doc      "Get the :bp/uuid using the name for the specified name attribute. Accepts a Datomic conn or db."}
  name->uuid c/name->uuid)

(def ^{:arglists '([db attr nname])
       :doc      "Get the :bp/nid using the name for the specified name attribute. Accepts a Datomic conn or db."}
  name->nid c/name->nid)

(def ^{:arglists '([db attr nname])
       :doc      "Get the datomic entity id given the attribute and name. May not work as expected
                  if attr and name is not unique. Accepts a Datomic conn or db."}
  name->eid c/name->eid)

(def ^{:arglists '([db nname])
       :doc      "Get the :bp/uuid using the cpp namepsace name. Accepts a Datomic conn or db."}
  cpp-ns->uuid c/cpp-ns->uuid)

(def ^{:arglists '([db nnamespace cclass])
       :doc      "Get the :bp/uuid using the cpp class name. Accepts a Datomic conn or db."}
  cpp-class->uuid c/cpp-class->uuid)

(def ^{:arglists '([db nnamespace cclass fn-name])
       :doc      "Get the :bp/uuid using the cpp function name. Accepts a Datomic conn or db."}
  cpp-fn->uuid c/cpp-fn->uuid)

(def ^{:arglists '([db m])
       :doc      "Given a map of with the names of a namespace, class and function, return a map
                   that resolves the names to a uuid. Requires all three names. Accepts a Datomic conn or db."}
  cpp-uuids c/cpp-uuids)

(def ^{:arglists '([db nnamespace cclass fn-name param-name])
       :doc      "Get the :bp/uuid using the cpp function name and parameter name. Accepts a Datomic conn or db."}
  cpp-param->uuid c/cpp-param->uuid)

(def ^{:arglists '([db eid])
       :doc      "Returns an entity's translation key. Accepts a Datomic conn or db."}
  eid->t-key c/eid->t-key)

(def ^{:arglists '([db t])
       :doc      "Get the :bp/uuid using translation-key. Accepts a Datomic conn or db."}
  t-key->uuid c/t-key->uuid)

(def ^{:arglists '([db t])
       :doc      "Get the datomic entity using translation-key. Accepts a Datomic conn or db."}
  t-key->entity c/t-key->entity)

(def ^{:arglists '([db t])
       :doc      "Get the :db/id using translation-key. Accepts a Datomic conn or db."}
  t-key->eid c/t-key->eid)

(def ^{:arglists '([db t])
       :doc      "Get the :bp/uuid using translation-key. Accepts a Datomic conn or db."}
  t-key->bp-uuid c/t-key->bp-uuid)

(def ^{:arglists '([db t])
       :doc      "Given a translation-key of a group-variable an action's name return the action's entity id. Accepts a Datomic conn or db."}
  t-key-action-name->eid c/t-key-action-name->eid)

(def ^{:arglists '([db bp6-code])
       :doc      "Given a Behave6 Variable code (e.g. `vSurfaceFuelCode`), returns the matching variable entity ID. Accepts a Datomic conn or db."}
  bp6-code->variable-eid c/bp6-code->variable-eid)

(def ^{:arglists '([db src-t-key dst-t-key])
       :doc      "Find the link entity connecting two group-variables by
                  translation key. Returns the :db/id or nil. Links may also
                  carry conditional attrs (:conditional/type, :operator,
                  :values). Accepts a Datomic conn or db."}
  find-link c/find-link)

(def ^{:arglists '([conn attr])
       :doc      "Sets :db/isComponent true for a given schema attribute.
                  Takes a datahike conn."}
  make-attr-is-component! c/make-attr-is-component!)

(def ^{:arglists '([db attr])
       :doc      "Returns the payload for making a schema attribute a \"Component\". Accepts a Datomic conn or db."}
  make-attr-is-component-payload c/make-attr-is-component-payload)

(def ^{:arglists '([conn tx])
       :doc      "Given a transaction ID or a transaction result (return from datomic.api/transact),
                  Reassert retracted datoms and retract asserted datoms in a transaction,
                  effectively \"undoing\" the transaction."}
  rollback-tx! c/rollback-tx!)

(def ^{:arglists '([data])
       :doc      "Postwalk over data and insert bp/uuid and bp/nid into every map form"}
  postwalk-insert c/postwalk-insert)

(def ^{:arglists '([db t-key->translation-map] [db eid-start t-key->translation-map])
       :doc      "Given a map of translation-key to it's translation create a payload that creates these
                  translation entities as well as adding these refs to the exisitng Enlgish language
                  entity. `eid-start` is optional and will be used as the starting eid for the
                  translation entities (prevents eid overlap if you wish to include this payload in
                  the same transaction where you've manually assigned :db/id). Accepts a Datomic conn or db."}
  build-translations-payload c/build-translations-payload)

(def ^{:arglists '([db shortcode t-key->translation-map])
       :doc      "Creates a payload to update existing translations for specific language shortcode. Accepts a Datomic conn or db."}
  update-translations-payload c/update-translations-payload)

(def ^{:arglists '([db t-key])
       :doc      "Removes an entity's (and it's components) translation keys. Accepts a Datomic conn or db."}
  remove-nested-i18ns-tx c/remove-nested-i18ns-tx)

(def ^{:arglists '([data])
       :doc      "Payload for a new Entity, which adds a :bp/nid and :bp/uuid."}
  ->entity c/->entity)

(def ^{:arglists '([uuid operator value])
       :doc      "Payload for a Group Variable Conditional."}
  ->gv-conditional c/->gv-conditional)

(def ^{:arglists '([operator values])
       :doc      "Payload for a Module Conditional."}
  ->module-conditional c/->module-conditional)

(def ^{:arglists '([db params])
       :doc      "Payload for a Conditional. Accepts a Datomic conn or db."}
  ->conditional c/->conditional)

(def ^{:arglists '([db params])
       :doc      "Payload for a new Group. Accepts a Datomic conn or db."}
  ->group c/->group)

(def ^{:arglists '([db params])
       :doc      "Payload for an Action. Accepts a Datomic conn or db."}
  ->action c/->action)

(def ^{:arglists '([db params])
       :doc      "Payload for a new Variable. Accepts a Datomic conn or db."}
  ->variable c/->variable)

(def ^{:arglists '([db params])
       :doc      "Payload for a new Group Variable. Accepts a Datomic conn or db."}
  ->group-variable c/->group-variable)

(def ^{:arglists '([source-eid destination-eid])
       :doc      "Payload for a new Link."}
  ->link c/->link)

(def ^{:arglists '([migration-name])
       :doc      "Payload for a new migration."}
  ->migration c/->migration)

(def ^{:arglists '([s])
       :doc      "add behaveplus: to `s`"}
  bp c/bp)

(def ^{:arglists '([conn dir])
       :doc      "Run all pending migrations found in `dir`. Skips namespaces
                  with `^{:migrate/ignore? true}` metadata and migrations
                  already recorded via `:bp/migration-id`. Halts on failure."}
  run-pending-migrations! r/run-pending-migrations!)
