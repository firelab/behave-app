((nil . ((cider-clojure-cli-aliases . ":dev:behave/app")
         (cider-default-cljs-repl . figwheel-main)
         (eval . (progn
                   (load (expand-file-name "scripts/behave.el"
                                           (locate-dominating-file buffer-file-name ".dir-locals.el")))
                   (add-hook 'after-save-hook #'behave-format-on-save nil t))))))

;; VMS Configuration
;; TODO: Fix to avoid having two separate aliases for projects
;; ((nil . ((cider-clojure-cli-aliases . "-A:dev:behave/vms")
;;          (cider-default-cljs-repl . figwheel-main)
;;          (cider-figwheel-main-default-options . "vms"))))
