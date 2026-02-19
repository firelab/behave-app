(ns re-posh.subs
  (:require
   [re-frame.core :as r]
   [re-frame.loggers :refer [console]]
   [re-posh.db :refer [store]]
   [reagent.ratom :refer-macros [reaction]]
   [posh.reagent  :as p]))

(defmulti execute-sub :type)

(defmethod execute-sub :query
  [{:keys [query variables]}]
  (let [pre-q (partial p/q query @store)]
    (apply pre-q (into [] variables))))

(defmethod execute-sub :pull
  [{:keys [pattern id]}]
  (p/pull @store pattern id))

(defmethod execute-sub :pull-many
  [{:keys [pattern ids]}]
  (p/pull-many @store pattern ids))

(defn reg-sub
  "For a given `query-id` register a `config` function and input `signals`"
  [query-id & args]
  (let [config-fn  (last args)
        input-args (butlast args)
        err-header (str "re-posh: reg-sub for " query-id ", ")
        inputs-fn  (case (count input-args)
                     ;; no `inputs` function provided - give the default
                     0 (fn
                         ([_] nil)
                         ([_ _] nil))

                     ;; a single `inputs` fn
                     1 (let [f (first input-args)]
                         (when-not (fn? f)
                           (console :error err-header "2nd argument expected to ba an inputs function, got: " f))
                         f)

                     ;; one sugar pair
                     2 (let [[marker vec] input-args]
                         (when-not (= :<- marker)
                           (console :error err-header "expected :<-, got: " marker))
                         (fn inp-fn
                           ([_] (r/subscribe vec))
                           ([_ _] (r/subscribe vec))))

                     ;; multiple sugar pairs
                     (let [pairs (partition 2 input-args)
                           markers (map first pairs)
                           vecs (map last pairs)]
                       (when-not (and (every? #{:<-} markers) (every? vector? vecs))
                         (console :error err-header "expected pairs of :<- and vectors, got:" pairs))
                       (fn inp-fn
                         ([_] (map r/subscribe vecs))
                         ([_ _] (map r/subscribe vecs)))))]
    (r/reg-sub-raw
     query-id
     (fn [_ params]
       (if (= (count input-args) 0)
         ;; if there is no inputs-fn provided (or sugar version) don't wrap anything in reaction,
         ;; just return posh's query or pull
         (execute-sub (config-fn @@store params))
         (reaction
          (let [inputs (inputs-fn params)
                signals (if (seq? inputs)
                          (map deref inputs)
                          (deref inputs))]
            @(execute-sub (config-fn signals params)))))))))

(defn reg-query-sub
  "Syntax sugar for writing queries."
  [sub-name query]
  (reg-sub
   sub-name
   (fn [_ [_ & params]]
     {:type      :query
      :query     query
      :variables params})))

(defn reg-pull-sub
  "Syntax sugar for writing pull queries."
  [sub-name pattern]
  (reg-sub
   sub-name
   (fn [_ [_ id]]
     {:type    :pull
      :pattern pattern
      :id      id})))

(defn reg-pull-many-sub
  "Syntax sugar for writing pull-many queries."
  [sub-name pattern]
  (reg-sub
   sub-name
   (fn [_ [_ ids]]
     {:type    :pull-many
      :pattern pattern
      :ids     ids})))
