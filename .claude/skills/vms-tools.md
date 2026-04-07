---
name: vms-tools
description: High-level VMS manipulation tools for writing migrations. Wraps schema_migrate builders with automatic translation creation, order management, and entity lookup by translation key.
triggers:
  - writing a VMS migration that adds, removes, or moves entities
  - adding a group, subgroup, or group-variable
  - updating translations or C++ bindings
  - reordering entities
  - adding conditionals or actions
---

# VMS Migration Tools

## Overview

`development/migrations/tools.clj` provides high-level functions that wrap `schema-migrate.interface` builders. Each function:

- Takes `conn` and a simple options map
- Looks up entities by **translation key** (no raw eids needed)
- Creates **translations automatically** alongside entities
- Computes **order automatically** (appends last) unless overridden
- Returns **tx-data vectors** ready for `payload-fn` or direct transact

## Quick Start

```clojure
(ns migrations.YYYY-MM-DD-description
  (:require [migrations.tools :as t]
            [schema-migrate.interface :as sm]))

(defn payload-fn [conn]
  (t/add-group conn
    {:parent-t-key "behaveplus:surface:input:fuel"
     :group-name   "New Group"
     :t-key        "behaveplus:surface:input:fuel:new-group"
     :translation  "New Group"}))
```

## Available Tools

### Entity Creation

#### `add-group` — Add a group to a submodule

```clojure
(t/add-group conn
  {:parent-t-key "behaveplus:surface:input:fuel"     ;; parent submodule t-key
   :group-name   "Fuel Models"                       ;; display name
   :t-key        "behaveplus:surface:input:fuel:fuel-models" ;; new t-key
   :translation  "Fuel Models"                       ;; English text
   ;; optional:
   :order        2                                   ;; default: last
   :hidden?      false
   :research?    false
   :conditionals [...]})
```

#### `add-subgroup` — Add a child group under an existing group

```clojure
(t/add-subgroup conn
  {:parent-t-key "behaveplus:surface:input:fuel:fuel-moisture"
   :group-name   "Dead Fuel"
   :t-key        "behaveplus:surface:input:fuel:fuel-moisture:dead"
   :translation  "Dead Fuel"})
```

#### `add-group-variable` — Add a GV to a group

```clojure
(t/add-group-variable conn
  {:parent-t-key  "behaveplus:surface:output:fire_behavior:surface_fire"
   :variable-name "FlameLength"                      ;; existing Variable name
   :t-key         "behaveplus:surface:output:fire_behavior:surface_fire:flame-length"
   :translation   "Flame Length"
   :result-text   "Flame Len."                       ;; short results label
   ;; optional C++ binding:
   :cpp-namespace "global"
   :cpp-class     "SIGSurface"
   :cpp-function  "getFlameLength"
   :cpp-parameter nil                                ;; for outputs, usually nil
   ;; optional flags:
   :conditionally-set? false
   :hide-result?       false
   :actions            [...]})
```

### Entity Removal

#### `remove-entity` — Delete entity + nested translations

```clojure
(t/remove-entity conn "behaveplus:surface:some:old:entity")
```

### Moving Entities

#### `move-entity` — Move to a new parent

Moves a group, subgroup, or group-variable to a different parent without
changing any of its existing attributes (translation keys, help keys, C++ bindings,
conditionals, actions, etc.).

```clojure
;; Move a group to a different submodule
(t/move-entity conn :group
  "behaveplus:surface:input:fuel:some-group"     ;; entity to move
  "behaveplus:surface:input:wind")               ;; new parent submodule

;; Move a group to become a subgroup of another group
(t/move-entity conn :group
  "behaveplus:surface:input:fuel:some-group"
  "behaveplus:surface:input:fuel:fuel-moisture")

;; Move a group-variable to a different group
(t/move-entity conn :group-variable
  "behaveplus:surface:input:fuel:fuel-moisture:dead:1-hour"
  "behaveplus:surface:input:fuel:fuel-moisture:live"
  :order 0)                                      ;; optional: position in new parent

;; Move a subgroup to a different parent group
(t/move-entity conn :subgroup
  "behaveplus:crown:input:canopy:some-subgroup"
  "behaveplus:crown:input:canopy:other-group")
```

Parameters:
- `entity-type` — `:group`, `:subgroup`, or `:group-variable`
- `t-key` — translation key of the entity to move
- `new-parent-t-key` — translation key of the destination parent
- `:order` — optional keyword arg; position in new parent (default: appended last)

For `:group`, the new parent is auto-detected as submodule or group.

### Ordering

#### `reorder-children` — Set explicit order by t-key list

