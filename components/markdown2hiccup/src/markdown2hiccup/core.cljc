(ns markdown2hiccup.core
  (:require [clojure.string        :as str]
            [clojure.walk          :refer [postwalk]]
            [hickory.core          :refer [parse-fragment as-hiccup]]
            [markdown.core         :as md]
            [markdown.links        :refer [link
                                           image
                                           reference-link
                                           image-reference-link
                                           implicit-reference-link
                                           footnote-link]]
            [markdown.lists        :refer [li]]
            [markdown.tables       :refer [table]]
            [markdown.common       :refer [bold
                                           bold-italic
                                           dashes
                                           em
                                           escape-inhibit-separator
                                           escaped-chars
                                           inhibit
                                           inline-code
                                           italics
                                           strikethrough
                                           strong
                                           thaw-strings]]
            [markdown.transformers :refer [autoemail-transformer
                                           autourl-transformer
                                           blockquote-1
                                           blockquote-2
                                           br
                                           clear-line-state
                                           code
                                           codeblock
                                           empty-line
                                           heading
                                           hr
                                           paragraph
                                           set-line-state]]))

#?(:cljs
   (defn- render-latex [s]
     (try
       (.renderToString js/katex s)
       (catch js/Error _ s))))

#?(:cljs
   (defn- latex [text state]
     [(if-not (or (:codeblock state) (:code state))
        (-> text
            (str/replace #"^\$\$\s(.*)\s\$\$$" #(render-latex (second %)))
            (str/replace #"\$\s(.*)\s\$" #(render-latex (second %))))
        text) state]))

(def ^:private custom-transform-vector
  [set-line-state
   empty-line
   inhibit
   escape-inhibit-separator
   code
   codeblock
   escaped-chars
   inline-code
   autoemail-transformer
   autourl-transformer
   image
   image-reference-link
   link
   implicit-reference-link
   reference-link
   footnote-link
   hr
   blockquote-1
   li
   heading
   blockquote-2
   italics
   bold-italic
   em
   strong
   bold
   strikethrough
   table
   paragraph
   br
   thaw-strings
   dashes
   clear-line-state
   #?(:cljs latex)])

(defn- md->html [text]
  #?(:cljs (md/md->html text
               :replacement-transformers
               custom-transform-vector)
     :clj (md/md-to-html-string text
                                :replacement-transformers
                                custom-transform-vector)))

(def ^:private special-svg-attrs
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

;; (def ^:private special-svg-attrs-keys (-> special-svg-attrs (keys) (set)))

(defn- style-map [s]
  (println s)
  (persistent!
    (as-> s $
      (str/split $ #";")
      (map #(str/split % #":") $)
      (reduce (fn [acc [k v]] (assoc! acc (keyword k) (str/trim v))) (transient {}) $))))

(defn md->hiccup [md]
  (->> md
       (md->html)
       (parse-fragment)
       (map as-hiccup)
       ;;(postwalk #(if (and (= :svg (first %))
       ;;                    (not-empty (set/intersection special-svg-attrs-keys (-> % (second) (keys) (set)))))
       ;;             (assoc % 1 (persistent! (reduce (fn [acc [k v]] (assoc! acc (get special-svg-attrs k k) v)) (transient {}) (second %))))
       ;;             %))
       (postwalk #(if-let [style-str (get-in % [1 :style])]
                    (assoc-in % [1 :style] (style-map style-str))
                    %))
       (map #(assoc-in % [1 :key] (-> % (last) (hash))))))
