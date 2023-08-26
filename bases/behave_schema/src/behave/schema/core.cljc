(ns behave.schema.core
  (:require [behave.schema.application      :as application]
            [behave.schema.conditionals     :as conditionals]
            [behave.schema.group            :as group]
            [behave.schema.group-variable   :as group-variable]
            [behave.schema.help-page        :as help-page]
            [behave.schema.language         :as language]
            [behave.schema.list             :as behave-list]
            [behave.schema.link             :as link]
            [behave.schema.module           :as module]
            [behave.schema.submodule        :as submodule]
            [behave.schema.translation      :as translation]
            [behave.schema.tool             :as tool]
            [behave.schema.subtool          :as subtool]
            [behave.schema.subtool-variable :as subtool-variable]
            [behave.schema.user             :as user]
            [behave.schema.unit             :as unit]
            [behave.schema.variable         :as variable]
            [behave.schema.worksheet        :as worksheet]
            [behave.schema.cpp.class        :as cpp-class]
            [behave.schema.cpp.enum         :as cpp-enum]
            [behave.schema.cpp.enum-member  :as cpp-enum-member]
            [behave.schema.cpp.function     :as cpp-function]
            [behave.schema.cpp.namespace    :as cpp-namespace]
            [behave.schema.cpp.parameter    :as cpp-parameter]))

(def uuid-schema [{:db/ident       :bp/uuid
                   :db/doc         "UUID of entity"
                   :db/valueType   :db.type/string
                   :db/unique      :db.unique/identity
                   :db/cardinality :db.cardinality/one
                   :db/index       true}])

(def all-schemas (apply concat [uuid-schema
                                application/schema
                                behave-list/schema
                                conditionals/schema
                                group-variable/schema
                                group/schema
                                help-page/schema
                                language/schema
                                link/schema
                                module/schema
                                submodule/schema
                                subtool-variable/schema
                                subtool/schema
                                tool/schema
                                translation/schema
                                unit/schema
                                user/schema
                                variable/schema
                                worksheet/schema

                                ;; CPP
                                cpp-class/schema
                                cpp-enum-member/schema
                                cpp-enum/schema
                                cpp-function/schema
                                cpp-namespace/schema
                                cpp-parameter/schema]))
