---
name: vms-migrations
description: VMS (Variable Management System) migration and data manipulation reference for BehavePlus 7. Covers entity hierarchy, Datomic attribute names, schema_migrate helpers, migration patterns, and common recipes for adding/modifying VMS entities.
triggers:
  - writing a VMS migration
  - adding or modifying VMS entities (Application, Module, Submodule, Group, GroupVariable, Variable, List, Tag, Domain, Conditional, Action)
  - importing C++ class/function definitions into the CMS
  - manipulating VMS data programmatically via Datomic
---

# VMS Migrations & Data Manipulation

## VMS Entity Hierarchy

```
Application
└── modules → [Module] (many refs)
    └── submodules → [Submodule] (many component refs)
        ├── conditionals → [Conditional] (controls visibility)
        └── groups → [Group] (many component refs)
            ├── conditionals → [Conditional] (controls visibility)
            ├── children → [Group] (nested sub-groups, component refs)
            └── group-variables → [GroupVariable] (many component refs)
                ├── actions → [Action] (component refs)
                └── ← variable ← [Variable] (shared, many GVs can ref one Variable)

Variable
├── list → List (discrete variables only)
│   └── options → [ListOption]
│       └── tag-refs → [Tag] (filtering)
├── domain-uuid → Domain
│   └── native/english/metric-unit-uuid → Unit
└── dimension-uuid → Dimension
    └── units → [Unit]

TagSet
└── tags → [Tag] (component refs)

List
├── tag-set → TagSet
└── color-tag-set → TagSet (deprecated)
```

**Key facts:**
- `isComponent true` means the child is owned by the parent — retracting the parent cascades to children.
- `Variable` is shared across many `GroupVariable`s; GroupVariable is owned by a Group.
- Translation keys (`*-translation-key`) are globally unique strings — they are how entities are looked up by `t-key->eid`.
- Every entity needs `:bp/uuid` (UUID string) and `:bp/nid` (nano-id) — use `postwalk-insert` or `->entity` to inject these automatically.

---

## Datomic Attribute Reference

### Application
| Attribute | Type | Notes |
|-----------|------|-------|
| `:application/uuid` | string, unique identity | |
| `:application/name` | string | |
| `:application/translation-key` | string, unique | |
| `:application/help-key` | string, unique | |
| `:application/modules` | ref, many | |
| `:application/version-major/minor/patch` | long | |
| `:application/tools` | ref, many | |

### Module
| Attribute | Type | Notes |
|-----------|------|-------|
| `:module/uuid` | string, unique identity | |
| `:module/name` | string | |
| `:module/order` | long (>= 0) | display order |
| `:module/translation-key` | string, unique | |
| `:module/help-key` | string, unique | |
| `:module/submodules` | ref, many, isComponent | |
| `:module/diagrams` | ref, many | |

### Submodule
| Attribute | Type | Notes |
|-----------|------|-------|
| `:submodule/uuid` | string, unique identity | |
| `:submodule/name` | string | |
| `:submodule/io` | keyword | `:input` or `:output` |
| `:submodule/order` | long | |
| `:submodule/translation-key` | string | |
| `:submodule/help-key` | string | |
| `:submodule/groups` | ref, many, isComponent | |
| `:submodule/conditionals` | ref, many, isComponent | |
| `:submodule/conditionals-operator` | keyword | `:and` or `:or` |
| `:submodule/research?` | boolean | |

### Group
| Attribute | Type | Notes |
|-----------|------|-------|
| `:group/uuid` | string, unique identity | |
| `:group/name` | string | |
| `:group/order` | long | |
| `:group/translation-key` | string, unique | |
| `:group/result-translation-key` | string, unique | results page label |
| `:group/help-key` | string, unique | |
| `:group/group-variables` | ref, many, isComponent | |
| `:group/children` | ref, many, isComponent | nested sub-groups |
| `:group/conditionals` | ref, many, isComponent | |
| `:group/conditionals-operator` | keyword | `:and` or `:or` |
| `:group/repeat?` | boolean | repeating group |
| `:group/max-repeat` | long | |
| `:group/single-select?` | boolean | mutually exclusive GVs |
| `:group/research?` | boolean | |

