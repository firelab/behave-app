(ns migrations.2026-04-28-convert-direction-to-list-option
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [datomic.api              :as d]
            [nano-id.core             :refer [nano-id]]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; 1. Create "Directions" color tag-set with heading/flanking/backing tags and hex colors.
;; 2. Create "Directions" list with three list-options, each linked to its color tag.
;; 3. Seed English translations for the tag-set, list, and list-options.
;; 4. Backfill existing group-variables: link :group-variable/direction-ref to the matching list-option.
;;
;; The deprecated :group-variable/direction keyword is NOT retracted.
;;
;; Prerequisite: if the old 2026_04_27_convert_direction_to_entity migration was applied on this DB,
;; roll it back first (sm/rollback-tx! its transactions in reverse order) before running this one.

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(sm/make-attr-is-component! conn :list/options)

;; Step 1 — tag-set + tags (no db lookups needed at top level)
#_{:clj-kondo/ignore [:missing-docstring]}
(def directions-color-tag-payload
  (sm/postwalk-insert
   [{:db/id                                                       -100
     :tag-set/name                                                "Directions"
     :tag-set/translation-key                                     "behaveplus:tags:directions"
     :tag-set/color?                                              true
     :tag-set/tags
     [{:db/id               -101
       :tag/name            "Heading"
       :tag/color           "#13486a"
       :tag/translation-key "behaveplus:tags:directions:heading"
       :tag/order           0}
      {:db/id               -102
       :tag/name            "Flanking"
       :tag/color           "#4a7086"
       :tag/translation-key "behaveplus:tags:directions:flanking"
       :tag/order           1}
      {:db/id               -103
       :tag/name            "Backing"
       :tag/color           "#8297a3"
       :tag/translation-key "behaveplus:tags:directions:backing"
       :tag/order           2}]}]))

;; Step 2 — translations (defined at top level; requires conn to be initialized)
#_{:clj-kondo/ignore [:missing-docstring]}
(def translations-payload
  (sm/build-translations-payload
   conn
   {"behaveplus:tags:directions"                 "Directions"
    "behaveplus:tags:directions:heading"         "Heading"
    "behaveplus:tags:directions:flanking"        "Flanking"
    "behaveplus:tags:directions:backing"         "Backing"
    "behaveplus:lists:directions"                "Directions"
    "behaveplus:list-option:directions:heading"  "Heading"
    "behaveplus:list-option:directions:flanking" "Flanking"
    "behaveplus:list-option:directions:backing"  "Backing"}))

;; Step 3: create the Directions list (must run after Step 1 so t-key->eid resolves)
(def directions-list-payload
  (sm/postwalk-insert
   [{:list/name                                                                  "Directions"
     :list/translation-key                                                       "behaveplus:lists:directions"
     :list/color-tag-set                                                         -100
     :list/options
     [{:db/id                       -201
       :list-option/name            "Heading"
       :list-option/value           "heading"
       :list-option/order           0
       :list-option/translation-key "behaveplus:list-option:directions:heading"
       :list-option/color-tag-ref   -101}
      {:db/id                       -202
       :list-option/name            "Flanking"
       :list-option/value           "flanking"
       :list-option/order           1
       :list-option/translation-key "behaveplus:list-option:directions:flanking"
       :list-option/color-tag-ref   -102}
      {:db/id                       -203
       :list-option/name            "Backing"
       :list-option/value           "backing"
       :list-option/order           2
       :list-option/translation-key "behaveplus:list-option:directions:backing"
       :list-option/color-tag-ref   -103}]}]))

;; Step 4: backfill existing group-variables with :group-variable/direction → direction-ref list-option

(def direciton-kw->tempid
  {:heading  -201
   :flanking -202
   :backing  -202})

(def gv-pairs
  (d/q '[:find ?gv ?dir
         :where [?gv :group-variable/direction ?dir]]
       (d/db conn)))

(def link-payload
  (vec (for [[gv dir] gv-pairs]
         [:db/add gv :group-variable/direction-ref (direciton-kw->tempid dir)])))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  ;; Step 1: create the Directions color tag-set and tags
  (def tx1 @(d/transact conn (concat directions-color-tag-payload
                                     translations-payload
                                     directions-list-payload
                                     link-payload))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx1))
