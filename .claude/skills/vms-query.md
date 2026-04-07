---
name: vms-query
description: VMS discovery queries for exploring BehavePlus 7 data structure via Datomic REPL. Covers module/group/variable inspection, translation lookups, C++ binding exploration, and conditional/action queries.
triggers:
  - exploring VMS data structure
  - querying modules, submodules, groups, or variables
  - looking up translations or C++ bindings
  - inspecting conditionals or actions
  - understanding the VMS hierarchy before writing a migration
---

# VMS Discovery Queries

## Quick Start

The file `development/migrations/queries.clj` provides ready-made query functions for exploring VMS data in the REPL. It is marked `^{:migrate/ignore? true}` so the auto-migrate runner skips it.

### Initialize

```clojure
(require '[migrations.queries :as q])

;; Then in a REPL comment block:
(require '[behave-cms.server :as cms])
(cms/init-db!)
(def conn (behave-cms.store/default-conn))
```

### Or via nREPL MCP

If connected via the clojure-mcp nREPL, evaluate the init block then pass `conn` to any query function.

---

## Available Query Functions

All functions take `conn` as their first argument.

### Structure Navigation

| Function | Args | Returns |
|----------|------|---------|
| `(q/list-modules conn)` | — | All modules: name, order, t-key |
| `(q/list-submodules conn module-name)` | `"Surface"` | Submodules: name, io, order, t-key |
| `(q/list-groups conn submodule-t-key)` | `"behaveplus:surface:input:fuel"` | Groups: name, order, t-key |
| `(q/list-subgroups conn group-t-key)` | `"behaveplus:surface:input:fuel:fuel-moisture"` | Child groups |
| `(q/pull-hierarchy conn module-name)` | `"Surface"` | Full module → submodule → group → gv tree |

### Group Variables & Variables

| Function | Args | Returns |
|----------|------|---------|
| `(q/list-group-variables conn group-t-key)` | group translation key | GVs: order, t-key, variable name |
| `(q/find-variable conn var-name)` | `"FlameLength"` | UUID, kind, t-key |
| `(q/variable-details conn var-name)` | `"FlameLength"` | Full pull of variable entity |
| `(q/gv-cpp-bindings conn gv-t-key)` | GV translation key | C++ namespace/class/function/parameter |
| `(q/find-gvs-for-variable conn var-name)` | `"FlameLength"` | All GVs referencing this variable, with IO |

### Conditionals & Actions

| Function | Args | Returns |
|----------|------|---------|
| `(q/group-conditionals conn group-t-key)` | group translation key | Conditionals with sub-conditionals |
| `(q/submodule-conditionals conn sm-t-key)` | submodule translation key | Conditionals with sub-conditionals |
| `(q/gv-actions conn gv-t-key)` | GV translation key | Actions with their conditionals |

### Lists & Tags

| Function | Args | Returns |
|----------|------|---------|
| `(q/list-options conn list-name)` | `"SurfaceFuelModels"` | All options sorted by order |
| `(q/list-tag-sets conn)` | — | All tag sets with color? flag |
| `(q/tag-set-tags conn tag-set-name)` | `"FuelModelCategory"` | Tags sorted by order |

### Translations

| Function | Args | Returns |
|----------|------|---------|
| `(q/translation conn t-key)` | exact translation key | Display text string |
| `(q/translations-like conn pattern)` | `"flame-length"` | All matching keys and translations |

### Entity Inspection

| Function | Args | Returns |
|----------|------|---------|
| `(q/entity-by-t-key conn t-key)` | any translation key | Full pull of entity |
| `(q/touch-entity conn eid)` | entity ID | All attributes (d/touch) |

### C++ Bindings

| Function | Args | Returns |
|----------|------|---------|
| `(q/list-cpp-namespaces conn)` | — | All C++ namespaces with UUIDs |
| `(q/list-cpp-classes conn cpp-ns)` | `"global"` | Classes in namespace |
| `(q/list-cpp-functions conn cpp-ns class-name)` | `"global" "SIGSurface"` | Functions with return types |

### Migration Status

| Function | Args | Returns |
|----------|------|---------|
| `(q/applied-migrations conn)` | — | Sorted list of applied migration IDs |

---

## Common Workflows

### Before writing a migration: understand the target area

```clojure
;; 1. Find the module
(q/list-modules conn)

;; 2. Drill into submodules
(q/list-submodules conn "Crown")

;; 3. Find the group you want to modify
(q/list-groups conn "behaveplus:crown:input:canopy")

;; 4. See what's already in the group
(q/list-group-variables conn "behaveplus:crown:input:canopy:canopy-characteristics")

;; 5. Check existing conditionals
(q/group-conditionals conn "behaveplus:crown:input:canopy:canopy-characteristics")
```

### Find where a variable appears across the entire VMS

```clojure
(q/find-gvs-for-variable conn "WindSpeed")
;; => [{:gv-t-key "behaveplus:surface:..." :group-t-key "..." :io :input}
;;     {:gv-t-key "behaveplus:crown:..."  :group-t-key "..." :io :input}]
```

### Check a C++ function's parameters before binding a new GV

```clojure
(q/list-cpp-functions conn "global" "SIGSurface")
;; Then use sm/cpp-fn->uuid in your migration
```

### Verify a translation exists

```clojure
(q/translation conn "behaveplus:surface:input:fuel:fuel-model")
;; => "Fuel Model"

;; Or search by substring
(q/translations-like conn "torching")
```

---

## Key Files

| Purpose | Path |
|---------|------|
| Query functions | `development/migrations/queries.clj` |
| VMS rules (recursive hierarchy) | `bases/behave_schema/src/behave/schema/rules.cljc` |
| Schema helper queries | `bases/behave_schema/src/behave/schema/queries.cljc` |
| schema_migrate lookups | `components/schema_migrate/src/schema_migrate/core.clj` |

## VMS Rules Reference

The queries use `behave.schema.rules/all-rules` which provide recursive hierarchy navigation:

- `(group ?submodule ?group)` — find all groups under a submodule (recursive through children)
- `(subgroup ?group ?subgroup)` — find nested child groups (recursive)
- `(group-variable ?group ?gv ?variable)` — find GVs with their associated Variable
- `(io ?entity ?io)` — determine if entity is `:input` or `:output`
- `(app-root ?app ?entity)` — find the root Application for any entity
- `(translation-key ?entity ?key)` — get translation key for any entity type
