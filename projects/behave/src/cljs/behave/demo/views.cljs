(ns behave.demo.views
  (:require [behave.components.chart :refer [demo-output-diagram]]))

(defn demo-output-diagram-page [_]
  [:div
   [:div
    (let [width  500
          height 500]
      [demo-output-diagram {:width    width
                            :height   height
                            :x-axis   {:scale  [-100 100]
                                       :title  "x"
                                       :offset (-> (/ height 2)
                                                   (* -1)) }
                            :y-axis   {:scale  [-100 100]
                                       :title  "y"
                                       :offset (-> (/ width 2)
                                                   (* -1)) }
                            :ellipses [{:id "1" :color-encoding "series1" :color "blue"}
                                       {:id "2" :color-encoding "series2" :color "red"}]
                            :arrows   [{:id "1" :color-encoding "series3" :color "green"}
                                       {:id "2" :color-encoding "series4" :color "black"}]}])]
   [:div
    (let [width  500
          height 500]
      [demo-output-diagram {:width    width
                            :height   height
                            :x-axis   {:scale [-100 100]
                                       :title "x"}
                            :y-axis   {:scale [-100 100]
                                       :title "y"}
                            :ellipses [{:id "1" :color-encoding "series1" :color "blue"}
                                       {:id "2" :color-encoding "series2" :color "red"}]
                            :arrows   [{:id "1" :color-encoding "series3" :color "green"}
                                       {:id "2" :color-encoding "series4" :color "black"}]}])]])