### GroupVariable
| Attribute | Type | Notes |
|-----------|------|-------|
| `:group-variable/uuid` | string, unique identity | |
| `:group-variable/order` | long | |
| `:group-variable/translation-key` | string, unique | |
| `:group-variable/result-translation-key` | string, unique | |
| `:group-variable/help-key` | string, unique | |
| `:group-variable/cpp-namespace` | string | e.g. `"global"` |
| `:group-variable/cpp-class` | string | e.g. `"SIGSurface"` |
| `:group-variable/cpp-function` | string | e.g. `"getFlameLength"` |
| `:group-variable/cpp-parameter` | string | optional, for inputs |
| `:group-variable/actions` | ref, many, isComponent | |
| `:group-variable/direction` | keyword | `:heading`, `:backing`, `:flanking` |
| `:group-variable/discrete-multiple?` | boolean | multi-select discrete |
| `:group-variable/conditionally-set?` | boolean | hidden, set programmatically |
| `:group-variable/hide-result?` | boolean | |
| `:group-variable/hide-csv?` | boolean | |
| `:group-variable/research?` | boolean | |

### Variable
| Attribute | Type | Notes |
|-----------|------|-------|
| `:variable/uuid` | string, unique identity | |
| `:variable/name` | string, indexed | |
| `:variable/kind` | keyword | `:continuous`, `:discrete`, `:text` |
| `:variable/order` | long | |
| `:variable/translation-key` | string, unique | |
| `:variable/help-key` | string, unique | |
| `:variable/list` | ref, one | discrete variables only |
| `:variable/domain-uuid` | string | UUID of Domain |
| `:variable/dimension-uuid` | string | UUID of Dimension |
| `:variable/english-unit-uuid` | string | UUID of Unit |
| `:variable/metric-unit-uuid` | string | UUID of Unit |
| `:variable/native-unit-uuid` | string | UUID of Unit |
| `:variable/minimum` | double | continuous only |
| `:variable/maximum` | double | continuous only |
| `:variable/default-value` | double | continuous only |
| `:variable/english-decimals` | long | |
| `:variable/metric-decimals` | long | |
| `:variable/native-decimals` | long | |
| `:variable/bp6-label` | string | legacy BehavePlus 6 name |
| `:variable/bp6-code` | string | legacy BehavePlus 6 code |

### List / ListOption
| Attribute | Type | Notes |
|-----------|------|-------|
| `:list/uuid` | string, unique identity | |
| `:list/name` | string | |
| `:list/options` | ref, many | |
| `:list/tag-set` | ref, one | |
| `:list-option/uuid` | string, unique identity | |
| `:list-option/name` | string | |
| `:list-option/value` | string | value passed to C++ |
| `:list-option/order` | long | |
| `:list-option/translation-key` | string | |
| `:list-option/result-translation-key` | string, unique | |
| `:list-option/default` | boolean | |
| `:list-option/hide?` | boolean | |
| `:list-option/tag-refs` | ref, many | |

### Tag / TagSet
| Attribute | Type | Notes |
|-----------|------|-------|
| `:tag-set/name` | string, unique | |
| `:tag-set/color?` | boolean | |
| `:tag-set/tags` | ref, many, isComponent | |
| `:tag-set/translation-key` | string, unique | |
| `:tag/name` | string, unique | |
| `:tag/translation-key` | string, unique | |
| `:tag/order` | long | |
| `:tag/color` | string | hex color for color tags |

### Domain / DomainSet
| Attribute | Type | Notes |
|-----------|------|-------|
| `:domain-set/name` | string, unique | |
| `:domain-set/domains` | ref, many | |
| `:domain/name` | string | |
| `:domain/decimals` | long | default decimal places |
| `:domain/dimension-uuid` | string | UUID of Dimension |
| `:domain/native-unit-uuid` | string | UUID of Unit |
| `:domain/english-unit-uuid` | string | UUID of Unit |
| `:domain/metric-unit-uuid` | string | UUID of Unit |
| `:domain/filtered-unit-uuids` | string, many | subset for unit dropdown |

