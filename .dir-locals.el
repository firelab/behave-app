((nil . ((cider-clojure-cli-aliases . ":dev:behave/app")
         (cider-default-cljs-repl . figwheel-main)
         (eval . (progn
                   (add-to-list 'load-path (locate-dominating-file default-directory ".dir-locals.el"))
                   (require 'clj-align-require)
                   (defvar-local clj--formatting nil)
                   (add-hook 'after-save-hook
                             (lambda ()
                               (when (and buffer-file-name
                                          (not clj--formatting)
                                          (string-match-p "\\.cljs?c?$" buffer-file-name))
                                 (unwind-protect
                                     (progn
                                       (setq clj--formatting t)
                                       (clj-align-require)
                                       (save-buffer)
                                       (shell-command (format "cljfmt fix %s" (shell-quote-argument buffer-file-name)))
                                       (revert-buffer t t t))
                                   (setq clj--formatting nil))))
                             nil t))))))

;; VMS Configuration
;; TODO: Fix to avoid having two separate aliases for projects
;; ((nil . ((cider-clojure-cli-aliases . "-A:dev:behave/vms")
;;          (cider-default-cljs-repl . figwheel-main)
;;          (cider-figwheel-main-default-options . "vms"))))
