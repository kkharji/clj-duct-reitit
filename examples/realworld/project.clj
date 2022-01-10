(defproject conduit-example "0.1.0-SNAPSHOT"
  :description "Implementation of https://github.com/gothinkster/realworld"
  :url "https://github.com/tami5/clj-duct-reitit/blob/master/realworld"
  :license {:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "1.5.648"]
                 [duct/module.sql "0.6.1"]
                 [duct/module.reitit "0.3.1-SNAPSHOT"]
                 [duct/middleware.buddy "0.2.0"]
                 [buddy/buddy-hashers "1.8.1"]]

  :profiles {:repl {:source-paths ["dev"]}
                    ;; :prep-tasks   ^:replace ["javac" "compile"]}
             :uberjar {:aot :all}}
  :plugins [[duct/lein-duct "0.12.3"]
            [cider/cider-nrepl      "0.27.3"]]
  ; :main ^:skip-aot conduit.main
  :resource-paths ["resources" "target/resources"])
  ; :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]])