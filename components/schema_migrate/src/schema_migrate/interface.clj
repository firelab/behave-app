(ns schema-migrate.interface
  (:require [schema-migrate.core :as c]))

(def ^{:arglists '([conn attr nname])
       :doc      "Get the :bp/uuid using the name for the specified name attribute"}
  name->uuid c/name->uuid)

(def ^{:arglists '([conn attr nname])
       :doc      "Get the :bp/nid using the name for the specified name attribute"}
  name->nid c/name->nid)

(def ^{:arglists '([conn attr nname])
       :doc      "Get the datomic entity id given the attribute and name. May not work as expected
                  if attr and name is not unique."}
  name->eid c/name->eid)

(def ^{:arglists '([conn nname])
       :doc      "Get the :bp/uuid using the cpp namepsace name"}
  cpp-ns->uuid c/cpp-ns->uuid)

(def ^{:arglists '([conn nnamespace cclass])
       :doc      "Get the :bp/uuid using the cpp class name"}
  cpp-class->uuid c/cpp-class->uuid)

(def ^{:arglists '([conn nnamespace cclass fn-name])
       :doc      "Get the :bp/uuid using the cpp function name"}
  cpp-fn->uuid c/cpp-fn->uuid)

(def ^{:arglists '([conn m])
       :doc      "Given a map of with the names of a namespace, class and function, return a map
                   that resolves the names to a uuid. Requires all three names."}
  cpp-uuids c/cpp-uuids)

(def ^{:arglists '([conn nnamespace cclass fn-name param-name])
       :doc      "Get the :bp/uuid using the cpp function name and parameter name."}
  cpp-param->uuid c/cpp-param->uuid)

(def ^{:arglists '([conn eid])
       :doc      "Returns an entity's translation key."}
  eid->t-key c/eid->t-key)

(def ^{:arglists '([conn t])
       :doc      "Get the :bp/uuid using translation-key"}
  t-key->uuid c/t-key->uuid)

(def ^{:arglists '([conn t])
       :doc      "Get the datomic entity using translation-key"}
  t-key->entity c/t-key->entity)

(def ^{:arglists '([conn t])
       :doc      "Get the :db/id using translation-key"}
  t-key->eid c/t-key->eid)

(def ^{:arglists '([conn t])
       :doc      "Given a translation-key of a group-variable an action's name return the action's entity id"}
  t-key-action-name->eid c/t-key-action-name->eid)

(def ^{:arglists '([conn attr])
       :doc      "Sets :db/isComponent true for a given schema attribute.
                  Takes a datahike conn."}
  make-attr-is-component! c/make-attr-is-component!)

(def ^{:arglists '([conn attr])
       :doc      "Returns the payload for making a schema attribute a \"Component\""}
  make-attr-is-component-payload c/make-attr-is-component-payload)

(def ^{:arglists '([conn tx])
       :doc      "Given a transaction ID or a transaction result (return from datomic.api/transact),
                  Reassert retracted datoms and retract asserted datoms in a transaction,
                  effectively \"undoing\" the transaction."}
  rollback-tx! c/rollback-tx!)

(def ^{:arglists '([data])
       :doc      "Postwalk over data and insert bp/uuid and bp/nid into every map form"}
  postwalk-insert c/postwalk-insert)

(def ^{:arglists '([conn eid-start t-key->translation-map])
       :doc      "Given a map of translation-key to it's translation create a payload that creates these
                  translation entities as well as adding these refs to the exisitng Enlgish language
                  entity. `eid-start` is optional and will be used as the starting eid for the
                  translation entities (prevents eid overlap if you wish to include this payload in
                  the same transaction where you've manually assigned :db/id)."}
  build-translations-payload c/build-translations-payload)

(def ^{:arglists '([conn eid-start t-key->translation-map])
       :doc      "Creates a payload to update existing translations for specific language shortcode."}
  update-translations-payload c/update-translations-payload)

(def ^{:arglists '([conn t-key])
       :doc      "Removes an entity's (and it's components) translation keys."}
  remove-nested-i18ns-tx c/remove-nested-i18ns-tx)

(def ^{:arglists '([data])
       :doc "Payload for a new Entity, which adds a :bp/nid and :bp/uuid."}
  ->entity c/->entity)

(def ^{:arglists '([uuid operator value])
       :doc "Payload for a Group Variable Conditional."}
  ->gv-conditional c/->gv-conditional)

(def ^{:arglists '([operator values])
       :doc "Payload for a Module Conditional."}
  ->module-conditional c/->module-conditional)

(def ^{:arglists '([conn params])
       :doc      "Payload for a Conditional."}
  ->conditional c/->conditional)

(def ^{:arglists '([conn params])
       :doc "Payload for a new Group."}
  ->group c/->group)

(def ^{:arglists '([conn params])
       :doc      "Payload for an Action."}
  ->action c/->action)

(def ^{:arglists '([conn params])
       :doc      "Payload for a new Variable."}
  ->variable c/->variable)

(def ^{:arglists '([conn params])
       :doc "Payload for a new Group Variable."}
  ->group-variable c/->group-variable)

(def ^{:arglists '([source-eid destination-eid])
       :doc "Payload for a new Link."}
  ->link c/->link)

(def ^{:arglists '([migration-name])
       :->actiondoc "Payload for a new migration."}
  ->migration c/->migration)

(def ^{:arglists '([s])
       :doc      "add behaveplus: to `s`"}
  bp c/bp)