### Conditional
| Attribute | Type | Notes |
|-----------|------|-------|
| `:conditional/type` | keyword | `:module` or `:group-variable` |
| `:conditional/operator` | keyword | `:equal`, `:not-equal`, `:in` |
| `:conditional/values` | string, many | values to match |
| `:conditional/group-variable-uuid` | string | for `:group-variable` type |
| `:conditional/sub-conditionals` | ref, many, isComponent | nested conditionals |
| `:conditional/sub-conditional-operator` | keyword | `:and` or `:or` |

### Action
| Attribute | Type | Notes |
|-----------|------|-------|
| `:action/name` | string | describes when action fires |
| `:action/type` | keyword | `:select` or `:disable` |
| `:action/target-value` | string | |
| `:action/conditionals` | ref, many, isComponent | |
| `:action/conditionals-operator` | keyword | `:and` or `:or` |

### C++ Entities
| Attribute | Type | Notes |
|-----------|------|-------|
| `:cpp.namespace/name` | string | e.g. `"global"` |
| `:cpp.class/name` | string | e.g. `"SIGSurface"` |
| `:cpp.function/name` | string | |
| `:cpp.function/return-type` | string | |
| `:cpp.parameter/name` | string | |
| `:cpp.parameter/type` | string | |
| `:cpp.parameter/order` | long | |
| `:cpp.enum/name` | string | |
| `:cpp.enum-member/name` | string | |
| `:cpp.enum-member/value` | string | |

### Translation
| Attribute | Type | Notes |
|-----------|------|-------|
| `:translation/uuid` | string, unique identity | |
| `:translation/key` | string, unique | matches entity `*-translation-key` |
| `:translation/translation` | string | display text |

---

## `schema_migrate.interface` Function Reference

Require as: `[schema-migrate.interface :as sm]`

### Lookup Functions
```clojure
;; Generic attribute lookup
(sm/name->uuid  conn attr name)    ; → :bp/uuid string
(sm/name->nid   conn attr name)    ; → :bp/nid string
(sm/name->eid   conn attr name)    ; → Datomic entity id (long)

;; Translation-key lookup (most common for VMS entities)
(sm/t-key->uuid    conn t-key)     ; → :bp/uuid string
(sm/t-key->eid     conn t-key)     ; → entity id (long)
(sm/t-key->entity  conn t-key)     ; → Datomic entity map
(sm/eid->t-key     conn eid)       ; → translation-key string
(sm/t-key-action-name->eid conn t-key action-name) ; → action entity id

;; C++ entity lookup
(sm/cpp-ns->uuid    conn ns-name)               ; → uuid of cpp.namespace
(sm/cpp-class->uuid conn class-name)            ; → uuid of cpp.class
(sm/cpp-fn->uuid    conn fn-name)               ; → uuid of cpp.function
(sm/cpp-param->uuid conn fn-name param-name)    ; → uuid of cpp.parameter
(sm/cpp-uuids conn {:cpp-namespace "ns"
                    :cpp-class     "Cls"
                    :cpp-function  "fn"})        ; → map with resolved uuids
```

### Payload Builders
```clojure
;; Wrap any map to add :bp/uuid and :bp/nid
(sm/->entity data)                                ; → map with uuid+nid

;; Higher-level builders (handle uuid/nid + parent refs automatically)
(sm/->group         submodule-eid gname t-key)   ; → group entity map
(sm/->subgroup      group-eid gname t-key)       ; → subgroup entity map
(sm/->group-variable group-eid variable-eid t-key) ; → gv entity map
(sm/->link          source-eid destination-eid)  ; → link payload

;; Conditional builders
(sm/->gv-conditional uuid operator value)        ; uuid of target GV, :equal/:in/:not-equal, value string
(sm/->module-conditional operator values)        ; :equal/:not-equal/:in, set of module name strings

;; Migration record (for rollback tracking)
(sm/->migration migration-name)                  ; → migration entity
```

### Utility Functions
```clojure
;; Recursively inject :bp/uuid and :bp/nid into every nested map
(sm/postwalk-insert data)                        ; use on deeply nested entity trees

;; Build translation entities and attach to English language entity
(sm/build-translations-payload conn eid-start
  {"translation:key:1" "Display Text"
   "translation:key:2" "More Text"})             ; eid-start prevents tempid overlap

;; Roll back a transaction (for testing/undoing migrations)
(sm/rollback-tx! conn tx-result)                 ; pass the deref'd d/transact result

;; Remove translation refs from an entity and its components
(sm/remove-nested-i18ns-tx conn t-key)

;; Schema metadata manipulation
(sm/make-attr-is-component! conn attr)
(sm/make-attr-is-component-payload conn attr)
```

