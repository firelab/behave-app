(ns schema-migrate.interface
  (:require [schema-migrate.core :as c]))

(def ^{:argslist '([conn attr nname])
       :doc      "Get the :bp/uuid using the name for the specified name attribute"}
  name->uuid c/name->uuid)

(def ^{:argslist '([conn attr nname])
       :doc      "Get the :bp/nid using the name for the specified name attribute"}
  name->nid c/name->nid)

(def ^{:argslist '([conn nname])
       :doc      "Get the :bp/uuid using the cpp namepsace name"}
  cpp-ns->uuid c/cpp-ns->uuid)

(def ^{:argslist '([conn nname])
       :doc      "Get the :bp/uuid using the cpp class name"}
  cpp-class->uuid c/cpp-class->uuid)

(def ^{:argslist '([conn nname])
       :doc      "Get the :bp/uuid using the cpp function name"}
  cpp-fn->uuid c/cpp-fn->uuid)

(def ^{:argslist '([conn t])
       :doc      "Get the :bp/uuid using translation-key"}
  t-key->uuid c/t-key->uuid)

(def ^{:argslist '([conn t])
       :doc      "Get the datomic entity using translation-key"}
  t-key->enitty c/t-key->entity)

(def ^{:argslist '([conn t])
       :doc      "Get the :db/id using translation-key"}
  t-key->eid c/t-key->eid)

(def ^{:argslist '([conn attr])
       :doc      "Walk through every element in data and insert :bp/uuid and :bp/nid"}
  postwalk-assoc-uuid+nid c/postwalk-assoc-uuid+nid)

(def ^{:argslist '([conn attr])
       :doc      "Sets :db/isComponent true for a given schema attribute.
                  Takes a datahike conn."}
  make-attr-is-component! c/make-attr-is-component!)

(def ^{:argslist '([conn attr])
       :doc      "Returns the payload for making a schema attribute a \"Component\""}
  make-attr-is-component-payload c/make-attr-is-component-payload)

(def ^{:argslist '([conn attr])
       :doc      "Sets :db/isComponent true for a given schema attribute.
                  Takes a datahike conn."}
  make-attr-is-component! c/make-attr-is-component!)

(def ^{:argslist '([conn tx])
       :doc      "Given a transaction ID or a transaction result (return from datomic.api/transact),
                  Reassert retracted datoms and retract asserted datoms in a transaction,
                  effectively \"undoing\" the transaction."}
  rollback-tx! c/rollback-tx!)
