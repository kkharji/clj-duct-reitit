(defproject duct/module.reitit "0.0.1"
  :description "Duct module and router for the reitit routing library"
  :url "https://github.com/tami5/clj-duct-reitit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring-logger         "1.0.1"]
                 [metosin/reitit      "0.5.15"]
                 [duct/core           "0.7.0"]
                 [integrant           "0.7.0"]
                 [medley              "1.0.0"]]
  :plugins [[cider/cider-nrepl      "0.27.3"]]
  :profiles {:repl
             {:source-paths   ["dev/src"]
              :resource-paths ["dev/resources"]
              :dependencies [[clj-http/clj-http "3.12.3"]
                             [ring/ring-mock    "0.4.0"]]}})