---

## Migration File Template

```clojure
(ns migrations.YYYY-MM-DD-description
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; 1. Initialize and get connection
(cms/init-db!)
(def conn (default-conn))

;; 2. Build entity payloads (use negative :db/id for new entities)
(def new-entities
  (sm/postwalk-insert
    [{:db/id                             -1
      :group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
      :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGSurface")
      :group-variable/cpp-function       (sm/cpp-fn->uuid conn "getFlameLength")
      :group-variable/translation-key    "behaveplus:surface:fire:flame-length"
      :group-variable/result-translation-key "behaveplus:surface:fire:flame-length:result"
      :group-variable/help-key           "behaveplus:surface:fire:flame-length:help"
      :group-variable/order              0}]))

;; 3. Link new entities to existing parents (use t-key->eid to find parent)
(def parent-refs
  [{:db/id               (sm/t-key->eid conn "behaveplus:surface:fire:surface-fire")
    :group/group-variables [-1]}])

;; 4. Build translation payload (eid-start avoids overlap with tempids above)
(def translations
  (sm/build-translations-payload conn 100
    {"behaveplus:surface:fire:flame-length"        "Flame Length"
     "behaveplus:surface:fire:flame-length:result" "Flame Len."}))

;; 5. Combine and transact — always inside a comment block
(comment
  (def tx-result
    (d/transact conn (concat new-entities parent-refs translations)))

  ;; Rollback if needed
  (sm/rollback-tx! conn @tx-result))
```

**Rules:**
- Use `sm/postwalk-insert` when building nested structures to avoid manually adding `:bp/uuid`/`:bp/nid` everywhere.
- Reserve tempids (`-1`, `-2`, …) for new entities; set `:db/id` on existing entities via `sm/t-key->eid`.
- `eid-start` in `build-translations-payload` should be higher than the largest tempid used (e.g., 100 when tempids go to -3).
- Always wrap `d/transact` in a `(comment ...)` block — migrations are REPL-driven, not auto-executed.

---

## Common Recipes

### Add a GroupVariable to an Existing Group

```clojure
;; Find the group and variable
(def group-eid    (sm/t-key->eid conn "behaveplus:surface:fire:surface-fire"))
(def variable-eid (sm/t-key->eid conn "behaveplus:variable:flame-length"))

;; Build payload (->group-variable handles uuid/nid/parent ref)
(def payload [(sm/->group-variable group-eid variable-eid
                "behaveplus:surface:fire:flame-length")])

(comment
  (d/transact conn payload))
```

### Add a New Group to a Submodule

```clojure
(def submodule-eid (sm/t-key->eid conn "behaveplus:surface:fire-behavior-output"))

(def payload [(sm/->group submodule-eid "New Group Name"
                "behaveplus:surface:new-group")])

(comment
  (d/transact conn payload))
```

### Add a Submodule/Group Conditional

```clojure
;; Show submodule only when "spot" module is selected
(def conditional
  (sm/->module-conditional :equal #{"spot"}))

(def payload
  [{:db/id                          (sm/t-key->eid conn "behaveplus:spot:input")
    :submodule/conditionals         [conditional]
    :submodule/conditionals-operator :or}])

(comment
  (d/transact conn payload))
```

### Add an Action to a GroupVariable

```clojure
;; Disable an option when a specific output is selected
(def gv-eid (sm/t-key->eid conn "behaveplus:surface:wind:wind-speed-type"))

(def action
  (sm/->entity
    {:action/name                  "Disable midflame when midflame wind speed output is selected"
     :action/type                  :disable
     :action/target-value          "midflame-eye-level"
     :action/conditionals-operator :and
     :action/conditionals
     [(sm/->gv-conditional
        (sm/t-key->uuid conn "behaveplus:surface:wind:midflame-wind-speed")
        :equal "selected")]}))

(def payload
  [{:db/id                  gv-eid
    :group-variable/actions [action]}])

(comment
  (d/transact conn payload))
```

### Add a New List with Options

