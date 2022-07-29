(ns behave.review)

(defn root-component [params]
  [:h1 (str "Worksheet " (:db/id params) " Review")])
