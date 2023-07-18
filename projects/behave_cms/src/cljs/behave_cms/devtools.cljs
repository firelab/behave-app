(ns behave-cms.devtools
  (:require [devtools.core :as devtools]))

(devtools/install! [:formatters :hints :async :sanity-hints])
(enable-console-print!)
(.log js/console "Loaded CLJS DevTools https://github.com/binaryage/cljs-devtools")
