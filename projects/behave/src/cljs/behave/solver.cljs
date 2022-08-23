(ns behave.solver
  (:require [behave.lib.core :as lib]
            [behave.lib.contain :as contain]
            [behave.lib.enums :as enum]))

(defn surface-solver [worksheet]
  (assoc-in worksheet [:results :surface] []))

(defn crown-solver [worksheet]
  (assoc-in worksheet [:results :crown] []))

(defn contain-solver [worksheet]
  (let [results (contain/init)])
  (assoc-in worksheet [:results :contain] []))

(defn mortality-solver [worksheet]
  (assoc-in worksheet [:results :mortality] []))


(defn solve-worksheet [{:keys [modules] :as worksheet}]
  (cond-> worksheet

    (modules :surface)
    (surface-solver)

    (modules :crown)
    (crown-solver)

    (modules :contain)
    (contain-solver)

    (modules :mortality)
    (mortality-solver)))


(#{:contain} :contain)
(comment
  (solve-worksheet {:modules #{:surface :contain}})

  (get enum/speed-units "Acres")

  )


