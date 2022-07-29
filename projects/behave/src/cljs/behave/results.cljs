(ns behave.results)

(defn root-component [params]
  [:h1 (str "Worksheet "
            (:db/id params)
            " Results - "
            (str (get params :page "All")))])
