(defproject duct/module.reitit "0.3.1-SNAPSHOT"
  :description "Duct module and router for the reitit routing library"
  :url "https://github.com/tami5/clj-duct-reitit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring-cors           "0.1.13"]
                 [metosin/reitit      "0.5.15"]
                 [duct/core           "0.7.0"]
                 [duct/logger         "0.3.0"]
                 [integrant           "0.7.0"]
                 [medley              "1.0.0"]
                 [metosin/malli       "0.7.5"]]
  :profiles {:repl
             {:source-paths   ["dev/src"]
              :resource-paths ["dev/resources"]
              :middleware [io.aviso.lein-pretty/inject]
              :dependencies [[clj-http/clj-http "3.12.3"]
                             [ring/ring-mock    "0.4.0"]
                             [tami5/clj-dev     "0.1.1"]
                             [duct/logger.timbre  "0.5.0"]
                             [duct/module.logging "0.5.0"]
                             [io.aviso/pretty "1.0"]]}}
  :plugins [[cider/cider-nrepl      "0.27.3"]
            [lein-shell             "0.5.0"]
            [io.aviso/pretty        "1.0"]]

  :aliases {"update-changelog" ["shell" "./bin/update-changelog"]})
