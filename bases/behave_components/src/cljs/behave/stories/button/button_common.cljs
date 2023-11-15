(ns behave.stories.button.button-common)

(def common-args {:size      nil
                  :disabled? false
                  :on-click  #(js/console.log "Clicked!")})

(def common-arg-types {:icon-position {:control "radio"
                                       :options ["left" "right"]}
                       :flat-edge     {:control "radio"
                                       :options ["top" "bottom" "left" "right" "none"
                                                 "all"]}
                       :size          {:control "radio"
                                       :options ["small" "normal" "large"]}
                       :on-click      {:action "clicked!"}})
