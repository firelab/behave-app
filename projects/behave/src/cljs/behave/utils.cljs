(ns behave.utils)

(defn add-script [js-path]
  (let [script-el (.createElement js/document "script")]
    (set! (.-src script-el) js-path)
    (set! (.-type script-el) "text/javascript")
    (-> js/document
        (.-body)
        (.appendChild script-el))))