```clojure
(def new-list
  (sm/postwalk-insert
    {:list/uuid    "generated-or-hardcoded-uuid"
     :list/name    "MyNewList"
     :list/options [{:list-option/name              "Option A"
                     :list-option/value             "option-a"
                     :list-option/order             0
                     :list-option/translation-key   "behaveplus:list:my-new-list:option-a"
                     :list-option/default           true}
                    {:list-option/name              "Option B"
                     :list-option/value             "option-b"
                     :list-option/order             1
                     :list-option/translation-key   "behaveplus:list:my-new-list:option-b"}]}))

(def translations
  (sm/build-translations-payload conn 100
    {"behaveplus:list:my-new-list:option-a" "Option A Label"
     "behaveplus:list:my-new-list:option-b" "Option B Label"}))

(comment
  (d/transact conn (concat [new-list] translations)))
```

### Update an Existing Translation

```clojure
;; Find the translation entity and update its :translation/translation field
(def tx-key "behaveplus:surface:fire:surface-fire")
(def entity  (sm/t-key->entity conn tx-key))

(comment
  (d/transact conn [{:db/id                    (:db/id entity)
                     :translation/translation  "Updated Display Name"}]))
```

### Import C++ Class Definitions

Use `add-export-file-to-conn` from `development/cms_import.clj`:

```clojure
;; EDN export file format (e.g., cms-exports/SIGSurface.edn):
;; {:global {:SIGSurface {:getFlameLengthFire {:id "getFlameLengthFire"
;;                                             :type "double"
;;                                             :parameters [{:id "p1" :type "double"}]}}}}

(require '[cms-import :refer [add-export-file-to-conn]])
(add-export-file-to-conn "cms-exports/SIGSurface.edn" conn)
```

### Reorder Entities

Reordering is done via `:order` attribute on the entity. Find siblings, update their `:*/order` values:

```clojure
;; Swap order of two groups
(comment
  (d/transact conn
    [{:db/id        (sm/t-key->eid conn "behaveplus:surface:group-a")
      :group/order  1}
     {:db/id        (sm/t-key->eid conn "behaveplus:surface:group-b")
      :group/order  0}]))
```

---

## Key File Paths

| Purpose | Path |
|---------|------|
| `schema_migrate` helper functions | `components/schema_migrate/src/schema_migrate/interface.clj` |
| `schema_migrate` implementation | `components/schema_migrate/src/schema_migrate/core.clj` |
| All schema definitions | `bases/behave_schema/src/behave/schema/` |
| Group schema | `bases/behave_schema/src/behave/schema/group.cljc` |
| GroupVariable schema | `bases/behave_schema/src/behave/schema/group_variable.cljc` |
| Variable schema | `bases/behave_schema/src/behave/schema/variable.cljc` |
| Conditional schema | `bases/behave_schema/src/behave/schema/conditionals.cljc` |
| Action schema | `bases/behave_schema/src/behave/schema/actions.cljc` |
| List/Tag schema | `bases/behave_schema/src/behave/schema/list.cljc` |
| Domain schema | `bases/behave_schema/src/behave/schema/domain.cljc` |
| Example migrations | `development/migrations/` |
| C++ import utilities | `development/cms_import.clj` |
| CMS store (connection) | `projects/behave_cms/src/clj/behave_cms/store.clj` |
| CMS server init | `projects/behave_cms/src/clj/behave_cms/server.clj` |
| CMS CLJS entity events | `projects/behave_cms/src/cljs/behave_cms/events.cljs` |
| CMS routes | `projects/behave_cms/src/cljc/behave_cms/routes.cljc` |

---

## Translation Key Conventions

Translation keys follow the pattern: `behaveplus:<module>:<submodule>:<group>:<variable>`

Examples:
- `"behaveplus:surface:fire-behavior-output:surface-fire"` — a Group
- `"behaveplus:surface:fire-behavior-output:surface-fire:flame-length"` — a GroupVariable
- `"behaveplus:surface:fire-behavior-output:surface-fire:flame-length:result"` — result label
- `"behaveplus:surface:fire-behavior-output:surface-fire:flame-length:help"` — help key

Help keys append `:help` to the entity's translation key.
Result translation keys append `:result` (shorter form shown in results matrices).
