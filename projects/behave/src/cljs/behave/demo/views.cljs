(ns behave.demo.views
  (:require [behave.components.vega.diagram :refer [demo-output-diagram]]))

(defn demo-output-diagram-page [_params]
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
                            :ellipses [{:id "series1" :color "blue"}
                                       {:id "series2" :color "red"}]
                            :arrows   [{:id "series3" :color "green"}
                                       {:id "series4" :color "black"}]}])]])
