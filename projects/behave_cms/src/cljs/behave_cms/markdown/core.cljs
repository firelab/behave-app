(ns behave-cms.markdown.core
  (:require [clojure.string        :as str]
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

(defn- render-latex [s]
  (try
    (.renderToString js/katex s)
    (catch js/Error _ s)))

(defn- latex [text state]
  [(if-not (or (:codeblock state) (:code state))
     (-> text
         (str/replace #"^\$\$\s(.*)\s\$\$$" #(render-latex (second %)))
         (str/replace #"\$\s(.*)\s\$" #(render-latex (second %))))
     text) state])

(def custom-transform-vector
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
   latex])

(defn md->html [text]
  (md/md->html text
               :replacement-transformers
               custom-transform-vector))
