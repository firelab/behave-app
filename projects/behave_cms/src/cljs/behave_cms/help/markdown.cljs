(ns behave-cms.help.markdown
  (:require [clojure.string             :as str]
            [clojure.set                :refer [rename-keys]]
            [clojure.walk               :refer [postwalk]]
            [hickory.core               :refer [parse-fragment as-hiccup]]
            [behave-cms.markdown.core   :refer [md->html]]))

;;; Markdown Rendering

(def ^{:private true}
  special-svg-attrs
  {:zoomandpan          :zoom-and-pan,
   :surfacescale        :surface-scale,
   :keysplines          :key-splines,
   :markerheight        :marker-height,
   :attributetype       :attribute-type,
   :stitchtiles         :stitch-tiles,
   :calcmode            :calc-mode,
   :refy                :ref-y,
   :primitiveunits      :primitive-units,
   :diffuseconstant     :diffuse-constant,
   :patternunits        :pattern-units,
   :referrerpolicy      :referrer-policy,
   :keytimes            :key-times,
   :viewtarget          :view-target,
   :kernelmatrix        :kernel-matrix,
   :stddeviation        :std-deviation,
   :preservealpha       :preserve-alpha,
   :kernelunitlength    :kernel-unit-length,
   :numoctaves          :num-octaves,
   :refx                :ref-x,
   :specularconstant    :specular-constant,
   :glyphref            :glyph-ref,
   :viewbox             :view-box,
   :contentscripttype   :content-script-type,
   :baseprofile         :base-profile,
   :autoreverse         :auto-reverse,
   :pointsaty           :points-at-y,
   :repeatcount         :repeat-count,
   :markerunits         :marker-units,
   :targety             :target-y,
   :keypoints           :key-points,
   :basefrequency       :base-frequency,
   :targetx             :target-x,
   :attributename       :attribute-name,
   :patterncontentunits :pattern-content-units,
   :requiredextensions  :required-extensions,
   :repeatdur           :repeat-dur,
   :markerwidth         :marker-width,
   :maskunits           :mask-units,
   :filterres           :filter-res,
   :pathlength          :path-length,
   :ychannelselector    :y-channel-selector,
   :contentstyletype    :content-style-type,
   :filterunits         :filter-units,
   :xchannelselector    :x-channel-selector,
   :pointsatx           :points-at-x,
   :preserveaspectratio :preserve-aspect-ratio,
   :requiredfeatures    :required-features,
   :specularexponent    :specular-exponent,
   :maskcontentunits    :mask-content-units,
   :gradienttransform   :gradient-transform,
   :tablevalues         :table-values,
   :limitingconeangle   :limiting-cone-angle,
   :textlength          :text-length,
   :systemlanguage      :system-language,
   :gradientunits       :gradient-units,
   :pointsatz           :points-at-z,
   :lengthadjust        :length-adjust,
   :startoffset         :start-offset,
   :spreadmethod        :spread-method,
   :edgemode            :edge-mode,
   :clippathunits       :clip-path-units,
   :patterntransform    :pattern-transform})

(defn- str->style-map [s]
  (persistent!
   (as-> s $
     (str/split $ #";")
     (map #(str/split % #":") $)
     (reduce (fn [acc [k v]] (assoc! acc (keyword k) (str/trim v))) (transient {}) $))))

(defn- fix-svg-attrs [el]
  (if (= :svg (first el))
    (assoc el 1 (rename-keys (second el) special-svg-attrs))
    el))

(defn- fix-style-map [el]
  (if-let [style-str (get-in el [1 :style])]
    (assoc-in el [1 :style] (str->style-map style-str))
    el))

(defn- add-key-attr [el]
  (if (-> el (second) (map?))
    (assoc-in el [1 :key] (-> el (last) (hash)))
    el))

(defn- postwalk-fn [v]
  (if (vector? v)
    (-> v
        (fix-style-map)
        (fix-svg-attrs)
        (add-key-attr))
    v))

(defn md->hiccup
  "Converts markdown into hiccup for rendering with reagent."
  [markdown]
  (->> markdown
       (md->html)
       (parse-fragment)
       (map as-hiccup)
       (postwalk #'postwalk-fn)))


(comment

  (md->hiccup "## Hello")

  )
