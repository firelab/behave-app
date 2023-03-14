(ns behave.test-utils
  (:require [datascript.impl.entity]))

(defn is-entity? [e]
  (= (type e) datascript.impl.entity/Entity))

(defn entity->cljs [entity]
  (if (is-entity? entity)
    (reduce
     (fn [acc key]
       (let [val (get entity key)]
         (cond
           (or (set? val) (seq? val))
           (assoc acc key (mapv entity->cljs val))

           (is-entity? val)
           (assoc acc key (entity->cljs val))

           :else
           (assoc acc key val))))
     {}
     (keys entity))
    entity))
