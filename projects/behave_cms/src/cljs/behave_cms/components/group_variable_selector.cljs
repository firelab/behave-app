(ns behave-cms.components.group-variable-selector
  (:require [clojure.set                  :refer [rename-keys]]
            [behave-cms.components.common :refer [dropdown]]
            [reagent.core                 :as r]
            [re-frame.core                :as rf]
            [behave-cms.utils             :as u]))

(defn- ->option [name-key]
  (fn [m]
    (-> m
        (select-keys [:db/id name-key])
        (rename-keys {:db/id :value name-key :label}))))

(defn group-variable-selector
  "Displays a Group Variable selector.
  Takes:
  - app-id: Entity ID of the application (required)
  - state-path: path to the editor. (optional). Use this if you have multiple
                selectors on your page.
  - gv-id: group variable entity id to edit (optional)
  - on-submit: On submit function, expects a single variable that is the group-variable entity id
  - title: Description of the group-variable.
  - module-filter-fn: function to filter modules. Expects a module entity. (optional)
  - submodule-filter-fn: function to filter submodules. Expects a submodule entity. (optional)
  - group-filter-fn: function to filter groups. Expects a group entity. (optional)
  "
  [{:keys [app-id gv-id state-path on-submit title
           module-filter-fn submodule-filter-fn group-filter-fn]
    :or   {state-path [:editors :group-variable-lookup]}}]

  (let [get-field   (fn [path] (rf/subscribe [:state path]))
        set-field   (fn [path v] (rf/dispatch [:state/set-state path v]))
        clear-field (fn [path] (rf/dispatch [:state/set-state path nil]))
        p           #(conj state-path %)]

    ;; Pre-populate Editor with existing gv-id
    (when (not= gv-id @(get-field (p :initial-group-variable)))
      (let [[module submodule group] @(rf/subscribe [:group-variable/module-submodule-group gv-id])]
        (rf/dispatch [:state/set-state
                      state-path
                      {:module                 module
                       :submodule              submodule
                       :group                  group
                       :group-variable         gv-id
                       :initial-group-variable gv-id}])))
    (let [modules    (rf/subscribe [:pull-children :application/modules app-id])
          submodules (rf/subscribe [:pull-children :module/submodules @(get-field (p :module))])
          groups     (rf/subscribe [:group-variable/submodule-groups-and-subgroups @(get-field (p :submodule))])
          variables  (rf/subscribe [:group/variables @(get-field (p :group))])
          disabled?  (r/track #(some nil? (map (fn [k] @(get-field (p k))) [:module :submodule :group :group-variable])))]
      [:div
       [:h4 (str (if gv-id "Update " "Add ") title)]

       [:form
        {:on-submit (when-let [v @(get-field (p :group-variable))]
                      (u/on-submit (fn [_]
                                     (on-submit v)
                                     (clear-field state-path))))}
        [dropdown {:label     "Module:"
                   :selected  @(get-field (p :module))
                   :options   (->> @modules
                                   (if module-filter-fn (filter module-filter-fn @submodules))
                                   (map (->option :module/name)))
                   :on-select #(do
                                 (set-field (p :module) (u/input-int-value %))
                                 (clear-field (p :submodule))
                                 (clear-field (p :group))
                                 (clear-field (p :group-variable)))}]
        [dropdown {:label     "Submodule:"
                   :selected  @(get-field (p :submodule))
                   :options   (->> @submodules
                                   (if submodule-filter-fn (filter submodule-filter-fn @submodules))
                                   (sort-by (juxt :submodule/io :submodule/order))
                                   (map (fn [{id :db/id io :submodule/io nname :submodule/name}]
                                          {:value id
                                           :label (str nname " (" (name io) ")")})))
                   :on-select #(do (set-field (p :submodule) (u/input-int-value %))
                                   (clear-field (p :group))
                                   (clear-field (p :group-variable)))}]
        [dropdown {:label     "Groups:"
                   :selected  @(get-field (p :group))
                   :options   (->> @groups
                                   (if group-filter-fn (filter group-filter-fn @submodules))
                                   (map (->option :group/name)))
                   :on-select #(do (set-field (p :group) (u/input-int-value %))
                                   (clear-field (p :group-variable)))}]
        [dropdown {:label     "Variable:"
                   :selected  @(get-field (p :group-variable))
                   :options   (map (->option :variable/name) @variables)
                   :on-select #(set-field (p :group-variable) (u/input-int-value %))}]
        [:button.btn.btn-sm.btn-outline-primary {:type "submit" :disabled @disabled?} "Select"]]])))