```clojure
(t/reorder-children conn :group/order
  ["behaveplus:surface:input:fuel:fuel-model"     ;; order 0
   "behaveplus:surface:input:fuel:fuel-moisture"  ;; order 1
   "behaveplus:surface:input:fuel:new-group"])     ;; order 2
```

Works with any order attribute: `:group/order`, `:group-variable/order`, `:submodule/order`.

#### `insert-at-order` — Insert at position, shift siblings

```clojure
(t/insert-at-order conn
  {:parent-t-key "behaveplus:surface:input:fuel"
   :child-attr   :submodule/groups
   :order-attr   :group/order
   :t-key        "behaveplus:surface:input:fuel:new-group"
   :new-order    1})
```

### Translations

#### `update-translation` — Change display text

```clojure
(t/update-translation conn
  "behaveplus:surface:input:fuel:fuel-model"
  "Surface Fuel Model")
```

#### `add-translations` — Add new translation entries

```clojure
(t/add-translations conn
  {"behaveplus:ui:new-button"  "New Button"
   "behaveplus:ui:new-label"   "New Label"})
```

### C++ Bindings

#### `update-cpp-binding` — Change C++ mapping on a GV

```clojure
(t/update-cpp-binding conn
  {:t-key         "behaveplus:surface:output:fire_behavior:surface_fire:flame-length"
   :cpp-namespace "global"
   :cpp-class     "SIGSurface"
   :cpp-function  "getFlameLength"})
```

Include only the keys that are changing. All C++ name → UUID resolution is automatic.

### Conditionals

#### `add-group-conditional` — Add conditional to a group

```clojure
;; Show group only when "crown" module is selected
(t/add-group-conditional conn
  "behaveplus:crown:input:canopy:canopy-characteristics"
  (sm/->module-conditional :equal #{"crown"})
  :operator :or)

;; Show group based on a GV value
(t/add-group-conditional conn
  "behaveplus:surface:input:wind:some-group"
  (sm/->gv-conditional
    (sm/t-key->uuid conn "behaveplus:surface:input:wind:wind-speed-type")
    :equal "20ft")
  :operator :and)
```

#### `add-submodule-conditional` — Same, for submodules

```clojure
(t/add-submodule-conditional conn
  "behaveplus:spot:input"
  (sm/->module-conditional :equal #{"spot"}))
```

### Actions

#### `add-action` — Add action to a GV

```clojure
(t/add-action conn
  {:gv-t-key     "behaveplus:surface:input:wind:wind-speed-type"
   :action-name  "Disable midflame for output"
   :action-type  :disable
   :target-value "midflame"
   :conditionals [(sm/->gv-conditional
                    (sm/t-key->uuid conn "behaveplus:surface:output:wind:midflame")
                    :equal "selected")]})
```

### Generic Update

#### `update-entity` — Set arbitrary attributes

```clojure
(t/update-entity conn "behaveplus:surface:some:group"
  {:group/hidden?   true
   :group/research? false})
```

## Composing Tools in Migrations

Tools return tx-data vectors, so compose them with `concat`:

```clojure
(defn payload-fn [conn]
  (concat
    (t/add-group conn
      {:parent-t-key "behaveplus:surface:input:fuel"
       :group-name   "New Group"
       :t-key        "behaveplus:surface:input:fuel:new-group"
       :translation  "New Group"})
    (t/add-group-variable conn
      {:parent-t-key  "behaveplus:surface:input:fuel:new-group"
       :variable-name "FlameLength"
       :t-key         "behaveplus:surface:input:fuel:new-group:flame-length"
       :translation   "Flame Length"
       :result-text   "Flame Len."})))
```

For operations that depend on prior transactions (e.g., adding a GV to a group
you just created), use `payload-steps`:

```clojure
(def payload-steps
  [(fn [conn]
     (t/add-group conn
       {:parent-t-key "behaveplus:surface:input:fuel"
        :group-name   "New Group"
        :t-key        "behaveplus:surface:input:fuel:new-group"
        :translation  "New Group"}))
   (fn [conn]
     (t/add-group-variable conn
       {:parent-t-key  "behaveplus:surface:input:fuel:new-group"
        :variable-name "FlameLength"
        :t-key         "behaveplus:surface:input:fuel:new-group:flame-length"
        :translation   "Flame Length"
        :result-text   "Flame Len."}))])
```

## Key Files

| Purpose | Path |
|---------|------|
| Tool functions | `development/migrations/tools.clj` |
| Query functions | `development/migrations/queries.clj` |
| Migration template | `development/migrations/template.clj` |
| schema_migrate builders | `components/schema_migrate/src/schema_migrate/interface.clj` |
| Auto-migrate runner | `components/schema_migrate/src/schema_migrate/runner.clj` |
