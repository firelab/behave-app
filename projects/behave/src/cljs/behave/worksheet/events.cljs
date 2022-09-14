(ns behave.worksheet.events
  (:require [re-frame.core :as rf]
            [behave.solver :refer [solve-worksheet]]))

(comment

  (rf/clear-sub :worksheet/solve)
  (rf/clear-subscription-cache!)

  (rf/dispatch [:worksheet/solve])

  )
