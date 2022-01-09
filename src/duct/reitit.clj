(ns duct.reitit
  (:require [duct.core :as core :refer [merge-configs]]
            [duct.lib.module :as module]
            [duct.logger :as logger]
            [duct.reitit.defaults :refer [reitit-module-defaults]]
            [duct.reitit.handler]
            [duct.reitit.log]
            [duct.reitit.util :as util :refer [get-namespaces resolve-key spy]]
            [integrant.core :refer [init-key] :as ig]))

(defn registry-resolve
  "Resolve registry keys into a map of {k [resolve config]}"
  [namespaces registry]
  (letfn [(process [f] (reduce f {} registry))
          (resolve [k] (resolve-key namespaces k))]
    (process
     (fn [acc [k v]]
       (when-let [res (resolve k)]
         (assoc acc k [res (or v {})]))))))

(defn registry-tree
  "Returns a config tree that should be merged in duct configuration map"
  [registry]
  (reduce-kv (fn [m _ v]
               (assoc m (first v) (second v))) {} registry))

(defn- registry-references
  "Returns a map of keys and their integrant reference."
  [registry]
  (reduce-kv (fn [m k v]
               (assoc m k (ig/ref (first v)))) {} registry))

(defn get-config
  "Merge user configuration with default-configuration and
  environment-default-configuration"
  [user-config]
  (let [profile-config (some-> user-config :duct.core/environment reitit-module-defaults)]
    (merge-configs (reitit-module-defaults :base) profile-config user-config)))

(defn ^:private auto-detect-exception [namespaces config]
  (let [as-handler (keyword (first namespaces) "exceptions")
        as-main (keyword (str (first namespaces) ".exceptions") "main")
        handler-ref (when (config as-handler) (ig/ref as-handler))
        main-ref (when (config as-main) (ig/ref as-main))]
    {::options {:exception (or handler-ref main-ref)}}))

(defmethod init-key :duct.module/reitit [_ _]
  (fn [{:duct.reitit/keys [registry routes exception] :as user-config}]
    (let [config     (get-config user-config)
          namespaces (get-namespaces config)
          registry   (registry-resolve namespaces registry)]
      (module/init
       {:root  :duct.reitit
        :config config
        :extra [(registry-tree registry)
                (when-not exception (auto-detect-exception namespaces config))]
        :store  {:namespaces namespaces :routes routes}
        :schema {::registry (registry-references registry)
                 ::routes   [:routes :namespaces ::registry]
                 ::router   [::routes ::options ::log]
                 ::log      ::options
                 ::handler  [::router ::options ::log]}}))))
