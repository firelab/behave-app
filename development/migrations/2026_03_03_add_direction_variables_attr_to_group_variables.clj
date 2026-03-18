(ns migrations.2026-03-03-add-direction-variables-attr-to-group-variables
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [datascript.core          :refer [squuid]]
            [datomic.api              :as d]
            [nano-id.core             :refer [nano-id]]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Introducing a new attribute to group variables: `:group-variable/direction-variables`. These are
;; references to other group variables that are meant to be the directional version of the parent.
;; These "directional children" are group variables that have it's `:group-variable/direction` set and
;; also has the associated cpp functions. We have a few existing group variables in the surface module
;; (i.e. rate of spread, flame length, fireline intensity) that served as a directional parent
;; (i.e. they trigger the enabling of its directional variants via conditionals).
;; Instead of relying on the conditionals on the children, we will now explicity link the parent to the
;; child via this new attribute. This way, the UI has a way to easily group these children.

;; 1. Link Directional Versions of Group Variable to it's Parent

;; There are a few outputs in Mortality (i.e. probability of mortality, scorch height, tree crown
;; length scorched, tree crown volume scorched) which do not follow the pattern of having a
;; direcitonal parent setting all the children since there are no output checkboxes for these, and
;; instead rely soley on being conditionally set. We need to create directional parent group
;; variables for each of these. This requires some renaming of translation key for the existing
;; heading direciton of the outputs, create the directional group variables, and finally link them
;; as we did in step 1.

;; 2. Rename Translation keys
;; 3. Add new Group Variables

;; We can now delete all variable entities that was created for each directional variant (i.e.
;; Heading Rate of Spread, Flanking Rate of Spread, etc). Now all directional childrens of the same
;; output should refer to the same variable (i.e. Rate of Spread)

;; 4. Clean up Variables

;; 5. Add translations for the group variables with translation keys renamed from step 1.
;; 6. Fix Variables, missing bp/uuid and bp/nid

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ;; ===========================================================================================================
;; ;; 3. Exploare and find out what directional group variables we need to process
;; ;; ===========================================================================================================

;; ;; Existing directional group variables should all point to the same variable instead a specific variable for it's direction
;; ;; Remove essentially duplicated variables for each direction (i.e. Scorch Height Heading, Scorch Height Backing, Scorch Height Flanking). Should only be one variable called Scorch Height that all group variables, regardless of directionally should point to.

#_{:clj-kondo/ignore [(:missing-docstring)]}
(def direction-variable-pairs
  (d/q '[:find ?parent-eid ?dir-gv-eid
         :where
         [?dir-gv-eid :group-variable/direction _]
         [?dir-gv-eid :group-variable/actions ?action]
         [?action :action/conditionals ?conditional]
         [?conditional :conditional/type :group-variable]
         [?conditional :conditional/group-variable-uuid ?parent-uuid]
         [?parent-eid :bp/uuid ?parent-uuid]]
       (d/db conn)))

#_{:clj-kondo/ignore [(:missing-docstring)]}
(defn gv-eid->translation-key [eid]
  (:group-variable/translation-key (d/entity (d/db conn) eid)))

