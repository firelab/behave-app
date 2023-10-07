(ns date-utils.core)

(defn today
  "Today's date in string format 'yyyy-MM-dd'."
  []
  (.format (java.time.LocalDateTime/now)
           (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")))
