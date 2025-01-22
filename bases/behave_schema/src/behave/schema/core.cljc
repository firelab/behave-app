(ns behave.schema.core
  (:require [behave.schema.actions             :as actions]
            [behave.schema.application         :as application]
            [behave.schema.conditionals        :as conditionals]
            [behave.schema.cpp.class           :as cpp-class]
            [behave.schema.cpp.enum            :as cpp-enum]
            [behave.schema.cpp.enum-member     :as cpp-enum-member]
            [behave.schema.cpp.function        :as cpp-function]
            [behave.schema.cpp.namespace       :as cpp-namespace]
            [behave.schema.cpp.parameter       :as cpp-parameter]
            [behave.schema.diagrams            :as diagrams]
            [behave.schema.domain              :as domain]
            [behave.schema.group               :as group]
            [behave.schema.group-variable      :as group-variable]
            [behave.schema.help-page           :as help-page]
            [behave.schema.language            :as language]
            [behave.schema.link                :as link]
            [behave.schema.list                :as behave-list]
            [behave.schema.module              :as module]
            [behave.schema.pivot-table         :as pivot-table]
            [behave.schema.prioritized-results :as prioritized-results]
            [behave.schema.rules               :as r]
            [behave.schema.submodule           :as submodule]
            [behave.schema.subtool             :as subtool]
            [behave.schema.subtool-variable    :as subtool-variable]
            [behave.schema.tool                :as tool]
            [behave.schema.translation         :as translation]
            [behave.schema.unit                :as unit]
            [behave.schema.user                :as user]
            [behave.schema.variable            :as variable]
            [behave.schema.worksheet           :as worksheet]))

(def uuid-schema         [{:db/ident       :bp/uuid
                           :db/doc         "UUID of entity"
                           :db/valueType   :db.type/string
                           :db/unique      :db.unique/identity
                           :db/cardinality :db.cardinality/one
                           :db/index       true}])

(def nano-id-schema      [{:db/ident       :bp/nid
                           :db/doc         "Nano-ID of entity"
                           :db/valueType   :db.type/string
                           :db/unique      :db.unique/identity
                           :db/cardinality :db.cardinality/one
                           :db/index       true}])

(def migration-id-schema [{:db/ident       :bp/migration-id
                           :db/doc         "Migration Identifier"
                           :db/valueType   :db.type/string
                           :db/unique      :db.unique/identity
                           :db/cardinality :db.cardinality/one
                           :db/index       true}])

(def ^{:doc "Datalog Rules for VMS, CPP, and Worksheets"}
  rules r/all-rules)

(def all-schemas (apply concat [uuid-schema
                                nano-id-schema
                                migration-id-schema
                                application/schema
                                actions/schema
                                behave-list/schema
                                conditionals/schema
                                domain/schema
                                diagrams/schema
                                group/schema
                                group-variable/schema
                                group/schema
                                help-page/schema
                                language/schema
                                link/schema
                                module/schema
                                pivot-table/schema
                                submodule/schema
                                subtool-variable/schema
                                subtool/schema
                                tool/schema
                                translation/schema
                                unit/schema
                                user/schema
                                variable/schema
                                worksheet/schema
                                prioritized-results/schema

                                ;; CPP
                                cpp-class/schema
                                cpp-enum-member/schema
                                cpp-enum/schema
                                cpp-function/schema
                                cpp-namespace/schema
                                cpp-parameter/schema]))
