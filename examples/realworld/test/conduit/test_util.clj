(ns conduit.test-util
  (:require [clojure.java.io :as io]
            [duct.core :as duct]
            [integrant.core :as ig]
            [ragtime.jdbc :as ragtime.jdbc]
            [ragtime.repl :as ragtime.repl]))

(def ^:dynamic *database* nil)
(def ^:dynamic *system* nil)

(defn with-system [f]
  (duct.core/load-hierarchy)
  (let [system (-> (io/resource "conduit/config.edn")
                   (duct/read-config)
                   (dissoc :duct.module/reitit)
                   (duct/prep-config [:duct.profile/dev :duct.profile/test])
                   (ig/init))
        spec    (:duct.database.sql/hikaricp system)
        ragtime {:datastore (-> (:spec spec) (ragtime.jdbc/sql-database))
                 :migrations (:duct.migrator.ragtime/resources system)}]
    (binding [*database* spec *system* system]
      (ragtime.repl/migrate ragtime)
      (f)
      (ragtime.repl/rollback ragtime)
      (ig/halt! system))))

