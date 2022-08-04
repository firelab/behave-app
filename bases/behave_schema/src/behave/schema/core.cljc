(ns behave.schema.core
  (:require [behave.schema.application     :as application]
            [behave.schema.group           :as group]
            [behave.schema.help            :as help]
            [behave.schema.language        :as language]
            [behave.schema.module          :as module]
            [behave.schema.submodule       :as submodule]
            [behave.schema.translation     :as translation]
            [behave.schema.user            :as user]
            [behave.schema.variable        :as variable]
            [behave.schema.worksheet       :as worksheet]
            [behave.schema.cpp.class       :as cpp-class]
            [behave.schema.cpp.enum        :as cpp-enum]
            [behave.schema.cpp.enum-member :as cpp-enum-member]
            [behave.schema.cpp.function    :as cpp-function]
            [behave.schema.cpp.namespace   :as cpp-namespace]
            [behave.schema.cpp.parameter   :as cpp-parameter]))

(def all-schemas (apply concat [application/schema
                                group/schema
                                help/schema
                                language/schema
                                module/schema
                                submodule/schema
                                translation/schema
                                user/schema
                                variable/schema
                                worksheet/schema
                                cpp-class/schema
                                cpp-enum/schema
                                cpp-enum-member/schema
                                cpp-function/schema
                                cpp-namespace/schema
                                cpp-parameter/schema]))
