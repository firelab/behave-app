(ns behave.components.a11y)

(defn on-matching-keys [keycodes f]
  (fn [e]
    (when (keycodes (.-charCode e))
      (f))))

(defn on-enter [f]
  (on-matching-keys #{13} f))

(defn on-space [f]
  (on-matching-keys #{32} f))

(defn on-space-enter [f]
  (on-matching-keys #{13 32} f))
