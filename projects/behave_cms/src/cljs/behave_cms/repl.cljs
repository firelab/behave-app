(ns behave-cms.repl
  #_(:require [weasel.repl :as repl]))

#_(when-not (repl/alive?)
  (repl/connect "ws://localhost:9001"
                :verbose  true
                :print    #{:repl :console}
                :on-error #(print "Error! " %)))
