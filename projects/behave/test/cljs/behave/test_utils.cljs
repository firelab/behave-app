(ns behave.test-utils
  (:require [datascript.impl.entity]
            [datascript.core :as d]
            [behave.store :as s]
            [austinbirch.reactive-entity :as re]))

(defn is-entity? [e]
  (= (type e) datascript.impl.entity/Entity))

(defn entity->cljs [entity]
  (cond

    (map? entity)
    (entity->cljs (d/entity @@s/conn (:db/id entity)))

    (is-entity? entity)
    (reduce (fn [acc key]
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

    :else
    entity))


(defn re-entity->cljs [entity]
  (entity->cljs (re/current-state entity)))
