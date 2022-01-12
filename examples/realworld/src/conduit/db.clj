(ns conduit.db
  (:require [duct.database.sql]
            [buddy.hashers :as hashers]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [duct.reitit.util :refer [spy]]))

(def ^:private default-options
  {:return-keys true
   :builder-fn rs/as-unqualified-kebab-maps})

(defn execute-one! [sql-map {{:keys [datasource]} :spec}  & [options]]
  (jdbc/execute-one! datasource
                     (sql/format sql-map)
                     (merge default-options options)))

(defn execute! [sql-map {{:keys [datasource]} :spec}  & [options]]
  (jdbc/execute! datasource
                 (sql/format sql-map)
                 (merge default-options options)))

