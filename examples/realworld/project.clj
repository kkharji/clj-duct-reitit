(defproject conduit-example "0.1.0-SNAPSHOT"
  :description "Implementation of https://github.com/gothinkster/realworld"
  :url "https://github.com/tami5/clj-duct-reitit/blob/master/realworld"
  :license {:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "1.5.648"]

                 ;; Database
                 [duct/module.sql "0.6.1" :exceptions [hikari-cp]]
                 [hikari-cp "2.13.0"]
                 [org.postgresql/postgresql "42.3.1"]
                 [com.github.seancorfield/next.jdbc "1.2.761"]
                 [com.github.seancorfield/honeysql "2.2.840"]

                 ;; Logging
                 [duct/module.logging "0.5.0"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail javax.jms/jms com.sun.jmdk/jmxtools com.sun.jmx/jmxri]]
                 [org.slf4j/slf4j-log4j12 "1.7.1"]
                 [com.taoensso/timbre "5.1.2"]

                 [duct/module.reitit "0.3.1"]
                 [duct/middleware.buddy "0.2.0"]
                 [buddy/buddy-hashers "1.8.1"]
                 [metosin/malli "0.7.5"]]

  :profiles {:repl {:source-paths ["dev"]
                    :prep-tasks   ^:replace ["javac" "compile"]}
             :uberjar {:aot :all}
             :dev {:dependencies [[org.xerial/sqlite-jdbc  "3.36.0.3"]]}}

  :plugins [[duct/lein-duct "0.12.3"]
            [cider/cider-nrepl      "0.27.3"]]
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]])
