(defproject duct/module.reitit "0.2.0"
  :description "Duct module and router for the reitit routing library"
  :url "https://github.com/tami5/clj-duct-reitit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring-logger         "1.0.1"]
                 [metosin/reitit      "0.5.15"]
                 [metosin/ring-http-response "0.9.3"]
                 [duct/core           "0.7.0"]
                 [integrant           "0.7.0"]
                 [medley              "1.0.0"]]
  :profiles {:repl
             {:source-paths   ["dev/src"]
              :resource-paths ["dev/resources"]
              :dependencies [[clj-http/clj-http "3.12.3"]
                             [ring/ring-mock    "0.4.0"]
                             [tami5/clj-dev     "0.1.1"]
                             [duct/logger.timbre  "0.5.0"]
                             [duct/module.logging "0.5.0"]]}}
  :plugins [[cider/cider-nrepl      "0.27.3"]
            [lein-shell             "0.5.0"]]
  :aliases {"update-changelog" ["shell" "./bin/update-changelog"]})
