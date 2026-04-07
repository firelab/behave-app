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

## Auto-Migrate Runner

Migrations are auto-discovered and executed by `schema-migrate.runner/run-pending-migrations!`.

### How It Works

1. **Discovery**: scans `development/migrations/` for `.clj` files, converts filenames to namespace symbols (`2026_01_01_foo.clj` → `migrations.2026-01-01-foo`), sorted lexicographically
2. **Skips**: the `template` namespace and any ns with `^{:migrate/ignore? true}` metadata
3. **Requires** each namespace and looks for one of three exports (see below)
4. **Idempotency**: each applied migration is recorded via `:bp/migration-id`; re-running skips already-applied migrations
5. **Halt-on-failure**: first failing migration stops execution

### Export Options

Each migration must export **exactly one** of:

| Export | Type | When to use |
|--------|------|-------------|
| `payload-fn` | `(fn [conn] [...tx-data...])` | Default — use when you need `conn` for lookups |
| `payload` | `(def payload [...tx-data...])` | Static tx data with no lookups needed |
| `payload-steps` | `(def payload-steps [(fn [conn] ...) ...])` | Multi-step: executed in order, auto-rollback on failure |

### Multi-Step Rollback

For `payload-steps`, if step N fails:
- Steps 0..(N-1) are rolled back in reverse order automatically
- The migration marker is NOT recorded
- The exception is re-thrown

### Key File

`components/schema_migrate/src/schema_migrate/runner.clj`

---

## Migration File Template

```clojure
(ns migrations.YYYY-MM-DD-description
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]))

;; =======================================================================
;; Overview
;; =======================================================================

;; Describe what this migration does and why.

;; =======================================================================
;; Payload
;; =======================================================================

;; Option A: Single-step migration
;; The runner calls (payload-fn conn) at startup.
;; Return a vector of transaction data.

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [conn]
  (let [parent-eid (sm/t-key->eid conn "behaveplus:surface:fire:surface-fire")]
    (concat
      (sm/postwalk-insert
        [{:db/id                                -1
          :group-variable/cpp-namespace          (sm/cpp-ns->uuid conn "global")
          :group-variable/cpp-class              (sm/cpp-class->uuid conn "SIGSurface")
          :group-variable/cpp-function           (sm/cpp-fn->uuid conn "getFlameLength")
          :group-variable/translation-key        "behaveplus:surface:fire:flame-length"
          :group-variable/result-translation-key "behaveplus:surface:fire:flame-length:result"
          :group-variable/help-key              "behaveplus:surface:fire:flame-length:help"
          :group-variable/order                  0}])
      [{:db/id               parent-eid
        :group/group-variables [-1]}]
      (sm/build-translations-payload conn 100
        {"behaveplus:surface:fire:flame-length"        "Flame Length"
         "behaveplus:surface:fire:flame-length:result" "Flame Len."}))))

;; Option B: Multi-step migration (uncomment and remove payload-fn above)
;; Each step is a function of conn (or a raw payload vector).
;; If any step fails, all previously completed steps are rolled back.
;;
;; #_{:clj-kondo/ignore [:missing-docstring]}
;; (def payload-steps
;;   [(fn [conn] [{:db/id (sm/t-key->eid conn "behaveplus:...") :group/order 0}])
;;    (fn [conn] [[:db/retractEntity (sm/t-key->eid conn "behaveplus:...")]])])

;; =======================================================================
;; Manual REPL usage
;; =======================================================================

(comment
  (require '[behave-cms.server :as cms])
  (cms/init-db!)

  #_{:clj-kondo/ignore [:missing-docstring]}
  (def conn (behave-cms.store/default-conn))

  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn (payload-fn conn)))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; =======================================================================
;; In case we need to rollback.
;; =======================================================================

(comment
  (sm/rollback-tx! conn tx-data))
```

**Rules:**
- **Runner interface**: migrations must export `payload-fn`, `payload`, or `payload-steps` — no other var name is discovered by the runner.
- **No top-level side effects**: `(cms/init-db!)` and `(def conn ...)` must NOT appear at the top level — the runner `require`s namespaces, so top-level side effects execute on discovery. Place these only in `(comment ...)` blocks for manual REPL use.
- **REPL-only migrations**: add `^{:migrate/ignore? true}` to ns metadata to prevent auto-execution (for one-off fixes, data explorations, etc.).
- **Multi-step**: use `payload-steps` when you need ordered steps with automatic rollback on failure.
- Use `sm/postwalk-insert` when building nested structures to avoid manually adding `:bp/uuid`/`:bp/nid` everywhere.
- Reserve tempids (`-1`, `-2`, …) for new entities; set `:db/id` on existing entities via `sm/t-key->eid`.
- `eid-start` in `build-translations-payload` should be higher than the largest tempid used (e.g., 100 when tempids go to -3).
- Always wrap `d/transact` in a `(comment ...)` block for manual REPL usage — the runner handles transacting automatically.

