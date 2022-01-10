(ns conduit.main
  (:gen-class)
  (:require [duct.core :as duct]
            [duct.reitit])) ;; TODO: fixme

(duct/load-hierarchy)

(defn -main [& args]
  (let [keys     (or (duct/parse-keys args) [:duct/daemon])
        profiles [:duct.profile/prod]]
    (-> (duct/resource "conduit/config.edn")
        (duct/read-config)
        (duct/exec-config profiles keys))
    (System/exit 0)))

