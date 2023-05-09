(ns behave.demo.views
  (:require [behave.components.vega.diagram :refer [output-diagram]]))

(defn demo-output-diagram-page [_params]
  [:div
   (let [width  800
         height 800]
     [output-diagram {:title    "Contain Diagram"
                      :width    width
                      :height   height
                      :x-axis   {:domain [-20 100]
                                 :title  "x"}
                      :y-axis   {:domain [-100 100]
                                 :title  "y"}
                      :ellipses [{:id    "FirePerimeterAtReport"
                                  :color "blue"
                                  :a     15
                                  :b     10
                                  :phi   90}
                                 {:id          "FirePerimeterAtAttack"
                                  :color       "red"
                                  :a           15
                                  :b           10
                                  :phi         90}
                                 {:id       "FireLineConstructed"
                                  :color    "black"
                                  :a        50
                                  :b        25
                                  :phi      90
                                  :x-offset -5}]}])
   (let [width  800
         height 800]
     [output-diagram {:title    "Fire Shape Diagram"
                      :width    width
                      :height   height
                      :x-axis   {:domain [-100 100]
                                 :title  "x"}
                      :y-axis   {:domain [-100 100]
                                 :title  "y"}
                      :ellipses [{:id    "SurfaceFire"
                                  :color "red"
                                  :a     50
                                  :b     25
                                  :phi   45}]
                      :arrows   [{:id    "WindDriection"
                                  :color "blue"
                                  :r     50
                                  :theta 45}]}])])
