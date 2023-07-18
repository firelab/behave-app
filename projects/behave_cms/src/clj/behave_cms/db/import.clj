(ns behave-cms.db.import
  (:require [clojure.string      :as str]
            [behave-cms.db.core  :as db]))

(comment

  (def behave-uuid (:uuid (db/exec-one! {:select [:uuid] :from :applications})))
  behave-uuid

  (def v (db/exec-one! {:select [:*]
                        :from :variables
                        :where [:= :kind [:cast "continuous" :varkind]]
                        :limit 1}))

  (defn lexec! [q]
    (-> q
        (sql/format)
        (sdb/exec!)))

  (def column-mapping
    {:defaultvalue    [:default_value    :float]
    :englishdecimals [:english_decimals :int]
    :englishunits    [:english_units    :str]
    :maximum         [:maximum          :float]
    :minimum         [:minimum          :float]
    :metricdecimals  [:metric_decimals  :int]
    :metricunits     [:metric_units     :str]
    :nativedecimals  [:native_decimals  :int]
    :nativeunits     [:native_units     :str]})

  (defn parse-value [k v]
    (if (or (nil? v) (empty? v))
      nil
      (let [[_ val-type] (get column-mapping k [k :str])]
        (condp = val-type
          :float
          (Float/parseFloat v)

          :int
          (Integer/parseInt v)

          :str
          v))))

  (def cont-vars (lexec! {:select (into [:name] (keys column-mapping)) :from :variables :where [:= :type "continuous"]}))

  (defn remap-cols [m]
    (persistent!
      (reduce (fn [acc [k v]]
                (assoc! acc (first (get column-mapping k [k nil])) (parse-value k v)))
              (transient {})
              m)))

  (def remapped-cont-vars (map remap-cols cont-vars))

  (defn camel->sentence [s]
    (-> s
        (str/replace  #"^v" "")
        (str/replace  #"([A-Z])" #(str " " (-> % second)))
        (str/trim)))

  (defn sentence->word [s]
    (-> s
        (str/lower-case)
        (str/replace #"[\s\.-]" "")))

  (defn insert-continuous-variable [behave-uuid v]
    (let [vname    (-> v (:name) (camel->sentence))
          app-name "BehavePlus"
          result   (db/exec-one! {:insert-into :variables
                                  :values [{:application-rid behave-uuid
                                            :variable-name   vname
                                            :translation-key (u/translation-key app-name
                                                                                "v"
                                                                                (sentence->word vname))
                                            :help-key (u/help-key app-name
                                                                  "v"
                                                                  (sentence->word vname))
                                            :kind            [:cast "continuous" :varkind]}]}
                                {:return-keys true})]
      (db/exec-one! {:insert-into :continuous_variable_properties
                    :values [(merge {:variable-rid (:uuid result)}
                                    (select-keys v (map first (vals column-mapping))))]}
                    {:return-keys true})))

  (defn insert-discrete-variable [behave-uuid v]
    (let [vname    (-> v (:name) (camel->sentence))
          app-name "BehavePlus"
          result   (db/exec-one! {:insert-into :variables
                                  :values [{:application-rid behave-uuid
                                            :variable-name   vname
                                            :translation-key (u/translation-key app-name
                                                                                "v"
                                                                                (sentence->word vname))
                                            :help-key (u/help-key app-name
                                                                  "v"
                                                                  (sentence->word vname))
                                            :kind            [:cast "discrete" :varkind]}]}
                                {:return-keys true})]
      (db/exec-one! {:insert-into :discrete_variable_properties
                    :values [(merge {:variable-rid (:uuid result)}
                                    (select-keys v [:list]))]}
                    {:return-keys true})))

  (defn insert-text-variable [behave-uuid v]
    (let [vname    (-> v (:name) (camel->sentence))
          app-name "BehavePlus"]
      (db/exec-one! {:insert-into :variables
                    :values [{:application-rid behave-uuid
                              :variable-name   vname
                              :translation-key (u/translation-key app-name
                                                                  "v"
                                                                  (sentence->word vname))
                              :help-key (u/help-key app-name
                                                    "v"
                                                    (sentence->word vname))
                              :kind            [:cast "text" :varkind]}]}
                    {:return-keys true})))

  (def discrete-vars (lexec! {:select [:name :list] :from :variables :where [:= :type "discrete"]}))

  (def behave-uuid (:uuid (db/exec-one! {:select [:uuid] :from :applications})))
  behave-uuid
  (+ (count discrete-vars)
    (count remapped-cont-vars))

  (doseq [v discrete-vars]
    (insert-discrete-variable behave-uuid v))
  (db/exec! {:select [:%count.*] :from :variables})

  (doseq [v remapped-cont-vars]
    (insert-continuous-variable behave-uuid v))

  (def text-vars (lexec! {:select [:*] :from :variables :where [:= :type "text"]}))
  (insert-text-variable behave-uuid (first text-vars))

  (db/exec! {:select [:%count.*] :from :variables})

  (sql/register-op! :<=>)

  (sql/call :similarity "Contain Resource" "Contain")
  (sql/format {:select (sql/call :similarity "Contain Resource" "Contain")})

)
