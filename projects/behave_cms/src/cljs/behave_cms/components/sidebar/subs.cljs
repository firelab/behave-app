(ns behave-cms.components.sidebar.subs
  (:require [behave-cms.routes :refer [app-routes]]
            [behave-cms.store  :refer [conn]]
            [bidi.bidi         :refer [path-for]]
            [datascript.core   :as d]
            [goog.string       :as gstring]
            [re-frame.core     :as rf]))

;;; Tree node builders — walk DataScript entities to produce the sidebar tree

(defn- gv-label [gv]
  (let [v-name (-> gv :variable/_group-variables first :variable/name)
        dir    (or (-> gv :group-variable/direction-ref :list-option/name)
                   (some-> (:group-variable/direction gv) name))]
    (if dir (gstring/format "%s (%s)" v-name dir) (or v-name ""))))

(defn- gv-node [gv]
  {:id       (:db/id gv)
   :nid      (:bp/nid gv)
   :label    (gv-label gv)
   :type     :group-variable
   :link     (path-for app-routes :get-group-variable :nid (:bp/nid gv))
   :children []})

(defn- group-node [group]
  (let [db        @@conn
        g         (d/entity db (:db/id group))
        subgroups (sort-by :group/name (:group/children g))
        gvars     (sort-by gv-label (:group/group-variables g))]
    {:id       (:db/id g)
     :nid      (:bp/nid g)
     :label    (:group/name g)
     :type     :group
     :link     (path-for app-routes :get-group :nid (:bp/nid g))
     :children (vec (concat (mapv group-node subgroups)
                            (mapv gv-node gvars)))}))

(defn- submodule-node [sm]
  (let [db     @@conn
        e      (d/entity db (:db/id sm))
        groups (sort-by :group/order (:submodule/groups e))
        io     (some-> (:submodule/io e) name)]
    {:id       (:db/id e)
     :nid      (:bp/nid e)
     :label    (if io
                 (gstring/format "%s (%s)" (:submodule/name e) io)
                 (:submodule/name e))
     :type     :submodule
     :link     (path-for app-routes :get-submodule :nid (:bp/nid e))
     :children (mapv group-node groups)}))

(defn- module-node [mod]
  (let [db         @@conn
        e          (d/entity db (:db/id mod))
        submodules (sort-by :submodule/order (:module/submodules e))]
    {:id       (:db/id e)
     :nid      (:bp/nid e)
     :label    (:module/name e)
     :type     :module
     :link     (path-for app-routes :get-module :nid (:bp/nid e))
     :children (mapv submodule-node submodules)}))

(defn- sv-label [sv]
  (-> sv :variable/_subtool-variables first :variable/name (or "")))

(defn- sv-node [sv]
  {:id       (:db/id sv)
   :nid      (:bp/nid sv)
   :label    (sv-label sv)
   :type     :subtool-variable
   :link     (path-for app-routes :get-subtool-variable :nid (:bp/nid sv))
   :children []})

(defn- subtool-node [st]
  (let [db   @@conn
        e    (d/entity db (:db/id st))
        vars (sort-by :subtool-variable/order (:subtool/variables e))]
    {:id       (:db/id e)
     :nid      (:bp/nid e)
     :label    (:subtool/name e)
     :type     :subtool
     :link     (path-for app-routes :get-subtool :nid (:bp/nid e))
     :children (mapv sv-node vars)}))

(defn- tool-node [tool]
  (let [db       @@conn
        e        (d/entity db (:db/id tool))
        subtools (sort-by :subtool/order (:tool/subtools e))]
    {:id       (:db/id e)
     :nid      (:bp/nid e)
     :label    (:tool/name e)
     :type     :tool
     :link     (path-for app-routes :get-tool :nid (:bp/nid e))
     :children (mapv subtool-node subtools)}))

(defn- modules-group-node [app-eid modules]
  {:id       (str "modules-group-" app-eid)
   :label    "Modules"
   :type     :modules-group
   :link     nil
   :children (mapv module-node modules)})

(defn- tools-group-node [app-eid tools]
  {:id       (str "tools-group-" app-eid)
   :label    "Tools"
   :type     :tools-group
   :link     nil
   :children (mapv tool-node tools)})

(defn- app-node [app]
  (let [db      @@conn
        e       (d/entity db (:db/id app))
        modules (sort-by :module/order (:application/modules e))
        tools   (sort-by :tool/order (:application/tools e))]
    {:id       (:db/id e)
     :nid      (:bp/nid e)
     :label    (:application/name e)
     :type     :application
     :link     (path-for app-routes :get-application :nid (:bp/nid e))
     :children (cond-> []
                 (seq modules) (conj (modules-group-node (:db/id e) modules))
                 (seq tools)   (conj (tools-group-node (:db/id e) tools)))}))

;;; Subs

(rf/reg-sub
 :sidebar/tree
 :<- [:applications]
 (fn [applications _]
   (mapv app-node (sort-by :application/name applications))))

(rf/reg-sub
 :sidebar/toggled
 (fn [db _]
   (get-in db [:state :sidebar :toggled] #{})))

(rf/reg-sub
 :sidebar/active-path
 :<- [:route]
 :<- [:sidebar/tree]
 (fn [[route tree] _]
   (letfn [(find-path [node path]
             (let [path' (conj path (:id node))]
               (if (= (:link node) route)
                 path'
                 (some #(find-path % path') (:children node)))))]
     (vec (or (some #(find-path % []) tree) [])))))
