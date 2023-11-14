(ns behave.components.compute
  (:require [behave.components.inputs :refer [number-input text-input]]
            [behave.components.button :refer [button]]
            [reagent.core :as r]
            [clojure.string :as s]
            [goog.string :as gstring]
            [goog.string.format]))

(defn- format
  [fmt & args]
  (apply gstring/format fmt args))

(defn- vector->comma-seperated-string [v]
  (s/join "," (map #(str %) v)))

(defn- result->string [r]
  (if (coll? r)
    (vector->comma-seperated-string r)
    (str r)))

(defn- update-results! [id value compute-fn compute-args args result]
  (swap! args assoc id value)
  (when (and (= (count @args) (count compute-args))
             (every? (comp not js/isNaN) @args))
    (->> (apply compute-fn @args)
         (result->string)
         (reset! result))))

(defn- create-input-elements [compute-args compute-fn args result]
  (map-indexed (fn [id arg]
                 (let [{:keys [name units range]} arg
                       [min max]                  range]
                   ^{:key id}
                   [:div {:class "compute__input"}
                    [number-input {:label     name
                                   :id        id
                                   :error?    (when range
                                                (when-let [arg (get @args id)]
                                                  (not (<= min arg max))))
                                   :min       min
                                   :max       max
                                   :on-change #(update-results! id
                                                                (js/parseFloat (.. % -target -value))
                                                                compute-fn
                                                                compute-args
                                                                args
                                                                result)}]
                    [:div {:class "compute__input__range"}
                     (cond
                       (and range units) (format "%s - %s %s" min max units)
                       range             (format "%s - %s" min max)
                       :else             nil)]]))
               compute-args))

(defn compute [{:keys [compute-fn compute-args compute-btn-label on-compute]}]
  (r/with-let [args         (r/atom [])
               result       (r/atom nil)
               compute-args (js->clj compute-args :keywordize-keys true)]
    [:div {:class "compute"}
     (create-input-elements compute-args compute-fn args result)
     [:div {:class "compute__result"}
      [text-input {:disabled? true
                   :error?    false
                   :focused?  false
                   :label     "Result:"
                   :value     @result}]
      [button {:label    compute-btn-label
               :variant  "primary"
               :on-click #(on-compute @result)}]]]))
