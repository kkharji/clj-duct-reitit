(ns user
  (:require [clj-dev.core :as d]
            [potemkin :as p]))

(p/import-vars
 [d start pause suspend resume stop restart watch system config go  halt  reset  reset-all])
(d/init
 {;; By default only watch and namespace reload and refresh works
  ;; Paths to target for refresh & tests
  :paths ["src" "test" "dev" "resources"]
  ;; Whether to auto-start i.e. when calling clj-dev/init, call clj-dev/start
  :start-on-init? false

  ;; File patterns to trigger reload on.
  :watch-pattern #"[^.].*(\.clj|\.edn)$"
  ;; time stamp format, set to nil if you don't want have timestamp.
  :watch-timestamp "[hh:mm:ss]"

  ;; Integrant file configuration path within :paths.
  :integrant-file-path "conduit/config.edn" ;; string
  ;; integrant profiles.
  :integrant-profiles [:duct.profile/dev :duct.profile/local] ;; vector
  ;; Whether duct framework should be considered.
  :integrant-with-duct? true})