---

## Common Recipes

### Add a GroupVariable to an Existing Group

```clojure
(defn payload-fn [conn]
  (let [group-eid    (sm/t-key->eid conn "behaveplus:surface:fire:surface-fire")
        variable-eid (sm/t-key->eid conn "behaveplus:variable:flame-length")]
    [(sm/->group-variable group-eid variable-eid
       "behaveplus:surface:fire:flame-length")]))
```

### Add a New Group to a Submodule

```clojure
(defn payload-fn [conn]
  (let [submodule-eid (sm/t-key->eid conn "behaveplus:surface:fire-behavior-output")]
    [(sm/->group submodule-eid "New Group Name"
       "behaveplus:surface:new-group")]))
```

### Add a Submodule/Group Conditional

```clojure
;; Show submodule only when "spot" module is selected
(defn payload-fn [conn]
  (let [conditional (sm/->module-conditional :equal #{"spot"})]
    [{:db/id                          (sm/t-key->eid conn "behaveplus:spot:input")
      :submodule/conditionals         [conditional]
      :submodule/conditionals-operator :or}]))
```

### Add an Action to a GroupVariable

```clojure
;; Disable an option when a specific output is selected
(defn payload-fn [conn]
  (let [gv-eid (sm/t-key->eid conn "behaveplus:surface:wind:wind-speed-type")
        action (sm/->entity
                 {:action/name                  "Disable midflame when midflame wind speed output is selected"
                  :action/type                  :disable
                  :action/target-value          "midflame-eye-level"
                  :action/conditionals-operator :and
                  :action/conditionals
                  [(sm/->gv-conditional
                     (sm/t-key->uuid conn "behaveplus:surface:wind:midflame-wind-speed")
                     :equal "selected")]})]
    [{:db/id                  gv-eid
      :group-variable/actions [action]}]))
```

### Add a New List with Options

```clojure
(defn payload-fn [conn]
  (let [new-list
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
                           :list-option/translation-key   "behaveplus:list:my-new-list:option-b"}]})
        translations
        (sm/build-translations-payload conn 100
          {"behaveplus:list:my-new-list:option-a" "Option A Label"
           "behaveplus:list:my-new-list:option-b" "Option B Label"})]
    (concat [new-list] translations)))
```

### Update an Existing Translation

```clojure
(defn payload-fn [conn]
  (let [entity (sm/t-key->entity conn "behaveplus:surface:fire:surface-fire")]
    [{:db/id                    (:db/id entity)
      :translation/translation  "Updated Display Name"}]))
```

### Import C++ Class Definitions

Use `add-export-file-to-conn` from `development/cms_import.clj`.
This is REPL-only — not suitable for auto-migration:

```clojure
;; EDN export file format (e.g., cms-exports/SIGSurface.edn):
;; {:global {:SIGSurface {:getFlameLengthFire {:id "getFlameLengthFire"
;;                                             :type "double"
;;                                             :parameters [{:id "p1" :type "double"}]}}}}

(comment
  (require '[cms-import :refer [add-export-file-to-conn]])
  (add-export-file-to-conn "cms-exports/SIGSurface.edn" conn))
```

### Reorder Entities

```clojure
(defn payload-fn [conn]
  [{:db/id        (sm/t-key->eid conn "behaveplus:surface:group-a")
    :group/order  1}
   {:db/id        (sm/t-key->eid conn "behaveplus:surface:group-b")
    :group/order  0}])
```

### Delete an Entity

```clojure
(defn payload-fn [conn]
  [[:db/retractEntity
    (sm/t-key->eid conn "behaveplus:surface:some:entity-to-remove")]])
```

### Multi-Step Migration Example

Use `payload-steps` when steps must be ordered and you want automatic rollback:

```clojure
#_{:clj-kondo/ignore [:missing-docstring]}
(def payload-steps
  [(fn [conn]
     ;; Step 1: add new group
     (let [sub-eid (sm/t-key->eid conn "behaveplus:surface:fire-behavior-output")]
       [(sm/->group sub-eid "New Group" "behaveplus:surface:new-group")]))
   (fn [conn]
     ;; Step 2: reorder siblings
     [{:db/id       (sm/t-key->eid conn "behaveplus:surface:existing-group")
       :group/order 1}
      {:db/id       (sm/t-key->eid conn "behaveplus:surface:new-group")
       :group/order 0}])])
```

---

## Key File Paths

| Purpose | Path |
|---------|------|
| Auto-migrate runner | `components/schema_migrate/src/schema_migrate/runner.clj` |
| `schema_migrate` helper functions | `components/schema_migrate/src/schema_migrate/interface.clj` |
| `schema_migrate` implementation | `components/schema_migrate/src/schema_migrate/core.clj` |
| Migration template | `development/migrations/template.clj` |
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