(->> direction-variable-pairs
     (group-by first)
     (map (fn [[parent-eid pairs]]
            {:db/id                              (gv-eid->translation-key parent-eid)
             :group-variable/direction-variables (mapv #(gv-eid->translation-key (second %)) pairs)})))

;; ===========================================================================================================
;; 1. Link Directional Versions of Group Variable to it's Parent
;; ===========================================================================================================

#_{:clj-kondo/ignore [(:missing-docstring)]}
(def links-to-add-payload
  [{:db/id                              (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:rate_of_spread"),
    :group-variable/direction-variables [(sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:heading_rate_of_spread")
                                         (sm/t-key->eid  conn "behaveplus:surface:output:fire_behavior:surface_fire:flanking_rate_of_spread")
                                         (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:backing_rate_of_spread")]}

   {:db/id                              (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:fireline_intensity"),
    :group-variable/direction-variables [(sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:heading_fireline_intensity")
                                         (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:flanking_fireline_intensity")
                                         (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:backing_fireline_intensity")]}

   {:db/id                              (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:flame_length"),
    :group-variable/direction-variables [(sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:heading_flame_length")
                                         (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:flanking_flame_length")
                                         (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:backing_flame_length")]}

   {:db/id                              (sm/t-key->eid conn "behaveplus:surface:output:size:surface___fire_size:spread-distance"),
    :group-variable/direction-variables [(sm/t-key->eid conn "behaveplus:surface:output:size:surface___fire_size:heading-spread-distance")
                                         (sm/t-key->eid  conn "behaveplus:surface:output:size:surface___fire_size:backing-spread-distance")
                                         (sm/t-key->eid conn "behaveplus:surface:output:size:surface___fire_size:flanking-spread-distance")]}])

;; ===========================================================================================================
;; 2. Rename Translation keys
;; ===========================================================================================================

;; These group variables should have their keys updated for the heading direction.

#_{:clj-kondo/ignore [(:missing-docstring)]}
(def t-keys-to-process ["behaveplus:mortality:output:tree_mortality:tree_mortality:probability_of_mortality"
                        "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height"
                        "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height"
                        "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched"
                        "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched"])

#_{:clj-kondo/ignore [(:missing-docstring)]}
(defn generate-new-t-keys
  [s]
  (let [last-colon     (clojure.string/last-index-of s ":")
        prefix         (subs s 0 (inc last-colon))
        last-seg       (clojure.string/replace (subs s (inc last-colon)) "-" "_")
        normalized     (str prefix last-seg)
        heading        (str normalized "_heading")
        result-heading (clojure.string/replace heading ":output:" ":result:")]
    [heading result-heading]))

#_{:clj-kondo/ignore [(:missing-docstring)]}
(defn t-key->new-t-key-payload [t-key]
  (let [[new-t-key new-result-key] (generate-new-t-keys t-key)]
    {:db/id                                 (sm/t-key->eid conn t-key)
     :group-variable/translation-key        new-t-key
     :group-variable/result-translation-key new-result-key
     :group-variable/help-key               (str new-t-key ":help")}))

#_{:clj-kondo/ignore [(:missing-docstring)]}
(def update-translation-key-payload
  (map t-key->new-t-key-payload t-keys-to-process))

;; ===========================================================================================================
;; 3. Add new Group Variables
;; ===========================================================================================================

;; Add new group variables that mimic the Surface > Fire Behavior (output) > Surface Fire > Rate of Spread
;; - no cpp functions
;; - hide from results
;; - has references to directional variables
;; - copy actions from the existing heading group variable

#_{:clj-kondo/ignore [(:missing-docstring)]}
(def tree-mortality-eid (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality"))

#_{:clj-kondo/ignore [(:missing-docstring)]}
(defn actions-copy-payload [t-key]
  (map (fn [action]
         (let [conditionals (:action/conditionals action)]
           (cond->
            {:nname        (:action/name action)
             :ttype        (:action/type action)
             :conditionals (map (fn [conditional]
                                  (cond-> {:ttype    (:conditional/type conditional)
                                           :operator (:conditional/operator conditional)
                                           :values   (:conditional/values conditional)}
                                    (:conditional/group-variable-uuid conditional) (assoc :group-variable-uuid (:conditional/group-variable-uuid conditional))))
                                conditionals)}
             (:action/value action) (assoc :target-value (:action/value action)))))
       (:group-variable/actions (sm/t-key->entity conn t-key))))

#_{:clj-kondo/ignore [(:missing-docstring)]}
(def new-group-variables-payload
  [(sm/->group-variable conn
                        {:parent-group-eid    tree-mortality-eid
                         :order               0
                         :variable-eid        (sm/name->eid conn :variable/name "Probability of Mortality")
                         :direction-variables [(sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:probability_of_mortality")
                                               (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-backing")
                                               (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-flanking")]
                         :translation-key     "behaveplus:mortality:output:tree_mortality:tree_mortality:probability_of_mortality"
                         :conditionally-set?  true
                         :actions             (actions-copy-payload "behaveplus:mortality:output:tree_mortality:tree_mortality:probability_of_mortality")
                         :hide-result?        true})

   (sm/->group-variable conn
                        {:parent-group-eid    tree-mortality-eid
                         :order               0
                         :variable-eid        (sm/name->eid conn :variable/name "Bole Char Height")
                         :direction-variables [(sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height")
                                               (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_backing")
                                               (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_flanking")]
                         :translation-key     "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height"
                         :conditionally-set?  true
                         :actions             (actions-copy-payload "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height")
                         :hide-result?        true})

   (sm/->group-variable conn
                        {:parent-group-eid    tree-mortality-eid
                         :order               0
                         :variable-eid        (sm/name->eid conn :variable/name "Scorch Height")
                         :direction-variables [(sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height")
                                               (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-backing")
                                               (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-flanking")]
                         :translation-key     "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height"
                         :conditionally-set?  true
                         :actions             (actions-copy-payload "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height")
                         :hide-result?        true})

   (sm/->group-variable conn
                        {:parent-group-eid    tree-mortality-eid
                         :order               0
                         :variable-eid        (sm/name->eid conn :variable/name "Tree Crown Length Scorched")
                         :direction-variables [(sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched")
                                               (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_backing")
                                               (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_flanking")]
                         :translation-key     "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched"
                         :conditionally-set?  true
                         :actions             (actions-copy-payload "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched")
                         :hide-result?        true})

   (sm/->group-variable conn
                        {:parent-group-eid    tree-mortality-eid
                         :order               0
                         :variable-eid        (sm/name->eid conn :variable/name "Tree Crown Volume Scorched")
                         :direction-variables [(sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched")
                                               (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_backing")
                                               (sm/t-key->eid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_flanking")]
                         :translation-key     "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched"
                         :conditionally-set?  true
                         :actions             (actions-copy-payload "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched")
                         :hide-result?        true})])

;; ===========================================================================================================
;; 4. Clean up Variables
;; ===========================================================================================================

;; These variables should no longer be in the system.

(def variables-to-clean
  [["Heading Rate of Spread" "Rate of Spread" "behaveplus:surface:output:fire_behavior:surface_fire:heading_rate_of_spread"]
   ["Backing Rate of Spread" "Rate of Spread" "behaveplus:surface:output:fire_behavior:surface_fire:backing_rate_of_spread"]
   ["Flanking Rate of Spread" "Rate of Spread" "behaveplus:surface:output:fire_behavior:surface_fire:flanking_rate_of_spread"]
   ["Heading Fireline Intensity" "Fireline Intensity" "behaveplus:surface:output:fire_behavior:surface_fire:heading_fireline_intensity"]
   ["Backing Fireline Intensity" "Fireline Intensity" "behaveplus:surface:output:fire_behavior:surface_fire:backing_fireline_intensity"]
   ["Flanking Fireline Intensity" "Fireline Intensity" "behaveplus:surface:output:fire_behavior:surface_fire:flanking_fireline_intensity"]
   ["Heading Flame Length" "Flame Length" "behaveplus:surface:output:fire_behavior:surface_fire:heading_flame_length"]
   ["Backing Flame Length" "Flame Length" "behaveplus:surface:output:fire_behavior:surface_fire:backing_flame_length"]
   ["Flanking Flame Length" "Flame Length" "behaveplus:surface:output:fire_behavior:surface_fire:flanking_flame_length"]
   ["Heading Spread Distance" "Spread Distance" "behaveplus:surface:output:size:surface___fire_size:heading-spread-distance"]
   ["Flanking Spread Distance" "Spread Distance" "behaveplus:surface:output:size:surface___fire_size:flanking-spread-distance"]
   ["Backing Spread Distance" "Spread Distance" "behaveplus:surface:output:size:surface___fire_size:backing-spread-distance"]
   ["Probability of Mortality Backing" "Probability of Mortality" "behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-backing"]
   ["Probability of Mortality Flanking" "Probability of Mortality" "behaveplus:mortality:output:tree_mortality:tree_mortality:probability-of-mortality-flanking"]
   ["Bole Char Height Backing" "Bole Char Height" "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_backing"]
   ["Bole Char Height Flanking" "Bole Char Height" "behaveplus:mortality:output:tree_mortality:tree_mortality:bole_char_height_flanking"]
   ["Scorch Height Backing" "Scorch Height" "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-backing"]
   ["Scorch Height Flanking" "Scorch Height" "behaveplus:mortality:output:tree_mortality:tree_mortality:scorch-height-flanking"]
   ["Tree Crown Length Scorched Backing" "Tree Crown Length Scorched" "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_backing"]
   ["Tree Crown Length Scorched Flanking" "Tree Crown Length Scorched" "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_length_scorched_flanking"]
   ["Tree Crown Volume Scorched Backing" "Tree Crown Volume Scorched" "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_backing"]
   ["Tree Crown Volume Scorched Flanking" "Tree Crown Volume Scorched" "behaveplus:mortality:output:tree_mortality:tree_mortality:tree_crown_volume_scorched_flanking"]])

(def clean-variables-payload
  (mapcat (fn [[current-var-name new-var-name t-key]]
            [[:db/retract (sm/name->eid conn :variable/name current-var-name) :variable/group-variables (sm/t-key->eid conn t-key)]
             [:db/add (sm/name->eid conn :variable/name new-var-name) :variable/group-variables (sm/t-key->eid conn t-key)]])
          variables-to-clean))

(def add-missing-result-translation
  [{:db/id                                 (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:flame_length")
    :group-variable/result-translation-key "behaveplus:surface:result:fire_behavior:surface_fire:flame_length"}
   {:db/id                                 (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:rate_of_spread")
    :group-variable/result-translation-key "behaveplus:surface:result:fire_behavior:surface_fire:rate_of_spread"}
   {:db/id                                 (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:heading_fireline_intensity")
    :group-variable/result-translation-key "behaveplus:surface:result:fire_behavior:surface_fire:heading_fireline_intensity"}])

;; ===========================================================================================================
;; 5. Add translations for the group variables with translation keys renamed from step 1.
;; ===========================================================================================================

#_{:clj-kondo/ignore [(:missing-docstring)]}
(defn t-key->translation [s]
  (let [last-seg (subs s (inc (clojure.string/last-index-of s ":")))
        stripped (clojure.string/replace last-seg #"_heading$" "")
        words    (clojure.string/split stripped #"_")]
    (->> words
         (map clojure.string/capitalize)
         (clojure.string/join " "))))

#_{:clj-kondo/ignore [(:missing-docstring)]}
(def translations-payload
  (sm/build-translations-payload conn 100
                                 (into
                                  {"behaveplus:surface:result:fire_behavior:surface_fire:flame_length"               "Flame Length"
                                   "behaveplus:surface:result:fire_behavior:surface_fire:rate_of_spread"             "Rate of Spread"
                                   "behaveplus:surface:result:fire_behavior:surface_fire:heading_fireline_intensity" "Fireline Intensity"}
                                  (map #(let [t-key (:group-variable/translation-key %)]
                                          [t-key (t-key->translation t-key)])
                                       update-translation-key-payload))))

;; ===========================================================================================================
;; 6. Fix Variables, missing bp/uuid and bp/nid
;; ===========================================================================================================

;; These prevously created variables are missing some of the necessary attributes we need. It was not necessary before because the actual variables that were used
;; were (i.e. Heading Rate of Spread, Backing Rate of Spread, etc) which had this info. Now that we are deleting those variables, we need to add these backin. All directional group variables will point to the same variable now

(def fix-variable-missing-bp-uuid-payload
  [{:db/id                   (sm/name->eid conn :variable/name "Rate of Spread")
    :bp/uuid                 (str (squuid))
    :bp/nid                  (nano-id)
    :variable/kind           :continuous
    :variable/domain-uuid    (:bp/uuid (d/entity (d/db conn) (sm/name->eid conn :domain/name "Surface Rate of Spread")))
    :variable/dimension-uuid (:bp/uuid (d/entity (d/db conn) (sm/name->eid conn :dimension/name "Length")))}

   {:db/id                   (sm/name->eid conn :variable/name "Flame Length")
    :bp/uuid                 (str (squuid))
    :bp/nid                  (nano-id)
    :variable/kind           :continuous
    :variable/domain-uuid    (:bp/uuid (d/entity (d/db conn) (sm/name->eid conn :domain/name "Flame Length & Scorch Ht")))
    :variable/dimension-uuid (:bp/uuid (d/entity (d/db conn) (sm/name->eid conn :dimension/name "Length")))}

   {:db/id                   (sm/name->eid conn :variable/name "Fireline Intensity")
    :bp/uuid                 (str (squuid))
    :bp/nid                  (nano-id)
    :variable/kind           :continuous
    :variable/domain-uuid    (:bp/uuid (d/entity (d/db conn) (sm/name->eid conn :domain/name "Fireline Intensity")))
    :variable/dimension-uuid (:bp/uuid (d/entity (d/db conn) (sm/name->eid conn :dimension/name "Fireline Intensity")))}])

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [(:missing-docstring)]}
;; (def payload
;;   (concat clear-attrs-fn-payload update-settings-hide-from-result-payload))

#_{:clj-kondo/ignore [(:missing-docstring)]}
(def first-payload
  (concat
   fix-variable-missing-bp-uuid-payload
   links-to-add-payload
   update-translation-key-payload
   add-missing-result-translation
   translations-payload))

#_{:clj-kondo/ignore [(:missing-docstring)]}
(def second-payload
  (concat new-group-variables-payload
          clean-variables-payload))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn first-payload))
       (def tx-data-2 @(d/transact conn second-payload))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (do
    (sm/rollback-tx! conn tx-data-2)
    (sm/rollback-tx! conn tx-data)))
